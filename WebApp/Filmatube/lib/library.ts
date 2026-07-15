import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";
import { getPublishedMovies, type CatalogMovie } from "@/lib/movies";

/**
 * The user's Watch Later movies (watchlists/{uid}/movies), newest first.
 * Joins the saved ids against the cached catalog instead of N per-movie reads.
 */
export async function getWatchlist(uid: string): Promise<CatalogMovie[]> {
  const [snap, catalog] = await Promise.all([
    getAdminDb().collection("watchlists").doc(uid).collection("movies").orderBy("addedAt", "desc").limit(100).get(),
    getPublishedMovies(),
  ]);
  const byId = new Map(catalog.map((m) => [m.id, m]));
  return snap.docs.map((d) => byId.get(d.id)).filter((m): m is CatalogMovie => m !== undefined);
}
