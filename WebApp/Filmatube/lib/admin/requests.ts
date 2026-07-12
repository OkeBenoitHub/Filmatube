import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

export interface RequestRow {
  id: string;
  userId: string;
  userName: string;
  title: string;
  note: string;
  status: string;
  reason: string;
  movieId: string;
  createdAtMs: number;
}

/** All content requests, newest first (pending on top by created order). */
export async function getRequests(): Promise<RequestRow[]> {
  const snap = await getAdminDb().collection("requests").orderBy("createdAt", "desc").limit(100).get();
  return snap.docs.map((d) => {
    const data = d.data();
    const ts = data.createdAt as { toMillis?: () => number } | undefined;
    return {
      id: d.id,
      userId: (data.userId as string) ?? "",
      userName: (data.userName as string) ?? "",
      title: (data.title as string) ?? "",
      note: (data.note as string) ?? "",
      status: (data.status as string) ?? "pending",
      reason: (data.reason as string) ?? "",
      movieId: (data.movieId as string) ?? "",
      createdAtMs: ts?.toMillis?.() ?? 0,
    };
  });
}
