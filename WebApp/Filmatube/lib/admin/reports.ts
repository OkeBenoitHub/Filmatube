import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

export interface ReportRow {
  id: string;
  type: string;
  movieId: string;
  targetId: string;
  reportedUserId: string;
  reporterId: string;
  reason: string;
  status: string;
  createdAtMs: number;
  targetText: string;
}

/** All moderation reports, newest first, enriched with the reported content's text. */
export async function getReports(): Promise<ReportRow[]> {
  const snap = await getAdminDb().collection("reports").orderBy("createdAt", "desc").limit(100).get();
  return Promise.all(
    snap.docs.map(async (d) => {
      const data = d.data();
      const type = (data.type as string) ?? "";
      const movieId = (data.movieId as string) ?? "";
      const targetId = (data.targetId as string) ?? "";
      let targetText = "";
      if (movieId && targetId) {
        const col = type === "comment" ? "comments" : "reviews";
        const target = await getAdminDb().collection(col).doc(movieId).collection("items").doc(targetId).get();
        targetText = (target.get("text") as string) ?? "";
      }
      const ts = data.createdAt as { toMillis?: () => number } | undefined;
      return {
        id: d.id,
        type,
        movieId,
        targetId,
        reportedUserId: (data.reportedUserId as string) ?? "",
        reporterId: (data.reporterId as string) ?? "",
        reason: (data.reason as string) ?? "",
        status: (data.status as string) ?? "pending",
        createdAtMs: ts?.toMillis?.() ?? 0,
        targetText,
      };
    }),
  );
}
