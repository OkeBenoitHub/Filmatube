import "server-only";

import type { NextRequest } from "next/server";
import type { DecodedIdToken } from "firebase-admin/auth";
import { getAdminAuth } from "@/lib/firebase-admin";
import { getCurrentUser } from "./session";

/**
 * Resolves the caller from either:
 *  - an `Authorization: Bearer <Firebase ID token>` header (native apps), or
 *  - the `__session` cookie (web).
 * Returns the decoded token (uid, claims) or null.
 */
export async function getRequestUser(request: NextRequest): Promise<DecodedIdToken | null> {
  const authorization = request.headers.get("authorization");
  if (authorization?.startsWith("Bearer ")) {
    const idToken = authorization.slice("Bearer ".length).trim();
    try {
      return await getAdminAuth().verifyIdToken(idToken);
    } catch {
      // fall through to cookie
    }
  }
  return getCurrentUser();
}
