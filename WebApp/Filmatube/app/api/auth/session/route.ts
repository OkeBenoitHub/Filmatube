import { NextResponse, type NextRequest } from "next/server";
import { clearSession, createSession } from "@/lib/auth/session";

/** POST { idToken } -> sets the session cookie. Called after client-side sign-in (Day 22). */
export async function POST(request: NextRequest) {
  const { idToken } = (await request.json().catch(() => ({}))) as { idToken?: string };
  if (!idToken) {
    return NextResponse.json({ error: "Missing idToken" }, { status: 400 });
  }
  try {
    await createSession(idToken);
    return NextResponse.json({ status: "ok" });
  } catch {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }
}

/** DELETE -> clears the session cookie (logout). */
export async function DELETE() {
  await clearSession();
  return NextResponse.json({ status: "ok" });
}
