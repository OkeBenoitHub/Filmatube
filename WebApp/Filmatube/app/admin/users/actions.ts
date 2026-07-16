"use server";

import { revalidatePath } from "next/cache";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminAuth, getAdminDb } from "@/lib/firebase-admin";

async function assertAdmin() {
  const user = await getCurrentUser();
  if (!user || user.admin !== true) throw new Error("Forbidden");
  return user;
}

/**
 * Ban or unban a user.
 *
 * The ban lives in a custom **claim**, not the users doc: a user can write their own doc, so a
 * doc flag would be self-serve. Rules treat a banned claim as "not signed in" (see isSignedIn),
 * which denies every read and write in one place.
 *
 * Revoking refresh tokens is what makes it bite: the client SDK then has to mint a fresh ID
 * token, which carries the new claim, so Firestore starts rejecting them (immediately on
 * re-auth, otherwise at the next hourly refresh).
 *
 * `users/{uid}.isBanned` is still mirrored — it's what the suggestion/broadcast filters read —
 * but it is NOT the source of truth.
 */
export async function setUserBanned(uid: string, banned: boolean): Promise<void> {
  const admin = await assertAdmin();
  if (uid === admin.uid) throw new Error("You cannot ban yourself");

  const auth = getAdminAuth();
  const existing = (await auth.getUser(uid)).customClaims ?? {};
  // Preserve other claims (notably `admin`) — setCustomUserClaims replaces the whole object.
  await auth.setCustomUserClaims(uid, { ...existing, banned: banned || undefined });
  await auth.revokeRefreshTokens(uid);

  await getAdminDb().collection("users").doc(uid).set({ isBanned: banned }, { merge: true });
  revalidatePath("/admin/users");
}
