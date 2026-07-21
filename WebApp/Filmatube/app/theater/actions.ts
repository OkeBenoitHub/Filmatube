"use server";

import { revalidatePath } from "next/cache";
import { FieldValue } from "firebase-admin/firestore";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";

async function requireUser() {
  const user = await getCurrentUser();
  if (!user) throw new Error("Unauthorized");
  return user;
}

function attendeeRef(showtimeId: string, uid: string) {
  return getAdminDb().collection("showtimes").doc(showtimeId).collection("attendees").doc(uid);
}

function waitlistRef(showtimeId: string, uid: string) {
  return getAdminDb().collection("showtimes").doc(showtimeId).collection("waitlist").doc(uid);
}

/** What happened when you asked for a seat — the UI says different things for each. */
export type RsvpResult = "going" | "waitlisted" | "left";

/**
 * A private screening belongs to one board; only its members may join.
 *
 * Checked server-side as well as in rules because the RSVP goes through this action with
 * admin credentials, which bypass rules entirely — without this, a crafted request could
 * put a non-member into a board-only room.
 */
async function assertCanJoin(
  showtime: FirebaseFirestore.DocumentSnapshot,
  uid: string,
): Promise<void> {
  const boardId = (showtime.get("boardId") as string) ?? "";
  if (!boardId) return;

  const board = await getAdminDb().collection("boards").doc(boardId).get();
  const members = (board.get("memberIds") as string[]) ?? [];
  if (!members.includes(uid) && board.get("ownerId") !== uid) {
    throw new Error("This screening is private to its board");
  }
}

/**
 * RSVP to a showtime, or cancel it.
 *
 * Only the attendee doc is written: `attendeesCount` is owned by the `syncShowtimeAttendees`
 * Cloud Function, because showtime docs stay admin-writable. Same write shape as Android
 * `TheaterRepository.setRsvp`, so both clients share the room.
 */
export async function setRsvp(showtimeId: string, going: boolean): Promise<RsvpResult> {
  const user = await requireUser();
  const db = getAdminDb();

  if (!going) {
    // Leaving frees a seat; `promoteFromWaitlist` picks that up and pulls the next person in.
    await Promise.all([
      attendeeRef(showtimeId, user.uid).delete(),
      waitlistRef(showtimeId, user.uid).delete(),
    ]);
    revalidatePath(`/theater/${showtimeId}`);
    revalidatePath("/theater");
    return "left";
  }

  const showtime = await db.collection("showtimes").doc(showtimeId).get();
  if (!showtime.exists) throw new Error("Showtime not found");
  if (showtime.get("status") === "ended") throw new Error("Showing has ended");
  await assertCanJoin(showtime, user.uid);

  const already = (await attendeeRef(showtimeId, user.uid).get()).exists;
  // Capacity is checked here as well as in the UI: the button state is a snapshot from render
  // time, and a room can fill between the page load and the click.
  const capacity = (showtime.get("capacity") as number) ?? 0;
  const count = (showtime.get("attendeesCount") as number) ?? 0;

  let result: RsvpResult = "going";
  if (!already && capacity > 0 && count >= capacity) {
    // Queue rather than refuse. A sold-out premiere is exactly when someone most wants to be
    // told a seat opened up, and cancellations are common.
    await waitlistRef(showtimeId, user.uid).set({
      userId: user.uid,
      joinedAt: FieldValue.serverTimestamp(),
    });
    result = "waitlisted";
  } else {
    // Opting in turns reminders on by default; that's the point of the RSVP.
    await attendeeRef(showtimeId, user.uid).set({
      rsvp: true,
      remind: true,
      joinedAt: FieldValue.serverTimestamp(),
    });
  }

  revalidatePath(`/theater/${showtimeId}`);
  revalidatePath("/theater");
  return result;
}

/** Toggle just the "remind me" flag, leaving the RSVP itself alone. */
export async function setRemind(showtimeId: string, remind: boolean): Promise<void> {
  const user = await requireUser();
  await attendeeRef(showtimeId, user.uid).set({ remind }, { merge: true });
  revalidatePath(`/theater/${showtimeId}`);
}
