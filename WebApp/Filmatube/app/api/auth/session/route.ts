import { NextResponse, type NextRequest } from "next/server";
import { clearSession, createSession } from "@/lib/auth/session";
import { getAdminAuth } from "@/lib/firebase-admin";
import { ensureUserDocument } from "@/lib/user";
import { recordReferral } from "@/lib/referrals";
import { REF_COOKIE } from "@/lib/referral-shared";

/** POST { idToken } -> sets the session cookie and provisions the user doc. */
export async function POST(request: NextRequest) {
  const { idToken } = (await request.json().catch(() => ({}))) as { idToken?: string };
  if (!idToken) {
    return NextResponse.json({ error: "Missing idToken" }, { status: 400 });
  }
  try {
    await createSession(idToken);
    const decoded = await getAdminAuth().verifyIdToken(idToken);
    const { isNew } = await ensureUserDocument(decoded);

    const response = NextResponse.json({ status: "ok" });
    // Attribute a captured invite to this account, but only on first sign-up. The cookie is
    // cleared either way so a later sign-in on the same device can't re-trigger attribution.
    const refCode = request.cookies.get(REF_COOKIE)?.value;
    if (refCode) {
      if (isNew) await recordReferral(refCode, decoded.uid);
      response.cookies.set(REF_COOKIE, "", { maxAge: 0, path: "/" });
    }
    return response;
  } catch {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
}

/** DELETE -> clears the session cookie (logout). */
export async function DELETE() {
  await clearSession();
  return NextResponse.json({ status: "ok" });
}
