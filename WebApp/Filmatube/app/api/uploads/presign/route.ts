import { randomUUID } from "node:crypto";
import { NextResponse, type NextRequest } from "next/server";
import { getRequestUser } from "@/lib/auth/request-user";
import { allowRequest } from "@/lib/rate-limit";
import { R2_BUCKETS, r2PublicUrl, type R2Bucket } from "@/lib/r2";
import { presignUpload } from "@/lib/r2-presign";

// videos/images/subtitles are catalog content → admin only.
// avatars are user-uploaded → any signed-in user (scoped to their uid).
const ADMIN_ONLY_BUCKETS: R2Bucket[] = ["videos", "images", "subtitles"];

function sanitize(name: string): string {
  return name.replace(/[^a-zA-Z0-9._-]/g, "_").slice(-100);
}

interface PresignBody {
  bucket?: string;
  filename?: string;
  contentType?: string;
  prefix?: string;
}

export async function POST(request: NextRequest) {
  const user = await getRequestUser(request);
  if (!user) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  // Throttle presign requests per user (30/min) to limit upload-URL minting abuse.
  if (!(await allowRequest("presign", user.uid, 30, 60_000))) {
    return NextResponse.json({ error: "Too many requests" }, { status: 429 });
  }

  const { bucket, filename, contentType, prefix } = (await request
    .json()
    .catch(() => ({}))) as PresignBody;

  if (!bucket || !filename || !contentType) {
    return NextResponse.json(
      { error: "bucket, filename and contentType are required" },
      { status: 400 },
    );
  }
  if (!(bucket in R2_BUCKETS)) {
    return NextResponse.json({ error: "Invalid bucket" }, { status: 400 });
  }

  const b = bucket as R2Bucket;
  const isAdmin = user.admin === true;
  if (ADMIN_ONLY_BUCKETS.includes(b) && !isAdmin) {
    return NextResponse.json({ error: "Forbidden" }, { status: 403 });
  }

  const safeName = sanitize(filename);
  const key =
    b === "avatars"
      ? `avatars/${user.uid}/${randomUUID()}-${safeName}`
      : `${prefix ? `${sanitize(prefix)}/` : ""}${randomUUID()}-${safeName}`;

  const uploadUrl = await presignUpload(b, key, contentType);

  return NextResponse.json({
    uploadUrl,
    key,
    bucket: b,
    publicUrl: r2PublicUrl(b, key), // null for the private `videos` bucket
  });
}
