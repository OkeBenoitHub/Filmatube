import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";
import { classifyReferrals } from "@/lib/admin/referral-fraud";

export interface ReferralRow {
  referredId: string;
  referredName: string;
  referrerId: string;
  referrerName: string;
  createdAtMs: number;
  suspicious: boolean;
  reason: string;
}

export interface ReferralAnalytics {
  total: number;
  last7: number;
  last30: number;
  referrerCount: number;
  suspiciousCount: number;
  topReferrers: { uid: string; name: string; count: number; suspicious: number }[];
  recent: ReferralRow[];
}

interface Raw {
  referredId: string;
  referrerId: string;
  ipHash: string;
  createdAtMs: number;
}

/** Referral analytics + fraud flags for the admin dashboard. */
export async function getReferralAnalytics(): Promise<ReferralAnalytics> {
  const db = getAdminDb();
  const snap = await db.collection("referrals").limit(1000).get();
  const raw: Raw[] = snap.docs.map((d) => ({
    referredId: (d.get("referredId") as string) ?? d.id,
    referrerId: (d.get("referrerId") as string) ?? "",
    ipHash: (d.get("ipHash") as string) ?? "",
    createdAtMs: d.get("createdAt")?.toMillis?.() ?? 0,
  }));

  const byReferrer = new Map<string, Raw[]>();
  for (const r of raw) {
    if (!byReferrer.has(r.referrerId)) byReferrer.set(r.referrerId, []);
    byReferrer.get(r.referrerId)!.push(r);
  }
  const reasons = classifyReferrals(raw);
  const reasonOf = (r: Raw): string => reasons.get(r.referredId) ?? "";

  // Resolve display names for everyone involved in one batched read.
  const uids = [...new Set(raw.flatMap((r) => [r.referrerId, r.referredId]).filter(Boolean))];
  const names = new Map<string, string>();
  if (uids.length > 0) {
    const refs = uids.map((u) => db.collection("users").doc(u));
    const docs = await db.getAll(...refs);
    docs.forEach((d) => names.set(d.id, (d.get("displayName") as string) || d.id));
  }

  const now = Date.now();
  const rows: ReferralRow[] = raw
    .map((r) => {
      const reason = reasonOf(r);
      return {
        referredId: r.referredId,
        referredName: names.get(r.referredId) ?? r.referredId,
        referrerId: r.referrerId,
        referrerName: names.get(r.referrerId) ?? r.referrerId,
        createdAtMs: r.createdAtMs,
        suspicious: reason !== "",
        reason,
      };
    })
    .sort((a, b) => b.createdAtMs - a.createdAtMs);

  const topReferrers = [...byReferrer.entries()]
    .map(([uid, list]) => ({
      uid,
      name: names.get(uid) ?? uid,
      count: list.length,
      suspicious: list.filter((r) => reasonOf(r) !== "").length,
    }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 10);

  return {
    total: raw.length,
    last7: raw.filter((r) => now - r.createdAtMs <= 7 * 24 * 3600e3).length,
    last30: raw.filter((r) => now - r.createdAtMs <= 30 * 24 * 3600e3).length,
    referrerCount: byReferrer.size,
    suspiciousCount: rows.filter((r) => r.suspicious).length,
    topReferrers,
    recent: rows.slice(0, 40),
  };
}
