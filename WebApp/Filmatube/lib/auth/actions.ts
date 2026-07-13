import {
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
  getAdditionalUserInfo,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  updateProfile,
  type User,
} from "firebase/auth";
import { auth } from "@/lib/firebase";

/** POST the ID token to create the httpOnly session cookie (also ensures the user doc). */
async function postSession(user: User, forceRefresh = false): Promise<void> {
  const idToken = await user.getIdToken(forceRefresh);
  const res = await fetch("/api/auth/session", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ idToken }),
  });
  if (!res.ok) throw new Error("session");
}

export async function signInWithEmail(email: string, password: string): Promise<void> {
  const cred = await signInWithEmailAndPassword(auth, email, password);
  await postSession(cred.user);
}

export async function registerWithEmail(name: string, email: string, password: string): Promise<void> {
  const cred = await createUserWithEmailAndPassword(auth, email, password);
  await updateProfile(cred.user, { displayName: name });
  // Force a token refresh so the ID token carries the just-set displayName —
  // otherwise the server provisions the user doc with the email prefix.
  await postSession(cred.user, true);
}

/** Returns `isNewUser` so callers can route first-time users through taste onboarding. */
export async function signInWithGoogle(): Promise<{ isNewUser: boolean }> {
  const cred = await signInWithPopup(auth, new GoogleAuthProvider());
  await postSession(cred.user);
  return { isNewUser: getAdditionalUserInfo(cred)?.isNewUser ?? false };
}

export async function sendReset(email: string): Promise<void> {
  await sendPasswordResetEmail(auth, email);
}
