// Apply CORS + lifecycle rules to all Filmatube R2 buckets over the S3 API.
// Also verifies the R2 credentials/buckets are reachable.
//
// Usage (Node 20+, loads .env.local for the R2 keys):
//   node --env-file=.env.local scripts/apply-r2-config.mjs
//
// Reads infra/r2/cors.json and infra/r2/lifecycle.json (repo root).

import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, resolve } from "node:path";
import {
  S3Client,
  HeadBucketCommand,
  PutBucketCorsCommand,
  PutBucketLifecycleConfigurationCommand,
} from "@aws-sdk/client-s3";

const __dirname = dirname(fileURLToPath(import.meta.url));
const infra = resolve(__dirname, "../../../infra/r2");
const corsRules = JSON.parse(readFileSync(resolve(infra, "cors.json"), "utf8"));
const lifecycle = JSON.parse(readFileSync(resolve(infra, "lifecycle.json"), "utf8"));

const accountId = process.env.R2_ACCOUNT_ID;
if (!accountId || !process.env.R2_ACCESS_KEY_ID || !process.env.R2_SECRET_ACCESS_KEY) {
  console.error("❌ Missing R2_ACCOUNT_ID / R2_ACCESS_KEY_ID / R2_SECRET_ACCESS_KEY in .env.local");
  process.exit(1);
}

const buckets = [
  process.env.R2_BUCKET_VIDEOS,
  process.env.R2_BUCKET_IMAGES,
  process.env.R2_BUCKET_AVATARS,
  process.env.R2_BUCKET_SUBTITLES,
].filter(Boolean);

const s3 = new S3Client({
  region: "auto",
  endpoint: `https://${accountId}.r2.cloudflarestorage.com`,
  credentials: {
    accessKeyId: process.env.R2_ACCESS_KEY_ID,
    secretAccessKey: process.env.R2_SECRET_ACCESS_KEY,
  },
});

let failures = 0;

for (const Bucket of buckets) {
  console.log(`\n📦 ${Bucket}`);

  try {
    await s3.send(new HeadBucketCommand({ Bucket }));
    console.log("   ✓ reachable");
  } catch (e) {
    console.log(`   ✗ not reachable: ${e.name} — ${e.message}`);
    failures++;
    continue;
  }

  try {
    await s3.send(new PutBucketCorsCommand({ Bucket, CORSConfiguration: { CORSRules: corsRules } }));
    console.log("   ✓ CORS applied");
  } catch (e) {
    console.log(`   ✗ CORS failed: ${e.name} — ${e.message}`);
    failures++;
  }

  try {
    await s3.send(
      new PutBucketLifecycleConfigurationCommand({ Bucket, LifecycleConfiguration: lifecycle }),
    );
    console.log("   ✓ lifecycle applied");
  } catch (e) {
    console.log(`   ✗ lifecycle failed: ${e.name} — ${e.message}`);
    failures++;
  }
}

console.log(failures === 0 ? "\n✅ All buckets configured." : `\n⚠️  ${failures} issue(s) above.`);
process.exit(failures === 0 ? 0 : 1);
