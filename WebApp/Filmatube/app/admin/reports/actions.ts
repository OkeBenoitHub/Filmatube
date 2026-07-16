"use server";

import { revalidatePath } from "next/cache";
import { FieldValue } from "firebase-admin/firestore";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";
import { reportedContentRef } from "@/lib/admin/reports";

async function assertAdmin() {
  const user = await getCurrentUser();
  if (!user || user.admin !== true) throw new Error("Forbidden");
}

/** Mark a report resolved or dismissed (keeps the record for audit). */
export async function setReportStatus(id: string, status: "resolved" | "dismissed") {
  await assertAdmin();
  await getAdminDb().collection("reports").doc(id).update({ status });
  revalidatePath("/admin/reports");
}

/** Delete the reported review/comment/board message, then mark the report resolved. */
export async function removeReportedContent(
  id: string,
  type: string,
  movieId: string,
  boardId: string,
  targetId: string,
) {
  await assertAdmin();
  const ref = reportedContentRef(type, movieId, boardId, targetId);
  if (ref) await ref.delete();
  await getAdminDb().collection("reports").doc(id).update({ status: "resolved" });
  revalidatePath("/admin/reports");
}

/** Mute the reported user in the board they were reported in, then resolve the report. */
export async function muteReportedMember(id: string, boardId: string, uid: string) {
  await assertAdmin();
  if (!boardId || !uid) throw new Error("Not a board report");
  const db = getAdminDb();
  await db.collection("boards").doc(boardId).collection("members").doc(uid).set({ muted: true }, { merge: true });
  await db.collection("reports").doc(id).update({ status: "resolved" });
  revalidatePath("/admin/reports");
  revalidatePath(`/boards/${boardId}/members`);
}

/** Remove the reported user from the board entirely, then resolve the report. */
export async function removeReportedMember(id: string, boardId: string, uid: string) {
  await assertAdmin();
  if (!boardId || !uid) throw new Error("Not a board report");

  const db = getAdminDb();
  const board = db.collection("boards").doc(boardId);
  if ((await board.get()).get("ownerId") === uid) throw new Error("Cannot remove the owner");

  const batch = db.batch();
  batch.update(board, { memberIds: FieldValue.arrayRemove(uid), memberCount: FieldValue.increment(-1) });
  batch.delete(board.collection("members").doc(uid));
  batch.update(db.collection("reports").doc(id), { status: "resolved" });
  await batch.commit();

  revalidatePath("/admin/reports");
  revalidatePath(`/boards/${boardId}`);
  revalidatePath(`/boards/${boardId}/members`);
}
