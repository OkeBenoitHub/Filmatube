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
