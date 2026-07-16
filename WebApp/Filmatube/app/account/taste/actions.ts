"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { requireUser } from "@/lib/auth/guards";
import { getAdminDb } from "@/lib/firebase-admin";
import { GENRE_KEYS } from "@/lib/genres";
import { LOCALE_COOKIE, type Locale } from "@/lib/i18n/config";

const CONTENT_LANGUAGES = ["en", "fr", "both"];

export interface TasteValues {
  genres: string[];
  contentLanguage: string;
  appLanguage: string;
}

/**
 * Saves onboarding preferences and enters the app in one round-trip.
 *
 * This runs on the server rather than writing with the client Firestore SDK: a client write
 * only settles once the backend acknowledges it, so a browser that can't reach Firestore
 * leaves the promise pending forever — an infinite spinner with no error. Going through the
 * session cookie also removes any dependency on the client SDK's auth state matching it.
 *
 * The Admin SDK bypasses security rules, so every field is whitelisted here: only the four
 * preference fields are written, never isAdmin/isBanned.
 */
export async function saveTaste(values: TasteValues): Promise<{ error: "invalid" | "save" } | void> {
  const user = await requireUser();

  const genres = values.genres.filter((g) => (GENRE_KEYS as readonly string[]).includes(g));
  if (genres.length === 0) return { error: "invalid" };

  const contentLanguage = CONTENT_LANGUAGES.includes(values.contentLanguage) ? values.contentLanguage : "both";
  const appLanguage: Locale = values.appLanguage === "fr" ? "fr" : "en";

  try {
    await getAdminDb().collection("users").doc(user.uid).set(
      {
        genrePreferences: genres,
        contentLanguage,
        language: appLanguage,
        tasteCompleted: true,
      },
      { merge: true },
    );
  } catch {
    return { error: "save" };
  }

  // The locale is resolved server-side from this cookie, so set it here rather than from the
  // client — one less round-trip, and it's already applied when /home renders.
  const store = await cookies();
  store.set(LOCALE_COOKIE, appLanguage, { path: "/", maxAge: 31536000, sameSite: "lax" });

  // redirect() throws NEXT_REDIRECT, so it must sit outside the try/catch above.
  redirect("/home");
}
