import "server-only";

import type { DecodedIdToken } from "firebase-admin/auth";
import { FieldValue } from "firebase-admin/firestore";
import { getAdminDb } from "@/lib/firebase-admin";

export interface UserProfileDoc {
  uid: string;
  email: string | null;
  displayName: string;
  bio: string;
  avatarUrl: string;
  language: string;
  followersCount: number;
  followingCount: number;
  isAdmin: boolean;
  tasteCompleted: boolean;
  genrePreferences: string[];
  contentLanguage: string;
}

/** Reads the `users/{uid}` document (server). */
export async function getUserProfile(uid: string): Promise<UserProfileDoc | null> {
  const snapshot = await getAdminDb().collection("users").doc(uid).get();
  if (!snapshot.exists) return null;
  const d = snapshot.data() ?? {};
  return {
    uid,
    email: d.email ?? null,
    displayName: d.displayName ?? "",
    bio: d.bio ?? "",
    avatarUrl: d.avatarUrl ?? "",
    language: d.language ?? "en",
    followersCount: d.followersCount ?? 0,
    followingCount: d.followingCount ?? 0,
    isAdmin: d.isAdmin ?? false,
    tasteCompleted: d.tasteCompleted ?? false,
    genrePreferences: d.genrePreferences ?? [],
    contentLanguage: d.contentLanguage ?? "both",
  };
}

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
