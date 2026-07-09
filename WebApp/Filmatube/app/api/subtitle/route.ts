import { NextRequest, NextResponse } from "next/server";
import { getCurrentUser } from "@/lib/auth/session";

/**
 * Same-origin proxy for a movie's .vtt subtitle so the browser <track> element
 * loads without cross-origin/CORS issues. Restricted to the public R2 subtitles
 * base to avoid SSRF, and gated to signed-in users.
 */
export async function GET(req: NextRequest) {
  const user = await getCurrentUser();
  if (!user) return new NextResponse("Unauthorized", { status: 401 });

  const url = req.nextUrl.searchParams.get("url");
  const base = process.env.R2_PUBLIC_URL_SUBTITLES;
  if (!url || !base || !url.startsWith(base)) {
    return new NextResponse("Bad request", { status: 400 });
  }

  const upstream = await fetch(url);
  if (!upstream.ok) return new NextResponse("Not found", { status: 404 });

  const body = await upstream.text();
  return new NextResponse(body, {
    headers: {
      "Content-Type": "text/vtt; charset=utf-8",
      "Cache-Control": "public, max-age=3600",
    },
  });
}
