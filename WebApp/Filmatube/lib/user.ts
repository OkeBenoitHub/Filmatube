import "server-only";

import type { DecodedIdToken } from "firebase-admin/auth";
import { FieldValue } from "firebase-admin/firestore";
import { getAdminDb } from "@/lib/firebase-admin";

/**
 * Creates `users/{uid}` with defaults on first sign-in, otherwise refreshes `lastActiveAt`.
 * Mirrors the Android UserRepository so both platforms produce identical documents.
 */
export async function ensureUserDocument(user: DecodedIdToken): Promise<void> {
  const ref = getAdminDb().collection("users").doc(user.uid);
  const snapshot = await ref.get();

  if (!snapshot.exists) {
    await ref.set({
      email: user.email ?? null,
      displayName: user.name ?? user.email?.split("@")[0] ?? "Filmatube user",
      bio: "",
      avatarUrl: user.picture ?? "",
      language: "en",
      followersCount: 0,
      followingCount: 0,
      genrePreferences: [],
      contentLanguage: "both",
      tasteCompleted: false,
      isAdmin: false,
      isBanned: false,
      createdAt: FieldValue.serverTimestamp(),
      lastActiveAt: FieldValue.serverTimestamp(),
    });
  } else {
    await ref.update({ lastActiveAt: FieldValue.serverTimestamp() });
  }
}
