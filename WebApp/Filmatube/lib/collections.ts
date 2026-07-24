import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";
import { getPublishedMovies, type CatalogMovie } from "@/lib/movies";

export interface Collection {
  id: string;
  title: string;
  coverUrl: string;
  isPublic: boolean;
  userId: string;
}

function mapCollection(id: string, x: FirebaseFirestore.DocumentData): Collection {
  return {
    id,
    title: (x.title as string) ?? "",
    coverUrl: (x.coverUrl as string) ?? "",
    isPublic: !!x.isPublic,
    userId: (x.userId as string) ?? "",
  };
}

/** The signed-in user's collections (index-safe: filter by userId, sort in memory). */
export async function getUserCollections(uid: string): Promise<Collection[]> {
  const snap = await getAdminDb().collection("collections").where("userId", "==", uid).limit(100).get();
  return snap.docs
    .map((d) => mapCollection(d.id, d.data()))
    .sort((a, b) => a.title.localeCompare(b.title));
}

/** A user's public collections only — shown on their public profile. */
export async function getPublicCollections(uid: string): Promise<Collection[]> {
  const snap = await getAdminDb().collection("collections").where("userId", "==", uid).limit(100).get();
  return snap.docs
    .map((d) => mapCollection(d.id, d.data()))
    .filter((c) => c.isPublic)
    .sort((a, b) => a.title.localeCompare(b.title));
}

/** A featured editorial collection, as shown in the Home marquee strip. */
export interface FeaturedCollection {
  id: string;
  title: string;
  subtitle: string;
  coverUrl: string;
  movieCount: number;
  /** First few poster URLs — a fallback stack when there's no cover art. */
  posters: string[];
}

/**
 * Admin-curated collections flagged `featured`, ordered for the Home marquee. Server-only (the
 * admin SDK bypasses rules), so Home can fetch them without a client-side collection-group read.
 */
export async function getFeaturedCollections(): Promise<FeaturedCollection[]> {
  const snap = await getAdminDb().collection("collections").where("featured", "==", true).limit(20).get();
  if (snap.empty) return [];
  const catalog = await getPublishedMovies();
  const byId = new Map(catalog.map((m) => [m.id, m]));

  const rows = await Promise.all(
    snap.docs.map(async (d) => {
      const items = await d.ref.collection("items").limit(50).get();
      const orderedIds = items.docs
        .map((i) => ({ id: i.id, order: (i.get("order") as number) ?? 0 }))
        .sort((a, b) => a.order - b.order)
        .map((o) => o.id);
      const posters = orderedIds
        .map((id) => byId.get(id)?.posterUrl)
        .filter((p): p is string => !!p)
        .slice(0, 3);
      return {
        id: d.id,
        title: (d.get("title") as string) ?? "",
        subtitle: (d.get("subtitle") as string) ?? "",
        coverUrl: (d.get("coverUrl") as string) ?? "",
        featuredOrder: (d.get("featuredOrder") as number) ?? 0,
        movieCount: orderedIds.length,
        posters,
      };
    }),
  );
  return rows
    .filter((r) => r.movieCount > 0)
    .sort((a, b) => a.featuredOrder - b.featuredOrder)
    .map((r) => ({ id: r.id, title: r.title, subtitle: r.subtitle, coverUrl: r.coverUrl, movieCount: r.movieCount, posters: r.posters }));
}

export async function getCollection(
  id: string,
): Promise<{ collection: Collection; movies: CatalogMovie[] } | null> {
  const doc = await getAdminDb().collection("collections").doc(id).get();
  if (!doc.exists) return null;
  const collection = mapCollection(doc.id, doc.data() ?? {});

  const [itemsSnap, catalog] = await Promise.all([
    getAdminDb().collection("collections").doc(id).collection("items").limit(200).get(),
    getPublishedMovies(),
  ]);
  const byId = new Map(catalog.map((m) => [m.id, m]));
  const movies = itemsSnap.docs
    .map((d) => ({
      id: d.id,
      order: (d.get("order") as number) ?? (d.get("addedAt") as { toMillis?: () => number })?.toMillis?.() ?? 0,
    }))
    .sort((a, b) => a.order - b.order)
    .map((o) => byId.get(o.id))
    .filter((m): m is CatalogMovie => m !== undefined);
  return { collection, movies };
}
