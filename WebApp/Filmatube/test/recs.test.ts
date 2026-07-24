import { describe, expect, it } from "vitest";
// The shared scorer the nightly Cloud Function runs — tested directly, so QA covers the real
// ranking rather than a reimplementation. CommonJS module, imported via default interop.
import recsCore from "../../../functions/recs-core.js";

const { buildRecsForUser, buildAllRecs } = recsCore as {
  buildRecsForUser: (db: FakeDb, fv: unknown, uid: string, movies: Movie[]) => Promise<boolean>;
  buildAllRecs: (db: FakeDb, fv: unknown) => Promise<{ built: number; total: number; catalogue: number }>;
};

interface Movie {
  id: string;
  title: { en: string };
  genres: string[];
  cast?: { name: string }[];
  directors?: string[];
  year?: number;
  viewsCount?: number;
  isComingSoon?: boolean;
  status?: string;
}

// ── Minimal in-memory Firestore, only the surface the core touches. ──
type Signals = {
  watchProgress?: Record<string, { isWatched?: boolean }>;
  watchlists?: Record<string, object>;
  likes?: Record<string, object>;
  reactions?: Record<string, { reaction?: string }>;
  recFeedback?: Record<string, object>;
};

const FieldValue = { serverTimestamp: () => "TS" };

function snap<T>(obj: Record<string, T>) {
  const docs = Object.entries(obj).map(([id, d]) => ({
    id,
    data: () => d,
    get: (k: string) => (d as Record<string, unknown>)[k],
  }));
  return { docs, size: docs.length, empty: docs.length === 0, forEach: (fn: (d: unknown) => void) => docs.forEach(fn) };
}

const SUBFIELD: Record<string, string> = {
  watchProgress: "items",
  watchlists: "movies",
  likes: "items",
  reactions: "items",
  recFeedback: "items",
};

interface FakeDb {
  written: Record<string, unknown>;
  collection: (name: string) => unknown;
}

function makeDb(opts: {
  movies?: Movie[];
  users?: Record<string, object>;
  signalsByUser?: Record<string, Signals>;
  signals?: Signals;
}): FakeDb {
  const written: Record<string, unknown> = {};
  const db: FakeDb = {
    written,
    collection(name: string) {
      if (name === "movies") {
        const published = (opts.movies ?? []).filter((m) => (m.status ?? "published") === "published");
        return { where: () => ({ get: async () => snap(Object.fromEntries(published.map((m) => [m.id, m]))) }) };
      }
      if (name === "users") {
        return { where: () => ({ limit: () => ({ get: async () => snap(opts.users ?? {}) }) }) };
      }
      if (name === "recs") {
        return { doc: (uid: string) => ({ set: async (payload: unknown) => { written[uid] = payload; } }) };
      }
      const sub = SUBFIELD[name];
      return {
        doc: (uid: string) => ({
          collection: (c: string) => ({
            get: async () => {
              const s = opts.signalsByUser?.[uid] ?? opts.signals ?? {};
              const bag = c === sub ? (s as Record<string, Record<string, Record<string, unknown>>>)[name] ?? {} : {};
              return snap(bag);
            },
          }),
        }),
      };
    },
  };
  return db;
}

const m = (id: string, genres: string[], over: Partial<Movie> = {}): Movie => ({
  id,
  title: { en: id },
  genres,
  year: 2024,
  viewsCount: 1000,
  ...over,
});

// A small crime/action catalogue with a shared actor, one coming-soon title.
const catalogue: Movie[] = [
  m("seed-crime", ["crime", "thriller"], { cast: [{ name: "Lead A" }] }),
  m("crime-1", ["crime"], { cast: [{ name: "Lead A" }] }),
  m("crime-2", ["crime", "drama"]),
  m("crime-3", ["thriller"]),
  m("crime-4", ["crime"]),
  m("rom-1", ["romance"]),
  m("rom-2", ["romance", "comedy"]),
  m("soon-crime", ["crime", "thriller"], { isComingSoon: true }),
];

describe("recommendation scorer", () => {
  it("writes nothing when the user has no signal", async () => {
    const db = makeDb({ movies: catalogue, signals: {} });
    const built = await buildRecsForUser(db, FieldValue, "u", catalogue);
    expect(built).toBe(false);
    expect(db.written.u).toBeUndefined();
  });

  it("recommends overlapping unseen titles and marks the seed as seen", async () => {
    const db = makeDb({ movies: catalogue, signals: { watchProgress: { "seed-crime": { isWatched: true } } } });
    const built = await buildRecsForUser(db, FieldValue, "u", catalogue);
    expect(built).toBe(true);
    const rec = db.written.u as { topPicks: string[]; rows: { movieIds: string[] }[] };
    expect(rec.topPicks).not.toContain("seed-crime"); // the seed is "seen"
    expect(rec.topPicks).toContain("crime-1"); // shares genre + actor → top
    // Crime candidates outrank the unrelated romance titles.
    expect(rec.topPicks.indexOf("crime-1")).toBeLessThan(rec.topPicks.indexOf("rom-1"));
  });

  it("never recommends a coming-soon title", async () => {
    const db = makeDb({ movies: catalogue, signals: { watchProgress: { "seed-crime": { isWatched: true } } } });
    await buildRecsForUser(db, FieldValue, "u", catalogue);
    const rec = db.written.u as { topPicks: string[]; rows: { movieIds: string[] }[] };
    expect(rec.topPicks).not.toContain("soon-crime");
    expect(rec.rows.flatMap((r) => r.movieIds)).not.toContain("soon-crime");
  });

  it("excludes dismissed titles (recFeedback)", async () => {
    const db = makeDb({
      movies: catalogue,
      signals: { watchProgress: { "seed-crime": { isWatched: true } }, recFeedback: { "crime-1": {} } },
    });
    await buildRecsForUser(db, FieldValue, "u", catalogue);
    const rec = db.written.u as { topPicks: string[] };
    expect(rec.topPicks).not.toContain("crime-1");
  });

  it("only builds a 'because you watched' rail when ≥3 titles overlap", async () => {
    // seed-crime overlaps crime-1..4 (≥3) → a rail; a lone romance seed would not.
    const db = makeDb({ movies: catalogue, signals: { likes: { "seed-crime": {} } } });
    await buildRecsForUser(db, FieldValue, "u", catalogue);
    const rec = db.written.u as { rows: { seedMovieId: string; movieIds: string[] }[] };
    expect(rec.rows.every((r) => r.movieIds.length >= 3)).toBe(true);
    expect(rec.rows.some((r) => r.seedMovieId === "seed-crime")).toBe(true);
  });

  it("caps topPicks at 20", async () => {
    const many: Movie[] = [m("seed", ["action"]), ...Array.from({ length: 30 }, (_, i) => m(`a${i}`, ["action"]))];
    const db = makeDb({ movies: many, signals: { watchProgress: { seed: { isWatched: true } } } });
    await buildRecsForUser(db, FieldValue, "u", many);
    const rec = db.written.u as { topPicks: string[] };
    expect(rec.topPicks.length).toBeLessThanOrEqual(20);
  });

  it("buildAllRecs only builds for active users who have signal", async () => {
    const db = makeDb({
      movies: catalogue,
      users: { withSignal: {}, noSignal: {} },
      signalsByUser: { withSignal: { watchProgress: { "seed-crime": { isWatched: true } } }, noSignal: {} },
    });
    const summary = await buildAllRecs(db, FieldValue);
    expect(summary.total).toBe(2);
    expect(summary.built).toBe(1);
    expect(db.written.withSignal).toBeDefined();
    expect(db.written.noSignal).toBeUndefined();
  });
});
