import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

/** The set of badge ids a user has unlocked (from achievements/{uid}/badges). */
export async function getUserBadges(uid: string): Promise<Set<string>> {
  const snap = await getAdminDb().collection("achievements").doc(uid).collection("badges").get();
  return new Set(snap.docs.map((d) => d.id));
}
