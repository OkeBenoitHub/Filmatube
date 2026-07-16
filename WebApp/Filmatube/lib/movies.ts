import "server-only";

import { unstable_cache } from "next/cache";
import { getAdminDb } from "@/lib/firebase-admin";
import { mapMovieDoc, type CatalogMovie, type ContinueWatchingItem } from "@/lib/catalog";

// Domain types, the mapper and the pure row selectors live in lib/catalog (client-safe);
// re-exported here so existing server imports keep working.
export {
  localized,
  pickFeatured,
  pickTrending,
  pickNewReleases,
  pickComingSoon,
  pickByGenre,
  pickRelated,
  searchMovies,
} from "@/lib/catalog";
export type { CatalogCast, CatalogSubtitle, CatalogMovie, ContinueWatchingItem } from "@/lib/catalog";

/** Cache tag for the movie catalog — admin mutations call revalidateTag(CATALOG_TAG). */
export const CATALOG_TAG = "catalog";

/**
 * All published movies, newest first. One batched read the catalog pages derive
 * their rows from in memory — index-safe (no composite indexes), mirroring the
 * Android repository's approach.
 */
export const getPublishedMovies = unstable_cache(
  async (): Promise<CatalogMovie[]> => {
    const snap = await getAdminDb().collection("movies").where("status", "==", "published").limit(500).get();
    return snap.docs.map((d) => mapMovieDoc(d.id, d.data())).sort((a, b) => b.addedAtMs - a.addedAtMs);
  },
  ["published-movies"],
  { revalidate: 60, tags: [CATALOG_TAG] },
);

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
    return mapMovieDoc(snap.id, data);
  },
  ["movie"],
  { revalidate: 60, tags: [CATALOG_TAG] },
);
