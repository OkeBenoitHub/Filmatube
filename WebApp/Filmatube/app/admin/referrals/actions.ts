"use server";

import { revalidatePath } from "next/cache";
import { FieldValue } from "firebase-admin/firestore";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";

async function assertAdmin() {
  const user = await getCurrentUser();
  if (!user || user.admin !== true) throw new Error("Forbidden");
}

/**
 * Revoke a referral judged fraudulent: delete it and roll back the referrer's reward. If it was
 * their only referral, the Recruiter badge and early-access perk come off too.
 */
export async function revokeReferral(referredId: string): Promise<void> {
  await assertAdmin();
  const db = getAdminDb();

  const doc = await db.collection("referrals").doc(referredId).get();
  if (!doc.exists) return;
  const referrerId = doc.get("referrerId") as string;

  await db.collection("referrals").doc(referredId).delete();

  if (referrerId) {
    await db.collection("users").doc(referrerId).set(
      { referralCount: FieldValue.increment(-1) },
      { merge: true },
    );
    // If nothing else references them, strip the Recruiter reward entirely.
    const remaining = await db.collection("referrals").where("referrerId", "==", referrerId).limit(1).get();
    if (remaining.empty) {
      await db.collection("users").doc(referrerId).set({ earlyAccess: false }, { merge: true });
      await db.collection("achievements").doc(referrerId).collection("badges").doc("recruiter").delete();
    }
  }

  revalidatePath("/admin/referrals");
}
