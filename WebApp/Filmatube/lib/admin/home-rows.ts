import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

/** A brief for the movie picker — enough to show a poster row without loading full docs. */
export interface MovieBrief {
  id: string;
  title: string;
  posterUrl: string;
  year: number;
}

/** An admin-curated Home row, serialized for the client (Timestamps → millis). */
export interface HomeRow {
  id: string;
  titleEn: string;
  titleFr: string;
  movieIds: string[];
  enabled: boolean;
  /** true = render above the personalised rails ("pinned"); false = below them. */
  pinned: boolean;
  order: number;
  startAtMs: number | null;
  endAtMs: number | null;
  /** Resolved briefs for movieIds, in order — for the editor's selected list. */
  movies: MovieBrief[];
}

function briefOf(id: string, data: FirebaseFirestore.DocumentData): MovieBrief {
  return {
    id,
    title: (data.title?.en as string) ?? "",
    posterUrl: (data.posterUrl as string) ?? "",
    year: (data.year as number) ?? 0,
  };
}

/** Resolve an ordered id list to briefs, dropping ids no longer in the catalogue. */
export async function resolveMovieBriefs(ids: string[]): Promise<MovieBrief[]> {
  if (ids.length === 0) return [];
  const db = getAdminDb();
  // Chunk by 10 for whereIn / getAll — use getAll for a straight id fetch.
  const refs = ids.map((id) => db.collection("movies").doc(id));
  const snaps = await db.getAll(...refs);
  const byId = new Map(snaps.filter((s) => s.exists).map((s) => [s.id, briefOf(s.id, s.data()!)]));
  return ids.map((id) => byId.get(id)).filter((m): m is MovieBrief => !!m);
}

/** All curated rows, ordered, with their movies resolved — for the admin manager. */
export async function listHomeRows(): Promise<HomeRow[]> {
  const snap = await getAdminDb().collection("homeRows").get();
  const rows = snap.docs.map((d) => {
    const x = d.data();
    return {
      id: d.id,
      titleEn: (x.titleEn as string) ?? "",
      titleFr: (x.titleFr as string) ?? "",
      movieIds: (x.movieIds as string[]) ?? [],
      enabled: x.enabled !== false,
      pinned: !!x.pinned,
      order: (x.order as number) ?? 0,
      startAtMs: x.startAt?.toMillis?.() ?? null,
      endAtMs: x.endAt?.toMillis?.() ?? null,
      movies: [] as MovieBrief[],
    };
  });
  rows.sort((a, b) => a.order - b.order);
  // Resolve each row's posters for the editor. Rows are few (admin-authored), so this is cheap.
  await Promise.all(
    rows.map(async (r) => {
      r.movies = await resolveMovieBriefs(r.movieIds);
    }),
  );
  return rows;
}
