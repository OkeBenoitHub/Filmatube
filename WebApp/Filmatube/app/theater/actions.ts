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

/**
 * RSVP to a showtime, or cancel it.
 *
 * Only the attendee doc is written: `attendeesCount` is owned by the `syncShowtimeAttendees`
 * Cloud Function, because showtime docs stay admin-writable. Same write shape as Android
 * `TheaterRepository.setRsvp`, so both clients share the room.
 */
export async function setRsvp(showtimeId: string, going: boolean): Promise<void> {
  const user = await requireUser();

  if (going) {
    const showtime = await getAdminDb().collection("showtimes").doc(showtimeId).get();
    if (!showtime.exists) throw new Error("Showtime not found");
    if (showtime.get("status") === "ended") throw new Error("Showing has ended");

    // Capacity is checked here as well as in the UI: the button state is a snapshot, and a
    // room can fill between the page render and the click.
    const capacity = (showtime.get("capacity") as number) ?? 0;
    const count = (showtime.get("attendeesCount") as number) ?? 0;
    const already = (await attendeeRef(showtimeId, user.uid).get()).exists;
    if (!already && capacity > 0 && count >= capacity) throw new Error("Showing is full");

    // Opting in turns reminders on by default; that's the point of the RSVP.
    await attendeeRef(showtimeId, user.uid).set({
      rsvp: true,
      remind: true,
      joinedAt: FieldValue.serverTimestamp(),
    });
  } else {
    await attendeeRef(showtimeId, user.uid).delete();
  }

  revalidatePath(`/theater/${showtimeId}`);
  revalidatePath("/theater");
}

/** Toggle just the "remind me" flag, leaving the RSVP itself alone. */
export async function setRemind(showtimeId: string, remind: boolean): Promise<void> {
  const user = await requireUser();
  await attendeeRef(showtimeId, user.uid).set({ remind }, { merge: true });
  revalidatePath(`/theater/${showtimeId}`);
}
