"use server";

import { revalidatePath } from "next/cache";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";
import { deliverBroadcast } from "@/lib/admin/broadcast";

async function assertAdmin() {
  const user = await getCurrentUser();
  if (!user || user.admin !== true) throw new Error("Forbidden");
}

/**
 * Approve or reject a content request: updates status + reason, optionally links an added
 * movie, and notifies the requester (in-app inbox + FCM) about the decision.
 */
export async function decideRequest(
  id: string,
  status: "approved" | "rejected",
  reason: string,
  movieId: string,
) {
  await assertAdmin();
  const ref = getAdminDb().collection("requests").doc(id);
  const snap = await ref.get();
  if (!snap.exists) return;

  await ref.update({ status, reason: reason.trim(), movieId: movieId.trim() });

  const requesterId = snap.get("userId") as string | undefined;
  const title = snap.get("title") as string | undefined;
  if (requesterId) {
    const heading = status === "approved" ? "Request approved" : "Request declined";
    const body = reason.trim() || (title ?? "");
    await deliverBroadcast([requesterId], heading, body, movieId.trim());
  }

  revalidatePath("/admin/requests");
}
