import { NextResponse, type NextRequest } from "next/server";

/**
 * Coarse, cookie-presence routing at the Edge. Full verification (and the isAdmin check)
 * happens in the server components / route handlers via getCurrentUser — middleware runs on
 * the Edge runtime and can't use the Firebase Admin SDK.
 */
export function middleware(request: NextRequest) {
  const hasSession = Boolean(request.cookies.get("__session")?.value);
  const { pathname } = request.nextUrl;

  // Landing: send signed-in visitors straight to the app. Deciding this here — rather than
  // rendering the page and redirecting from inside it — means pressing Back after signing in
  // never renders the marketing page first, so there's no blank flash on the way to /home.
  //
  // Cookie presence is enough here: if the cookie turns out to be invalid, /home's layout
  // bounces to /login, which this matcher doesn't touch — so no redirect loop. The landing
  // page keeps its own verified guard as a backstop.
  if (pathname === "/") {
    if (!hasSession) return NextResponse.next();
    const url = request.nextUrl.clone();
    url.pathname = "/home";
    return NextResponse.redirect(url);
  }

  // /admin/*: require a session cookie before the page does the real admin-claim check.
  if (!hasSession) {
    const url = request.nextUrl.clone();
    url.pathname = "/login";
    url.searchParams.set("next", pathname);
    return NextResponse.redirect(url);
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/", "/admin/:path*"],
};
