import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

/** An editorial collection row for the admin manager. */
export interface EditorialRow {
  id: string;
  title: string;
  subtitle: string;
  coverUrl: string;
  featured: boolean;
  featuredOrder: number;
  movieCount: number;
}

/** All admin-curated (editorial) collections, ordered — featured first, then by order. */
export async function listEditorialCollections(): Promise<EditorialRow[]> {
  const snap = await getAdminDb().collection("collections").where("isEditorial", "==", true).limit(100).get();
  const rows = await Promise.all(
    snap.docs.map(async (d) => {
      const count = (await d.ref.collection("items").count().get()).data().count;
      return {
        id: d.id,
        title: (d.get("title") as string) ?? "",
        subtitle: (d.get("subtitle") as string) ?? "",
        coverUrl: (d.get("coverUrl") as string) ?? "",
        featured: d.get("featured") === true,
        featuredOrder: (d.get("featuredOrder") as number) ?? 0,
        movieCount: count,
      };
    }),
  );
  return rows.sort((a, b) => {
    if (a.featured !== b.featured) return a.featured ? -1 : 1;
    return a.featuredOrder - b.featuredOrder;
  });
}
