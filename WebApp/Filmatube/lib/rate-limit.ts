import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

/**
 * Fixed-window per-key rate limiter backed by Firestore (`rateLimits/{namespace}_{key}`).
 * Returns true if the request is allowed. Fails open (returns true) on limiter errors so a
 * transient Firestore issue never blocks legitimate traffic.
 */
export async function allowRequest(
  namespace: string,
  key: string,
  max: number,
  windowMs: number,
): Promise<boolean> {
  const db = getAdminDb();
  const ref = db.collection("rateLimits").doc(`${namespace}_${key}`);
  try {
    return await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      const now = Date.now();
      const data = snap.exists ? snap.data() : undefined;
      const windowStart = (data?.windowStart as number) ?? 0;
      const count = (data?.count as number) ?? 0;

      if (now - windowStart > windowMs) {
        tx.set(ref, { windowStart: now, count: 1 });
        return true;
      }
      if (count >= max) return false;
      tx.set(ref, { windowStart, count: count + 1 }, { merge: true });
      return true;
    });
  } catch {
    return true;
  }
}
