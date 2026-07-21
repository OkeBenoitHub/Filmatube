"use server";

import { revalidatePath } from "next/cache";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";
import { getLocale } from "@/lib/i18n/server";
import { getMovie, localized } from "@/lib/movies";
import { SHOWTIME_STATUS } from "@/lib/theater";

/** How often a showtime repeats; the automation spawns the next one when it ends. */
export const RECURRENCES = ["none", "daily", "weekly"];

async function assertAdmin() {
  const user = await getCurrentUser();
  if (!user || user.admin !== true) throw new Error("Forbidden");
}

function refresh(id?: string) {
  revalidatePath("/admin/theater");
  revalidatePath("/theater");
  if (id) revalidatePath(`/theater/${id}`);
}

/**
 * Schedule a screening. Same document shape both clients already read, so an Android
 * viewer sees it on the lineup as soon as this returns.
 */
export async function createShowtime(input: {
  movieId: string;
  startAtMs: number;
  capacity: number;
  isPremiere: boolean;
  recurrence?: string;
}): Promise<void> {
  await assertAdmin();

  const [movie, locale] = await Promise.all([getMovie(input.movieId), getLocale()]);
  if (!movie) throw new Error("Movie not found");
  if (!Number.isFinite(input.startAtMs) || input.startAtMs <= 0) throw new Error("Invalid start time");

  const recurrence = RECURRENCES.includes(input.recurrence ?? "none") ? (input.recurrence ?? "none") : "none";

  await getAdminDb().collection("showtimes").add({
    movieId: input.movieId,
    // Denormalized so the lineup renders without joining the catalog — the same reason
    // parties carry movieTitle/moviePoster.
    movieTitle: localized(movie.title, locale),
    posterUrl: movie.posterUrl,
    backdropUrl: movie.backdropUrl ?? "",
    startAt: Timestamp.fromMillis(input.startAtMs),
    status: SHOWTIME_STATUS.SCHEDULED,
    isPremiere: input.isPremiere,
    // 0 means unlimited, matching `isFull`.
    capacity: Math.max(0, Math.floor(input.capacity)),
    attendeesCount: 0,
    // Denormalized so the Day 170 automation can end the showing without loading the movie.
    // It's also what makes "ended" self-correcting across an admin pause: pausing shifts
    // startAt forward, so startAt + durationMs moves with it.
    durationMs: Math.max(0, (movie.duration ?? 0) * 60_000),
    recurrence,
    createdAt: FieldValue.serverTimestamp(),
  });

  refresh();
}

/** Move a showtime through its lifecycle by hand (the Day 170 automation will do this too). */
export async function setShowtimeStatus(id: string, status: string): Promise<void> {
  await assertAdmin();
  const allowed: string[] = [
    SHOWTIME_STATUS.SCHEDULED,
    SHOWTIME_STATUS.LOBBY,
    SHOWTIME_STATUS.LIVE,
    SHOWTIME_STATUS.ENDED,
  ];
  if (!allowed.includes(status)) throw new Error("Invalid status");

  // Opening the doors clears any leftover pause, so a room reopened after being paused
  // doesn't start frozen with no obvious way for the audience to tell why.
  const patch: Record<string, unknown> = { status };
  if (status === SHOWTIME_STATUS.LIVE) patch.pausedAt = FieldValue.delete();

  await getAdminDb().collection("showtimes").doc(id).update(patch);
  refresh(id);
}

/**
 * Hold the whole room.
 *
 * There is no host to stop, so a pause is an edit to the schedule: freeze the effective
 * clock at `pausedAt` and every viewer independently holds at `pausedAt - startAt`.
 */
export async function pauseShowtime(id: string): Promise<void> {
  await assertAdmin();
  await getAdminDb().collection("showtimes").doc(id).update({
    pausedAt: Timestamp.now(),
  });
  refresh(id);
}

/**
 * Release the hold.
 *
 * `startAt` shifts forward by however long the pause lasted, so the room resumes exactly
 * where it froze instead of jumping ahead by the length of the intermission.
 */
export async function resumeShowtime(id: string): Promise<void> {
  await assertAdmin();
  const db = getAdminDb();
  const ref = db.collection("showtimes").doc(id);

  // A transaction because this is read-modify-write on startAt: two admins hitting resume
  // together would otherwise each add their own pause duration and skip the film forward.
  await db.runTransaction(async (tx) => {
    const doc = await tx.get(ref);
    if (!doc.exists) throw new Error("Showtime not found");
    const pausedAt = doc.get("pausedAt") as Timestamp | undefined;
    if (!pausedAt) return; // not paused — nothing to release
    const startAt = doc.get("startAt") as Timestamp | undefined;
    if (!startAt) throw new Error("Showtime has no start time");

    const pausedForMs = Date.now() - pausedAt.toMillis();
    tx.update(ref, {
      startAt: Timestamp.fromMillis(startAt.toMillis() + pausedForMs),
      pausedAt: FieldValue.delete(),
    });
  });

  refresh(id);
}

/**
 * Jump the room forward (positive) or back (negative) by [seconds].
 *
 * Moving the film forward means moving its start time *earlier* — position is
 * `now - startAt`, so the two run in opposite directions.
 */
export async function skipShowtime(id: string, seconds: number): Promise<void> {
  await assertAdmin();
  if (!Number.isFinite(seconds) || seconds === 0) throw new Error("Invalid skip");

  const db = getAdminDb();
  const ref = db.collection("showtimes").doc(id);

  await db.runTransaction(async (tx) => {
    const doc = await tx.get(ref);
    if (!doc.exists) throw new Error("Showtime not found");
    const startAt = doc.get("startAt") as Timestamp | undefined;
    if (!startAt) throw new Error("Showtime has no start time");

    const next = startAt.toMillis() - seconds * 1000;
    // Clamp: skipping back past the scheduled start would put the room at a negative
    // position, which every client floors to 0 anyway — this keeps the doc honest.
    const paused = doc.get("pausedAt") as Timestamp | undefined;
    const ceiling = paused ? paused.toMillis() : Date.now();
    tx.update(ref, { startAt: Timestamp.fromMillis(Math.min(next, ceiling)) });
  });

  refresh(id);
}

/** Remove a showtime and everything under it (attendees, chat, reactions, presence). */
export async function deleteShowtime(id: string): Promise<void> {
  await assertAdmin();
  const db = getAdminDb();
  await db.recursiveDelete(db.collection("showtimes").doc(id));
  refresh(id);
}
