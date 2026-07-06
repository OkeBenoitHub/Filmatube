// Grant a user the `admin` custom claim + set users/{uid}.isAdmin = true.
//
// Usage (Node 20+, loads .env.local for the service account):
//   node --env-file=.env.local scripts/set-admin.mjs you@example.com
//
// The user must sign out and back in afterwards so their session token includes the claim.

import { cert, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";

const email = process.argv[2];
if (!email) {
  console.error("Usage: node --env-file=.env.local scripts/set-admin.mjs <email>");
  process.exit(1);
}

initializeApp({
  credential: cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, "\n"),
  }),
});

const user = await getAuth().getUserByEmail(email);
await getAuth().setCustomUserClaims(user.uid, { admin: true });
await getFirestore().collection("users").doc(user.uid).set({ isAdmin: true }, { merge: true });

console.log(`✅ ${email} (uid ${user.uid}) is now admin. Sign out and back in to refresh the token.`);
process.exit(0);
