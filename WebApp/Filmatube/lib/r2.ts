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

// Each R2 bucket has its own public base URL (pub-*.r2.dev or a custom domain).
// `videos` is private (served via /api/stream), so it has no public URL.
const R2_PUBLIC_BASES: Partial<Record<R2Bucket, string | undefined>> = {
  images: process.env.R2_PUBLIC_URL_IMAGES,
  avatars: process.env.R2_PUBLIC_URL_AVATARS,
  subtitles: process.env.R2_PUBLIC_URL_SUBTITLES,
};

/** Public CDN URL for an object in a public bucket, or null if the bucket is private/unset. */
export function r2PublicUrl(bucket: R2Bucket, key: string): string | null {
  const base = R2_PUBLIC_BASES[bucket];
  if (!base) return null;
  return `${base.replace(/\/$/, "")}/${key}`;
}
