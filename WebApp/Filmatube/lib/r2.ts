import "server-only";

import { S3Client } from "@aws-sdk/client-s3";

/**
 * Cloudflare R2 (S3-compatible) client + bucket registry.
 *
 * R2 stores all binaries (video, images, avatars, subtitles); Firestore only holds URLs/keys.
 * The client is created lazily so the build doesn't require credentials.
 */
export const R2_BUCKETS = {
  videos: process.env.R2_BUCKET_VIDEOS ?? "filmatube-videos",
  images: process.env.R2_BUCKET_IMAGES ?? "filmatube-images",
  avatars: process.env.R2_BUCKET_AVATARS ?? "filmatube-avatars",
  subtitles: process.env.R2_BUCKET_SUBTITLES ?? "filmatube-subtitles",
} as const;

export type R2Bucket = keyof typeof R2_BUCKETS;

let client: S3Client | undefined;

export function getR2Client(): S3Client {
  if (client) return client;
  const accountId = process.env.R2_ACCOUNT_ID;
  client = new S3Client({
    region: "auto",
    endpoint: `https://${accountId}.r2.cloudflarestorage.com`,
    credentials: {
      accessKeyId: process.env.R2_ACCESS_KEY_ID ?? "",
      secretAccessKey: process.env.R2_SECRET_ACCESS_KEY ?? "",
    },
  });
  return client;
}

/** Public CDN URL for a stored object (R2 public bucket or custom domain). */
export function r2PublicUrl(key: string): string {
  const base = (process.env.R2_PUBLIC_BASE_URL ?? "").replace(/\/$/, "");
  return `${base}/${key}`;
}
