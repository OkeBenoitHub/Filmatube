import "server-only";

import { createHash } from "node:crypto";
import { FieldValue } from "firebase-admin/firestore";
import { getAdminDb } from "@/lib/firebase-admin";

/** Short, salted hash of a signup IP — a clustering signal, not a recoverable address. */
function hashIp(ip?: string | null): string {
  if (!ip) return "";
  const salt = process.env.REFERRAL_IP_SALT ?? "filmatube";
  return createHash("sha256").update(`${salt}:${ip}`).digest("hex").slice(0, 16);
}

/**
 * Referrals (v1.3, Day 197).
 *
 * A referral code is simply the referrer's uid — unguessable and needs no lookup table. An
 * invite link is `${origin}/invite/${uid}`; visiting it drops a cookie, and when the visitor
 * signs up the server attributes the referral once. `referrals/{referredId}` is keyed by the
 * *referred* user, so nobody can be referred twice, and it's written server-side only.
 *
 * Cookie name / link helper live in `referral-shared` (client-safe); re-exported for callers.
 */
export { REF_COOKIE, REF_COOKIE_MAX_AGE, inviteUrl } from "@/lib/referral-shared";

export interface Referral {
  referredId: string;
  referrerId: string;
  status: "completed";
  createdAtMs: number;
}

/**
 * Attribute a signup to a referrer, once. No-op (returns false) when the code is self-referral,
 * the referrer doesn't exist, or the referred user was already attributed. Server-only; the
 * rules block client writes so a referral can't be fabricated.
 */
export async function recordReferral(
  referrerId: string,
  referredId: string,
  ip?: string | null,
): Promise<boolean> {
  if (!referrerId || referrerId === referredId) return false;
  const db = getAdminDb();

  const existing = await db.collection("referrals").doc(referredId).get();
  if (existing.exists) return false;

  const referrer = await db.collection("users").doc(referrerId).get();
  if (!referrer.exists) return false;

  await db.collection("referrals").doc(referredId).set({
    referrerId,
    referredId,
    status: "completed",
    // Hashed so it's a fraud signal (which referrals share a signup network), never a stored
    // address. Admin analytics flags a referrer whose referred accounts cluster on one hash.
    ipHash: hashIp(ip),
    createdAt: FieldValue.serverTimestamp(),
  });
  return true;
}

/** People this user has successfully referred (newest first). */
export async function getReferralsBy(referrerId: string): Promise<Referral[]> {
  const snap = await getAdminDb().collection("referrals").where("referrerId", "==", referrerId).limit(200).get();
  return snap.docs
    .map((d) => ({
      referredId: (d.get("referredId") as string) ?? d.id,
      referrerId: (d.get("referrerId") as string) ?? "",
      status: "completed" as const,
      createdAtMs: d.get("createdAt")?.toMillis?.() ?? 0,
    }))
    .sort((a, b) => b.createdAtMs - a.createdAtMs);
}

/** Who referred this user, if anyone. */
export async function getReferrerOf(referredId: string): Promise<string | null> {
  const doc = await getAdminDb().collection("referrals").doc(referredId).get();
  return doc.exists ? ((doc.get("referrerId") as string) ?? null) : null;
}
