import { NextResponse, type NextRequest } from "next/server";

/**
 * Coarse route protection: redirects to /login when the `__session` cookie is absent
 * on protected paths. Full verification (and the isAdmin check) happens in the server
 * components / route handlers via getCurrentUser — middleware runs on the Edge runtime
 * and can't use the Firebase Admin SDK.
 */
export function middleware(request: NextRequest) {
  const hasSession = Boolean(request.cookies.get("__session")?.value);
  if (!hasSession) {
    const url = request.nextUrl.clone();
    url.pathname = "/login";
    url.searchParams.set("next", request.nextUrl.pathname);
    return NextResponse.redirect(url);
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/admin/:path*"],
};
