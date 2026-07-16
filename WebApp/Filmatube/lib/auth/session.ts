import "server-only";

import { cache } from "react";
import { cookies } from "next/headers";
import { getAdminAuth } from "@/lib/firebase-admin";

const SESSION_COOKIE = "__session";
const EXPIRES_IN_MS = 60 * 60 * 24 * 5 * 1000; // 5 days

/** Exchange a Firebase ID token for an httpOnly session cookie. */
export async function createSession(idToken: string): Promise<void> {
  const sessionCookie = await getAdminAuth().createSessionCookie(idToken, {
    expiresIn: EXPIRES_IN_MS,
  });
  const store = await cookies();
  store.set(SESSION_COOKIE, sessionCookie, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: EXPIRES_IN_MS / 1000,
  });
}

/** Remove the session cookie (logout). */
export async function clearSession(): Promise<void> {
  const store = await cookies();
  store.delete(SESSION_COOKIE);
}

/**
 * Returns the decoded session (uid, claims) if a valid cookie is present, else null.
 *
 * - `cache()` memoizes the result per request, so the layout + page calling this in one
 *   navigation only verify once.
 * - Verification is **local** (no `checkRevoked`) so page gating doesn't make a Firebase
 *   network round-trip on every navigation. Sign-out clears the cookie; sensitive server
 *   actions/routes re-assert the admin claim independently. (Revocation lands within the
 *   5-day cookie lifetime.)
 */
export const getCurrentUser = cache(async () => {
  const store = await cookies();
  const cookie = store.get(SESSION_COOKIE)?.value;
  if (!cookie) return null;
  try {
    const decoded = await getAdminAuth().verifySessionCookie(cookie);
    // A banned account is treated as signed out, mirroring the Firestore `isSignedIn()` rule.
    // Caveat: claims are baked into the cookie when it is minted, so this catches accounts
    // banned *before* their session started. Banning someone mid-session revokes their refresh
    // tokens (so they can never mint a new cookie) and Firestore rules reject all their data
    // access immediately — but this cookie stays technically valid until it expires. Closing
    // that last gap needs verifySessionCookie(cookie, true), which costs a network round-trip
    // per navigation; see the note above.
    if (decoded.banned === true) return null;
    return decoded;
  } catch {
    return null;
  }
});
