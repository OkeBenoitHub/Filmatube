import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";
import { getMovie, type CatalogMovie } from "@/lib/movies";

/** The user's Watch Later movies (watchlists/{uid}/movies), newest first. */
export async function getWatchlist(uid: string): Promise<CatalogMovie[]> {
  const snap = await getAdminDb()
    .collection("watchlists")
    .doc(uid)
    .collection("movies")
    .orderBy("addedAt", "desc")
    .limit(100)
    .get();

  const movies = await Promise.all(snap.docs.map((d) => getMovie(d.id)));
  return movies.filter((m): m is CatalogMovie => m !== null);
}
