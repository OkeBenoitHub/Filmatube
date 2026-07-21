/**
 * The theater's shared vocabulary: types, status constants and the position formula.
 *
 * Deliberately free of `server-only` and of any Firestore import so client components can
 * pull in real values (not just erased types) — the admin lineup needs `SHOWTIME_STATUS`,
 * and the sync engines need `playbackPositionMs`. Server-side reads live in `lib/theater.ts`.
 */

/** Lifecycle states — mirrors Android `ShowtimeStatus`. */
export const SHOWTIME_STATUS = {
  SCHEDULED: "scheduled",
  LOBBY: "lobby",
  LIVE: "live",
  ENDED: "ended",
} as const;

/** Everything the theater lists: anything not yet over. */
export const OPEN_STATUSES = [
  SHOWTIME_STATUS.SCHEDULED,
  SHOWTIME_STATUS.LOBBY,
  SHOWTIME_STATUS.LIVE,
];

/** One public screening. Mirrors the Android `Showtime` model. */
export interface Showtime {
  id: string;
  movieId: string;
  movieTitle: string;
  posterUrl: string;
  backdropUrl: string;
  startAtMs: number;
  status: string;
  isPremiere: boolean;
  capacity: number;
  attendeesCount: number;
  /**
   * Non-zero while an admin has the showing paused; the effective clock freezes here.
   *
   * The theater has no host, so a pause can't be "somebody's player stopped" — instead it's
   * an edit to the schedule itself. See [playbackPositionMs].
   */
  pausedAtMs: number;
}

export interface ShowtimeAttendee {
  uid: string;
  name: string;
  avatar: string;
}

/** My own RSVP state for a showtime. */
export interface Attendance {
  going: boolean;
  remind: boolean;
}

export function isOpen(s: Showtime): boolean {
  return s.status === SHOWTIME_STATUS.LOBBY || s.status === SHOWTIME_STATUS.LIVE;
}

export function isLive(s: Showtime): boolean {
  return s.status === SHOWTIME_STATUS.LIVE;
}

/** 0 capacity means unlimited, so a sold-out room needs an explicit cap. */
export function isFull(s: Showtime): boolean {
  return s.capacity > 0 && s.attendeesCount >= s.capacity;
}

/**
 * Where the film should be, for everyone, right now.
 *
 * `startAt` is the anchor and `pausedAt` freezes the clock against it. Resuming shifts
 * `startAt` forward by the paused duration, which is why this stays a pure function of the
 * two fields — no accumulated pause bookkeeping to drift out of step.
 */
export function playbackPositionMs(s: Showtime, serverNowMs: number): number {
  const effectiveNow = s.pausedAtMs > 0 ? s.pausedAtMs : serverNowMs;
  return Math.max(0, effectiveNow - s.startAtMs);
}
