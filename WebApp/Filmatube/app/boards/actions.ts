"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { FieldValue } from "firebase-admin/firestore";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";
import { BOARD_TYPES } from "@/lib/boards";

async function requireUser() {
  const user = await getCurrentUser();
  if (!user) throw new Error("Unauthorized");
  return user;
}

/** Board owners (and admins) may moderate a board — mirrors the isBoardOwner() rule. */
async function assertCanModerate(boardId: string, user: { uid: string; admin?: unknown }) {
  if (user.admin === true) return;
  const doc = await getAdminDb().collection("boards").doc(boardId).get();
  if (!doc.exists || doc.get("ownerId") !== user.uid) throw new Error("Forbidden");
}

export interface NewBoard {
  title: string;
  description: string;
  type: string;
  isPublic: boolean;
  coverUrl: string;
}

/**
 * Creates a board owned by the caller (added as the first member) and redirects to it in the
 * SAME action round-trip. The write shape mirrors Android `BoardRepository.createBoard` so both
 * clients read the same documents.
 */
export async function createBoard(values: NewBoard): Promise<never> {
  const user = await requireUser();
  const title = values.title.trim();
  if (!title) throw new Error("Title required");
  const type = values.type === BOARD_TYPES.MOVIE ? BOARD_TYPES.MOVIE : BOARD_TYPES.GENERAL;

  const db = getAdminDb();
  const ref = db.collection("boards").doc();
  await ref.set({
    title,
    description: values.description.trim(),
    type,
    coverUrl: values.coverUrl,
    isPublic: values.isPublic,
    isFeatured: false,
    isOfficial: false,
    ownerId: user.uid,
    memberIds: [user.uid],
    memberCount: 1,
    createdAt: FieldValue.serverTimestamp(),
  });
  await ref.collection("members").doc(user.uid).set({
    userId: user.uid,
    role: "owner",
    joinedAt: FieldValue.serverTimestamp(),
  });

  revalidatePath("/boards");
  // redirect() throws NEXT_REDIRECT — the client transition navigates without a second round-trip.
  redirect(`/boards/${ref.id}`);
}

/** Owner/admin: mute or unmute a member. A muted member can't post (enforced by canPost()). */
export async function setBoardMemberMuted(boardId: string, uid: string, muted: boolean): Promise<void> {
  const user = await requireUser();
  await assertCanModerate(boardId, user);
  await getAdminDb().collection("boards").doc(boardId).collection("members").doc(uid).set({ muted }, { merge: true });
  revalidatePath(`/boards/${boardId}/members`);
}

/** Owner/admin: remove a member — deletes the member doc and drops them from memberIds. */
export async function removeBoardMember(boardId: string, uid: string): Promise<void> {
  const user = await requireUser();
  await assertCanModerate(boardId, user);

  const db = getAdminDb();
  const board = db.collection("boards").doc(boardId);
  if ((await board.get()).get("ownerId") === uid) throw new Error("Cannot remove the owner");

  const batch = db.batch();
  batch.update(board, { memberIds: FieldValue.arrayRemove(uid), memberCount: FieldValue.increment(-1) });
  batch.delete(board.collection("members").doc(uid));
  await batch.commit();

  revalidatePath(`/boards/${boardId}/members`);
  revalidatePath(`/boards/${boardId}`);
}
