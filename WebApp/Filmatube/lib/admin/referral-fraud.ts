/**
 * Referral fraud classification (Day 206) — pure, so it's unit-tested without touching Firestore.
 *
 * Two signals, both cheap and false-positive-averse:
 *  - self-referral: referrer and referred are the same account (also blocked at write time).
 *  - shared signup IP: ≥2 of one referrer's referred accounts hash to the same signup IP, which
 *    reads as one person farming multiple accounts. A single account on a shared IP is *not*
 *    flagged — households and campus networks are normal.
 */
export interface ReferralSignal {
  referredId: string;
  referrerId: string;
  ipHash: string;
}

/** Reason string per referred id — "" means clean. */
export function classifyReferrals(rows: ReferralSignal[]): Map<string, string> {
  const byReferrer = new Map<string, ReferralSignal[]>();
  for (const r of rows) {
    const list = byReferrer.get(r.referrerId);
    if (list) list.push(r);
    else byReferrer.set(r.referrerId, [r]);
  }

  const out = new Map<string, string>();
  for (const r of rows) {
    if (r.referrerId === r.referredId) {
      out.set(r.referredId, "self-referral");
      continue;
    }
    const siblings = byReferrer.get(r.referrerId) ?? [];
    if (r.ipHash && siblings.filter((s) => s.ipHash === r.ipHash).length >= 2) {
      out.set(r.referredId, "shared signup IP");
      continue;
    }
    out.set(r.referredId, "");
  }
  return out;
}
