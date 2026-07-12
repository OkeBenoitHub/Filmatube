"use server";

import { revalidatePath } from "next/cache";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";

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

/** Delete the reported review/comment, then mark the report resolved. */
export async function removeReportedContent(
  id: string,
  type: string,
  movieId: string,
  targetId: string,
) {
  await assertAdmin();
  if (movieId && targetId) {
    const col = type === "comment" ? "comments" : "reviews";
    await getAdminDb().collection(col).doc(movieId).collection("items").doc(targetId).delete();
  }
  await getAdminDb().collection("reports").doc(id).update({ status: "resolved" });
  revalidatePath("/admin/reports");
}
