import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

export interface AdminMovieRow {
  id: string;
  title: string;
  year: number;
  posterUrl: string;
  status: string;
  isFeatured: boolean;
  isPinned: boolean;
  isComingSoon: boolean;
}

/** All movies (draft + published) for the admin list, newest first. */
export async function listMovies(): Promise<AdminMovieRow[]> {
  const snap = await getAdminDb().collection("movies").limit(500).get();
  return snap.docs
    .map((d) => {
      const x = d.data();
      return {
        addedAt: x.addedAt?.toMillis?.() ?? 0,
        row: {
          id: d.id,
          title: x.title?.en ?? "",
          year: x.year ?? 0,
          posterUrl: x.posterUrl ?? "",
          status: x.status ?? "draft",
          isFeatured: !!x.isFeatured,
          isPinned: !!x.isPinned,
          isComingSoon: !!x.isComingSoon,
        } satisfies AdminMovieRow,
      };
    })
    .sort((a, b) => b.addedAt - a.addedAt)
    .map((entry) => entry.row);
}

/** Full movie document for the edit form (plain object). */
export async function getMovieAdmin(id: string): Promise<Record<string, unknown> | null> {
  const snap = await getAdminDb().collection("movies").doc(id).get();
  if (!snap.exists) return null;
  return { id: snap.id, ...snap.data() };
}
