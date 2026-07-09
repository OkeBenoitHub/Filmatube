// Find (and optionally delete) orphaned R2 objects — files no longer referenced
// by any Firestore movie or user document.
//
// Usage (Node 20+, loads .env.local):
//   node --env-file=.env.local scripts/r2-cleanup.mjs           # dry run (report only)
//   node --env-file=.env.local scripts/r2-cleanup.mjs --delete  # delete orphans
//
// Uses the object-scoped R2 token (list + delete objects) + the Firebase admin SDK.

import {
  S3Client,
  ListObjectsV2Command,
  DeleteObjectsCommand,
} from "@aws-sdk/client-s3";
import { cert, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const DELETE = process.argv.includes("--delete");

initializeApp({
  credential: cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, "\n"),
  }),
});
const db = getFirestore();

const s3 = new S3Client({
  region: "auto",
  endpoint: `https://${process.env.R2_ACCOUNT_ID}.r2.cloudflarestorage.com`,
  credentials: {
    accessKeyId: process.env.R2_ACCESS_KEY_ID,
    secretAccessKey: process.env.R2_SECRET_ACCESS_KEY,
  },
});

const BUCKETS = {
  videos: process.env.R2_BUCKET_VIDEOS,
  images: process.env.R2_BUCKET_IMAGES,
  avatars: process.env.R2_BUCKET_AVATARS,
  subtitles: process.env.R2_BUCKET_SUBTITLES,
};
const BASES = {
  images: process.env.R2_PUBLIC_URL_IMAGES,
  avatars: process.env.R2_PUBLIC_URL_AVATARS,
  subtitles: process.env.R2_PUBLIC_URL_SUBTITLES,
};

/** Extract the object key from a public URL, or null if it isn't in that base. */
function keyOf(url, base) {
  if (!url || !base) return null;
  const b = base.replace(/\/$/, "");
  return url.startsWith(b) ? url.slice(b.length + 1) : null;
}

async function collectReferenced() {
  const referenced = { videos: new Set(), images: new Set(), avatars: new Set(), subtitles: new Set() };

  const movies = await db.collection("movies").get();
  movies.forEach((doc) => {
    const m = doc.data();
    if (m.videoKey) referenced.videos.add(m.videoKey);
    for (const u of [m.posterUrl, m.backdropUrl, m.thumbnailUrl]) {
      const k = keyOf(u, BASES.images);
      if (k) referenced.images.add(k);
    }
    (m.subtitleTracks ?? []).forEach((t) => {
      const k = keyOf(t?.url, BASES.subtitles);
      if (k) referenced.subtitles.add(k);
    });
  });

  const users = await db.collection("users").get();
  users.forEach((doc) => {
    const k = keyOf(doc.data().avatarUrl, BASES.avatars);
    if (k) referenced.avatars.add(k);
  });

  return referenced;
}

async function listKeys(bucket) {
  const keys = [];
  let token;
  do {
    const res = await s3.send(new ListObjectsV2Command({ Bucket: bucket, ContinuationToken: token }));
    (res.Contents ?? []).forEach((o) => keys.push(o.Key));
    token = res.IsTruncated ? res.NextContinuationToken : undefined;
  } while (token);
  return keys;
}

async function deleteKeys(bucket, keys) {
  for (let i = 0; i < keys.length; i += 1000) {
    const batch = keys.slice(i, i + 1000).map((Key) => ({ Key }));
    await s3.send(new DeleteObjectsCommand({ Bucket: bucket, Delete: { Objects: batch } }));
  }
}

const referenced = await collectReferenced();
let totalOrphans = 0;

for (const [name, bucket] of Object.entries(BUCKETS)) {
  if (!bucket) continue;
  console.log(`\n📦 ${bucket}`);
  let keys;
  try {
    keys = await listKeys(bucket);
  } catch (e) {
    console.log(`   ✗ list failed: ${e.name} — ${e.message}`);
    continue;
  }
  const orphans = keys.filter((k) => !referenced[name].has(k));
  console.log(`   ${keys.length} objects, ${referenced[name].size} referenced, ${orphans.length} orphaned`);
  orphans.slice(0, 20).forEach((k) => console.log(`     - ${k}`));
  if (orphans.length > 20) console.log(`     … and ${orphans.length - 20} more`);
  totalOrphans += orphans.length;

  if (DELETE && orphans.length) {
    await deleteKeys(bucket, orphans);
    console.log(`   🗑️  deleted ${orphans.length}`);
  }
}

console.log(
  DELETE
    ? `\n✅ Cleanup done (${totalOrphans} orphan(s) removed).`
    : `\n${totalOrphans} orphan(s) found. Re-run with --delete to remove them.`,
);
process.exit(0);
