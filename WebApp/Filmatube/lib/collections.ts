import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";
import { getMovie, type CatalogMovie } from "@/lib/movies";

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

export async function getCollection(
  id: string,
): Promise<{ collection: Collection; movies: CatalogMovie[] } | null> {
  const doc = await getAdminDb().collection("collections").doc(id).get();
  if (!doc.exists) return null;
  const collection = mapCollection(doc.id, doc.data() ?? {});

  const itemsSnap = await getAdminDb().collection("collections").doc(id).collection("items").limit(200).get();
  const ordered = itemsSnap.docs
    .map((d) => ({
      id: d.id,
      order: (d.get("order") as number) ?? (d.get("addedAt") as { toMillis?: () => number })?.toMillis?.() ?? 0,
    }))
    .sort((a, b) => a.order - b.order);
  const movies = (await Promise.all(ordered.map((o) => getMovie(o.id)))).filter(
    (m): m is CatalogMovie => m !== null,
  );
  return { collection, movies };
}
