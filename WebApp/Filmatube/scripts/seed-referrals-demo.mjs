// Exercise the referral reward chain end to end against prod, and leave demo data behind so the
// /refer dashboard and /admin/referrals page have something to show.
//
// It creates a few "referred friend" accounts, writes referrals/{} exactly as recordReferral does
// (referrerId, referredId, status, ipHash) — which fires the deployed onReferralCreated trigger:
// referralCount++, earlyAccess, the Recruiter badge, and an inbox+FCM notification. Two of them
// share an ipHash, so the admin fraud guard flags that referrer's cluster.
//
// Usage (from WebApp/Filmatube):  node --env-file=.env.local scripts/seed-referrals-demo.mjs

import { cert, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";

initializeApp({
  credential: cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, "\n"),
  }),
});
const db = getFirestore();

const REFERRER = "G13siOkqyFMaZNGYuq2zQqYa5c42"; // BENOIT PRESLY NDONG OKE

// Fixed ids so re-runs don't pile up. Two share a signup-IP hash → flagged as multi-account.
const friends = [
  { id: "demo-ref-amara", name: "Amara Okafor", ipHash: "shareddemohash01" },
  { id: "demo-ref-tunde", name: "Tunde Bello", ipHash: "shareddemohash01" },
  { id: "demo-ref-chidi", name: "Chidi Nwosu", ipHash: "uniquedemohash02" },
];

const before = await db.collection("users").doc(REFERRER).get();
console.log(`Referrer before: referralCount=${before.get("referralCount") ?? 0}, earlyAccess=${before.get("earlyAccess") ?? false}`);

for (const f of friends) {
  await db.collection("users").doc(f.id).set(
    {
      displayName: f.name,
      bio: "Demo referred account.",
      avatarUrl: "",
      followersCount: 0,
      followingCount: 0,
      genrePreferences: [],
      isAdmin: false,
      isBanned: false,
      createdAt: FieldValue.serverTimestamp(),
      lastActiveAt: FieldValue.serverTimestamp(),
    },
    { merge: true },
  );
  await db.collection("referrals").doc(f.id).set({
    referrerId: REFERRER,
    referredId: f.id,
    status: "completed",
    ipHash: f.ipHash,
    createdAt: FieldValue.serverTimestamp(),
  });
  console.log(`✅ referral: ${f.name} (ipHash ${f.ipHash})`);
}

console.log("\n⏳ waiting 12s for onReferralCreated to run…");
await new Promise((r) => setTimeout(r, 12000));

const after = await db.collection("users").doc(REFERRER).get();
const badge = await db.collection("achievements").doc(REFERRER).collection("badges").doc("recruiter").get();
const notifs = await db
  .collection("users").doc(REFERRER).collection("notifications")
  .where("type", "==", "referral").get();

console.log("\n── Reward chain result ──");
console.log(`referralCount : ${after.get("referralCount") ?? 0}`);
console.log(`earlyAccess   : ${after.get("earlyAccess") ?? false}`);
console.log(`recruiter badge: ${badge.exists ? "granted ✅" : "MISSING ❌"}`);
console.log(`referral notifs: ${notifs.size}`);

// Show how the admin fraud guard will classify them.
const shared = friends.filter((f) => friends.filter((g) => g.ipHash === f.ipHash).length >= 2);
console.log(`\nAdmin will flag ${shared.length} referral(s) as "shared signup IP": ${shared.map((f) => f.name).join(", ")}`);
console.log("\nDone. See /refer (as the referrer) and /admin/referrals.");
process.exit(0);
