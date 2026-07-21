import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";
import { SHOWTIME_STATUS, type Showtime } from "@/lib/theater";

/** A showtime plus the numbers only the admin lineup cares about. */
export interface AdminShowtime extends Showtime {
  /** People who actually turned up, from the presence subcollection. */
  presentCount: number;
  /** Chat lines posted in the room — a cheap proxy for how lively the screening was. */
  chatCount: number;
  /** Of the RSVPs, how many showed. Null when nobody RSVP'd (0/0 isn't 0%). */
  turnoutRate: number | null;
}

function toMillis(v: unknown): number {
  return (v as { toMillis?: () => number })?.toMillis?.() ?? 0;
}

/**
 * The whole lineup for the admin table — every status, newest showtime first.
 *
 * Unlike the public `getLineup` this deliberately includes `ended`, because the admin view
 * doubles as the attendance record.
 */
export async function getAdminShowtimes(limit = 100): Promise<AdminShowtime[]> {
  const snap = await getAdminDb().collection("showtimes").orderBy("startAt", "desc").limit(limit).get();

  // Counts come from aggregation queries rather than reading the documents: a popular
  // premiere can hold thousands of presence and chat docs, and the table only needs a number.
  return Promise.all(
    snap.docs.map(async (d) => {
      const x = d.data();
      const [presence, chat] = await Promise.all([
        d.ref.collection("presence").count().get(),
        d.ref.collection("chat").count().get(),
      ]);
      const presentCount = presence.data().count;
      const attendeesCount = (x.attendeesCount as number) ?? 0;

      return {
        id: d.id,
        movieId: (x.movieId as string) ?? "",
        movieTitle: (x.movieTitle as string) ?? "",
        posterUrl: (x.posterUrl as string) ?? "",
        backdropUrl: (x.backdropUrl as string) ?? "",
        startAtMs: toMillis(x.startAt),
        status: (x.status as string) ?? SHOWTIME_STATUS.SCHEDULED,
        isPremiere: (x.isPremiere as boolean) ?? false,
        capacity: (x.capacity as number) ?? 0,
        attendeesCount,
        pausedAtMs: toMillis(x.pausedAt),
        presentCount,
        chatCount: chat.data().count,
        turnoutRate: attendeesCount > 0 ? presentCount / attendeesCount : null,
      };
    }),
  );
}

/** Movies the CMS can schedule — published, non-coming-soon, title already localized. */
export interface SchedulableMovie {
  id: string;
  title: string;
  posterUrl: string;
  backdropUrl: string;
}
