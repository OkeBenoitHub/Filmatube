"use server";

import { revalidatePath } from "next/cache";
import { FieldValue } from "firebase-admin/firestore";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";
import { deliverBroadcast, resolveRecipients, type Segment } from "@/lib/admin/broadcast";

async function assertAdmin() {
  const user = await getCurrentUser();
  if (!user || user.admin !== true) throw new Error("Forbidden");
  return user;
}

export interface BroadcastInput {
  title: string;
  body: string;
  segment: Segment;
  genre: string;
  movieId: string;
  scheduledAt: string; // ISO string or ""
}

/** Count recipients for a segment (for the composer preview). */
export async function previewRecipients(segment: Segment, genre: string): Promise<number> {
  await assertAdmin();
  const uids = await resolveRecipients(segment, genre);
  return uids.length;
}

/**
 * Create a broadcast. If unscheduled, fan it out immediately (inbox + FCM) and mark it sent.
 * If scheduled, store it as `scheduled` for the Cloud Function to process at the due time.
 */
export async function createBroadcast(input: BroadcastInput): Promise<{ sent: boolean; delivered: number }> {
  const admin = await assertAdmin();
  const scheduled = input.scheduledAt ? new Date(input.scheduledAt) : null;
  const isFuture = !!scheduled && scheduled.getTime() > Date.now();

  const doc = getAdminDb().collection("broadcasts").doc();
  await doc.set({
    title: input.title.trim(),
    body: input.body.trim(),
    segment: input.segment,
    genre: input.genre,
    movieId: input.movieId,
    scheduledAt: scheduled ? scheduled : null,
    status: isFuture ? "scheduled" : "queued",
    createdBy: admin.uid,
    createdAt: FieldValue.serverTimestamp(),
  });

  if (isFuture) {
    revalidatePath("/admin/notifications");
    return { sent: false, delivered: 0 };
  }

  const uids = await resolveRecipients(input.segment, input.genre);
  const delivered = await deliverBroadcast(uids, input.title.trim(), input.body.trim(), input.movieId);
  await doc.update({ status: "sent", sentAt: FieldValue.serverTimestamp(), recipientCount: delivered });

  revalidatePath("/admin/notifications");
  return { sent: true, delivered };
}
