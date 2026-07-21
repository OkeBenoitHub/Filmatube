import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

// The pure model (types, statuses, position formula) lives in a client-safe module so
// client components can import real values from it; re-exported here for existing callers.
export * from "@/lib/theater-model";

import {
  OPEN_STATUSES,
  SHOWTIME_STATUS,
  type Attendance,
  type Showtime,
  type ShowtimeAttendee,
} from "@/lib/theater-model";

function toMillis(v: unknown): number {
  return (v as { toMillis?: () => number })?.toMillis?.() ?? 0;
}

function mapShowtime(id: string, x: FirebaseFirestore.DocumentData): Showtime {
  return {
    id,
    movieId: (x.movieId as string) ?? "",
    movieTitle: (x.movieTitle as string) ?? "",
    posterUrl: (x.posterUrl as string) ?? "",
    backdropUrl: (x.backdropUrl as string) ?? "",
    startAtMs: toMillis(x.startAt),
    status: (x.status as string) ?? SHOWTIME_STATUS.SCHEDULED,
    isPremiere: (x.isPremiere as boolean) ?? false,
    capacity: (x.capacity as number) ?? 0,
    attendeesCount: (x.attendeesCount as number) ?? 0,
    pausedAtMs: toMillis(x.pausedAt),
  };
}

/**
 * The open lineup, soonest first — live and lobby rooms plus everything scheduled.
 *
 * One `in` query rather than one per status: it rides the existing (status, startAt)
 * composite index, and the page splits the result into "now showing" and "coming up".
 */
export async function getLineup(limit = 50): Promise<Showtime[]> {
  const snap = await getAdminDb()
    .collection("showtimes")
    .where("status", "in", OPEN_STATUSES)
    .orderBy("startAt", "asc")
    .limit(limit)
    .get();
  return snap.docs.map((d) => mapShowtime(d.id, d.data()));
}

/** A single showtime. Public — unlike a party, anyone signed in may look. */
export async function getShowtime(id: string): Promise<Showtime | null> {
  const doc = await getAdminDb().collection("showtimes").doc(id).get();
  if (!doc.exists) return null;
  return mapShowtime(doc.id, doc.data() ?? {});
}

/** Attendees joined with their profiles (attendee docs hold only rsvp/remind/joinedAt). */
export async function getShowtimeAttendees(showtimeId: string, limit = 24): Promise<ShowtimeAttendee[]> {
  const db = getAdminDb();
  const snap = await db.collection("showtimes").doc(showtimeId).collection("attendees").limit(limit).get();
  if (snap.empty) return [];

  const profiles = await db.getAll(...snap.docs.map((d) => db.collection("users").doc(d.id)));
  const byId = new Map(profiles.map((p) => [p.id, p]));

  return snap.docs.map((d) => {
    const profile = byId.get(d.id);
    return {
      uid: d.id,
      name: (profile?.get("displayName") as string) ?? "",
      avatar: (profile?.get("avatarUrl") as string) ?? "",
    };
  });
}

export async function getMyAttendance(showtimeId: string, uid: string): Promise<Attendance> {
  const doc = await getAdminDb()
    .collection("showtimes")
    .doc(showtimeId)
    .collection("attendees")
    .doc(uid)
    .get();
  if (!doc.exists) return { going: false, remind: false };
  return {
    going: (doc.get("rsvp") as boolean) ?? false,
    remind: (doc.get("remind") as boolean) ?? false,
  };
}
