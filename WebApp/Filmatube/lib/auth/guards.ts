import "server-only";

import { redirect } from "next/navigation";
import type { DecodedIdToken } from "firebase-admin/auth";
import { getCurrentUser } from "./session";

/** Requires a signed-in user (redirects to /login otherwise). */
export async function requireUser(): Promise<DecodedIdToken> {
  const user = await getCurrentUser();
  if (!user) redirect("/login");
  return user;
}

/**
 * Sends already-signed-in visitors to the app. Use on public auth/marketing entry points
 * (landing, login, register, forgot-password) so hitting Back after signing in doesn't
 * land on the signed-out view.
 */
export async function redirectIfSignedIn(to = "/home"): Promise<void> {
  const user = await getCurrentUser();
  if (user) redirect(to);
}

/** Requires an admin user (redirects to /login or / otherwise). */
export async function requireAdmin(): Promise<DecodedIdToken> {
  const user = await getCurrentUser();
  if (!user) redirect("/login");
  if (user.admin !== true) redirect("/");
  return user;
}
