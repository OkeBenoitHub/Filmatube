import "server-only";

import { FieldValue } from "firebase-admin/firestore";
import { getAdminDb, getAdminMessaging } from "@/lib/firebase-admin";

export type Segment = "all" | "taste" | "active";

const MAX_RECIPIENTS = 1000;
const ACTIVE_DAYS = 14;

function chunk<T>(arr: T[], size: number): T[][] {
  const out: T[][] = [];
  for (let i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size));
  return out;
}

/** Resolve the target user ids for a broadcast segment. */
export async function resolveRecipients(segment: Segment, genre: string): Promise<string[]> {
  const users = getAdminDb().collection("users");
  let query = users.limit(MAX_RECIPIENTS);

  if (segment === "taste" && genre) {
    query = users.where("genrePreferences", "array-contains", genre).limit(MAX_RECIPIENTS);
  } else if (segment === "active") {
    const cutoff = new Date(Date.now() - ACTIVE_DAYS * 24 * 3600e3);
    query = users.where("lastActiveAt", ">=", cutoff).limit(MAX_RECIPIENTS);
  }

  const snap = await query.get();
  return snap.docs.filter((d) => d.get("isBanned") !== true).map((d) => d.id);
}

/**
 * Deliver a broadcast to [uids]: writes a `system` notification into each inbox (honoring the
 * user's system-channel opt-in) and sends an FCM push to their registered tokens.
 * Returns the number of inboxes written.
 */
export async function deliverBroadcast(
  uids: string[],
  title: string,
  body: string,
  movieId = "",
): Promise<number> {
  const db = getAdminDb();
  let delivered = 0;
  const allTokens: string[] = [];

  for (const group of chunk(uids, 400)) {
    const batch = db.batch();
    await Promise.all(
      group.map(async (uid) => {
        const settings = await db.collection("users").doc(uid).collection("settings").doc("notifications").get();
        if (settings.exists && settings.get("system") === false) return;

        const ref = db.collection("users").doc(uid).collection("notifications").doc();
        batch.set(ref, {
          type: "system",
          actorId: "",
          actorName: "Filmatube",
          actorAvatar: "",
          title,
          message: body,
          movieId,
          movieTitle: "",
          read: false,
          createdAt: FieldValue.serverTimestamp(),
        });
        delivered += 1;

        const tokensSnap = await db.collection("users").doc(uid).collection("fcmTokens").get();
        tokensSnap.forEach((t) => allTokens.push(t.id));
      }),
    );
    await batch.commit();
  }

  // Best-effort FCM push (needs a Web Push cert / valid tokens; ignore failures).
  if (allTokens.length > 0) {
    for (const group of chunk(allTokens, 500)) {
      try {
        await getAdminMessaging().sendEachForMulticast({
          tokens: group,
          notification: { title, body },
          data: { category: "system", route: movieId ? `/movie/${movieId}` : "/notifications" },
        });
      } catch {
        /* delivery failures are non-fatal */
      }
    }
  }

  return delivered;
}
