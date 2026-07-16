import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

/** Report kinds the queue understands. `board_message` is written by both board clients. */
export const REPORT_TYPES = { REVIEW: "review", COMMENT: "comment", BOARD_MESSAGE: "board_message" } as const;

export interface ReportRow {
  id: string;
  type: string;
  movieId: string;
  boardId: string;
  boardTitle: string;
  targetId: string;
  reportedUserId: string;
  reporterId: string;
  reason: string;
  status: string;
  createdAtMs: number;
  targetText: string;
}

/**
 * Where a reported item lives, by report type — board messages sit in a different subtree
 * from reviews/comments, which is why the queue couldn't resolve them before.
 */
export function reportedContentRef(type: string, movieId: string, boardId: string, targetId: string) {
  const db = getAdminDb();
  if (type === REPORT_TYPES.BOARD_MESSAGE) {
    if (!boardId || !targetId) return null;
    return db.collection("boards").doc(boardId).collection("messages").doc(targetId);
  }
  if (!movieId || !targetId) return null;
  const col = type === REPORT_TYPES.COMMENT ? "comments" : "reviews";
  return db.collection(col).doc(movieId).collection("items").doc(targetId);
}

/** All moderation reports, newest first, enriched with the reported content's text. */
export async function getReports(): Promise<ReportRow[]> {
  const snap = await getAdminDb().collection("reports").orderBy("createdAt", "desc").limit(100).get();

  return Promise.all(
    snap.docs.map(async (d) => {
      const data = d.data();
      const type = (data.type as string) ?? "";
      const movieId = (data.movieId as string) ?? "";
      const boardId = (data.boardId as string) ?? "";
      const targetId = (data.targetId as string) ?? "";

      const ref = reportedContentRef(type, movieId, boardId, targetId);
      const [target, board] = await Promise.all([
        ref ? ref.get() : Promise.resolve(null),
        boardId ? getAdminDb().collection("boards").doc(boardId).get() : Promise.resolve(null),
      ]);

      // A board message can be a shared movie card with no text — fall back to its title.
      const text = (target?.get("text") as string) ?? "";

      const ts = data.createdAt as { toMillis?: () => number } | undefined;
      return {
        id: d.id,
        type,
        movieId,
        boardId,
        boardTitle: (board?.get("title") as string) ?? "",
        targetId,
        reportedUserId: (data.reportedUserId as string) ?? "",
        reporterId: (data.reporterId as string) ?? "",
        reason: (data.reason as string) ?? "",
        status: (data.status as string) ?? "pending",
        createdAtMs: ts?.toMillis?.() ?? 0,
        targetText: text || ((target?.get("movieTitle") as string) ?? ""),
      };
    }),
  );
}
