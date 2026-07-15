import "server-only";

import { unstable_cache } from "next/cache";
import { getAdminDb } from "@/lib/firebase-admin";
import type { Locale } from "@/lib/i18n/config";

/** Cache tag for the movie catalog — admin mutations call revalidateTag(CATALOG_TAG). */
export const CATALOG_TAG = "catalog";

export interface CatalogCast {
  name: string;
  character: string;
  photoUrl: string;
}

export interface CatalogSubtitle {
  lang: string;
  url: string;
}

export interface CatalogMovie {
  id: string;
  title: { en: string; fr: string };
  description: { en: string; fr: string };
  posterUrl: string;
  backdropUrl: string;
  trailerUrl: string;
  genres: string[];
  year: number;
  duration: number;
  ageRating: string;
  cast: CatalogCast[];
  directors: string[];
  averageRating: number;
  ratingsCount: number;
  viewsCount: number;
  isFeatured: boolean;
  isComingSoon: boolean;
  hasVideo: boolean;
  subtitleTracks: CatalogSubtitle[];
  addedAtMs: number;
}

/** Pick the copy for the current locale, falling back to the other language. */
export function localized(text: { en: string; fr: string } | undefined, locale: Locale): string {
  if (!text) return "";
  if (locale === "fr") return text.fr || text.en;
  return text.en || text.fr;
}

function mapDoc(id: string, x: Record<string, unknown>): CatalogMovie {
  const title = (x.title as { en?: string; fr?: string }) ?? {};
  const description = (x.description as { en?: string; fr?: string }) ?? {};
  return {
    id,
    title: { en: title.en ?? "", fr: title.fr ?? "" },
    description: { en: description.en ?? "", fr: description.fr ?? "" },
    posterUrl: (x.posterUrl as string) ?? "",
    backdropUrl: (x.backdropUrl as string) ?? "",
    trailerUrl: (x.trailerUrl as string) ?? "",
    genres: (x.genres as string[]) ?? [],
    year: (x.year as number) ?? 0,
    duration: (x.duration as number) ?? 0,
    ageRating: (x.ageRating as string) ?? "",
    cast: ((x.cast as CatalogCast[]) ?? []).map((c) => ({
      name: c?.name ?? "",
      character: c?.character ?? "",
      photoUrl: c?.photoUrl ?? "",
    })),
    directors: (x.directors as string[]) ?? [],
    averageRating: (x.averageRating as number) ?? 0,
    ratingsCount: (x.ratingsCount as number) ?? 0,
    viewsCount: (x.viewsCount as number) ?? 0,
    isFeatured: !!x.isFeatured,
    isComingSoon: !!x.isComingSoon,
    hasVideo: Boolean(x.videoKey),
    subtitleTracks: ((x.subtitleTracks as CatalogSubtitle[]) ?? [])
      .filter((t) => t?.lang && t?.url)
      .map((t) => ({ lang: t.lang, url: t.url })),
    addedAtMs:
      (x.addedAt as { toMillis?: () => number })?.toMillis?.() ??
      (x.updatedAt as { toMillis?: () => number })?.toMillis?.() ??
      0,
  };
}

/**
 * All published movies, newest first. One batched read the catalog pages derive
 * their rows from in memory — index-safe (no composite indexes), mirroring the
 * Android repository's approach.
 */
export const getPublishedMovies = unstable_cache(
  async (): Promise<CatalogMovie[]> => {
    const snap = await getAdminDb().collection("movies").where("status", "==", "published").limit(500).get();
    return snap.docs.map((d) => mapDoc(d.id, d.data())).sort((a, b) => b.addedAtMs - a.addedAtMs);
  },
  ["published-movies"],
  { revalidate: 60, tags: [CATALOG_TAG] },
);

export interface ContinueWatchingItem {
  movie: CatalogMovie;
  progress: number;
}

/**
 * In-progress movies for the Continue Watching row, read from the same
 * `watchProgress/{uid}/items` collection the Android player writes to — so
 * progress syncs across platforms. Index-safe (orderBy updatedAt only).
 */
export async function getContinueWatching(uid: string, limit = 12): Promise<ContinueWatchingItem[]> {
  const [snap, catalog] = await Promise.all([
    getAdminDb().collection("watchProgress").doc(uid).collection("items").orderBy("updatedAt", "desc").limit(20).get(),
    getPublishedMovies(),
  ]);
  const byId = new Map(catalog.map((m) => [m.id, m]));

  return snap.docs
    .map((d) => ({
      movieId: (d.get("movieId") as string) ?? d.id,
      progress: (d.get("progress") as number) ?? 0,
      completed: (d.get("completed") as boolean) ?? false,
    }))
    .filter((e) => !e.completed)
    .slice(0, limit)
    .map((e) => {
      const movie = byId.get(e.movieId);
      return movie ? { movie, progress: Number(e.progress) } : null;
    })
    .filter((x): x is ContinueWatchingItem => x !== null);
}

/** A single published movie, or null (draft/missing movies stay hidden). */
export const getMovie = unstable_cache(
  async (id: string): Promise<CatalogMovie | null> => {
    const snap = await getAdminDb().collection("movies").doc(id).get();
    if (!snap.exists) return null;
    const data = snap.data() ?? {};
    if (data.status !== "published") return null;
    return mapDoc(snap.id, data);
  },
  ["movie"],
  { revalidate: 60, tags: [CATALOG_TAG] },
);

// --- pure row selectors (operate on getPublishedMovies() output) ---

export const pickFeatured = (m: CatalogMovie[]) => m.filter((x) => x.isFeatured && !x.isComingSoon);

export const pickTrending = (m: CatalogMovie[]) =>
  [...m].filter((x) => !x.isComingSoon).sort((a, b) => b.viewsCount - a.viewsCount).slice(0, 20);

export const pickNewReleases = (m: CatalogMovie[]) => m.filter((x) => !x.isComingSoon).slice(0, 20);

export const pickComingSoon = (m: CatalogMovie[]) => m.filter((x) => x.isComingSoon);

export const pickByGenre = (m: CatalogMovie[], genre: string) =>
  m.filter((x) => !x.isComingSoon && x.genres.includes(genre));

export function pickRelated(m: CatalogMovie[], movie: CatalogMovie): CatalogMovie[] {
  return m
    .filter((x) => x.id !== movie.id && !x.isComingSoon && x.genres.some((g) => movie.genres.includes(g)))
    .slice(0, 15);
}

export function searchMovies(m: CatalogMovie[], query: string): CatalogMovie[] {
  const q = query.trim().toLowerCase();
  if (!q) return [];
  return m
    .filter(
      (x) =>
        x.title.en.toLowerCase().includes(q) ||
        x.title.fr.toLowerCase().includes(q) ||
        x.directors.some((d) => d.toLowerCase().includes(q)) ||
        x.cast.some((c) => c.name.toLowerCase().includes(q)),
    )
    .slice(0, 40);
}
