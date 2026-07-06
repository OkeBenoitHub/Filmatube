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

/** Requires an admin user (redirects to /login or / otherwise). */
export async function requireAdmin(): Promise<DecodedIdToken> {
  const user = await getCurrentUser();
  if (!user) redirect("/login");
  if (user.admin !== true) redirect("/");
  return user;
}
