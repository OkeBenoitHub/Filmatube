import { NextResponse, type NextRequest } from "next/server";
import { getRequestUser } from "@/lib/auth/request-user";
import { recordReferral } from "@/lib/referrals";

/**
 * Attribute a referral for the authenticated caller (bearer id-token) — the path the Android
 * app uses, since it doesn't go through the web session route. Idempotent and guarded server-
 * side: `recordReferral` ignores self-referral, unknown referrers, and anyone already referred,
 * so calling it can neither double-count nor overwrite an existing attribution.
 */
export async function POST(request: NextRequest) {
  const user = await getRequestUser(request);
  if (!user) return NextResponse.json({ error: "Unauthorized" }, { status: 401 });

  const { code } = (await request.json().catch(() => ({}))) as { code?: string };
  if (!code) return NextResponse.json({ error: "Missing code" }, { status: 400 });

  const recorded = await recordReferral(code, user.uid);
  return NextResponse.json({ recorded });
}
