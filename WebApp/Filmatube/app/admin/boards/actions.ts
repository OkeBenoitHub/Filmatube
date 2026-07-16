"use server";

import { revalidatePath } from "next/cache";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";

async function assertAdmin() {
  const user = await getCurrentUser();
  if (!user || user.admin !== true) throw new Error("Forbidden");
}

/** Mark a board official — the verified badge shown next to its title. */
export async function setBoardOfficial(id: string, isOfficial: boolean): Promise<void> {
  await assertAdmin();
  await getAdminDb().collection("boards").doc(id).update({ isOfficial });
  revalidatePath("/admin/boards");
  revalidatePath("/boards");
}

/** Feature a board on the /boards discovery header. */
export async function setBoardFeatured(id: string, isFeatured: boolean): Promise<void> {
  await assertAdmin();
  await getAdminDb().collection("boards").doc(id).update({ isFeatured });
  revalidatePath("/admin/boards");
  revalidatePath("/boards");
}

/** Delete a board. Its messages/members subcollections are removed with it. */
export async function deleteBoard(id: string): Promise<void> {
  await assertAdmin();
  await getAdminDb().recursiveDelete(getAdminDb().collection("boards").doc(id));
  revalidatePath("/admin/boards");
  revalidatePath("/boards");
}
