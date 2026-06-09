import "server-only";

import { GetObjectCommand, PutObjectCommand } from "@aws-sdk/client-s3";
import { getSignedUrl } from "@aws-sdk/s3-request-presigner";
import { getR2Client, R2_BUCKETS, type R2Bucket } from "@/lib/r2";

/** Presigned PUT URL for direct browser → R2 upload (default 10 min). */
export function presignUpload(
  bucket: R2Bucket,
  key: string,
  contentType: string,
  expiresIn = 600,
): Promise<string> {
  const command = new PutObjectCommand({
    Bucket: R2_BUCKETS[bucket],
    Key: key,
    ContentType: contentType,
  });
  return getSignedUrl(getR2Client(), command, { expiresIn });
}

/** Short-lived presigned GET URL for private objects, e.g. video playback (default 1 h). */
export function presignDownload(
  bucket: R2Bucket,
  key: string,
  expiresIn = 3600,
): Promise<string> {
  const command = new GetObjectCommand({
    Bucket: R2_BUCKETS[bucket],
    Key: key,
  });
  return getSignedUrl(getR2Client(), command, { expiresIn });
}
