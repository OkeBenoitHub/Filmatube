import { NextResponse, type NextRequest } from "next/server";
import { getRequestUser } from "@/lib/auth/request-user";
import { getAdminDb } from "@/lib/firebase-admin";
import { presignDownload } from "@/lib/r2-presign";

/**
 * GET /api/stream/:id — returns a short-lived presigned playback URL for a movie's
 * private video object. Requires a signed-in user; the URL expires so it can't be
 * shared/hotlinked. The player (Day 50) sets this as the <video> source.
 */
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> },
) {
  const user = await getRequestUser(request);
  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const { id } = await params;
  const snapshot = await getAdminDb().collection("movies").doc(id).get();
  if (!snapshot.exists) {
    return NextResponse.json({ error: "Movie not found" }, { status: 404 });
  }

  const videoKey = snapshot.get("videoKey") as string | undefined;
  if (!videoKey) {
    return NextResponse.json({ error: "No video available" }, { status: 404 });
  }

  const expiresIn = 3600; // 1 hour
  const url = await presignDownload("videos", videoKey, expiresIn);
  return NextResponse.json({ url, expiresIn });
}
