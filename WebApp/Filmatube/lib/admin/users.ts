import "server-only";

import { getAdminAuth, getAdminDb } from "@/lib/firebase-admin";

export interface AdminUserRow {
  uid: string;
  displayName: string;
  avatarUrl: string;
  isAdmin: boolean;
  /** Authoritative: read from the custom claim, not the users doc. */
  isBanned: boolean;
  /** Set when the doc flag disagrees with the claim (e.g. a ban that never took). */
  flagMismatch: boolean;
  createdAtMs: number;
}

/**
 * Users for the admin console, newest first. The ban state comes from each account's custom
 * claims — the users-doc `isBanned` is only a mirror, so we surface any disagreement rather
 * than trusting it.
 */
export async function getAdminUsers(limit = 100): Promise<AdminUserRow[]> {
  const snap = await getAdminDb().collection("users").limit(limit).get();
  const auth = getAdminAuth();

  const rows = await Promise.all(
    snap.docs.map(async (d) => {
      const claims = await auth
        .getUser(d.id)
        .then((u) => u.customClaims ?? {})
        .catch(() => ({} as Record<string, unknown>));
      const bannedClaim = claims.banned === true;
      const docFlag = d.get("isBanned") === true;
      return {
        uid: d.id,
        displayName: (d.get("displayName") as string) ?? "",
        avatarUrl: (d.get("avatarUrl") as string) ?? "",
        isAdmin: claims.admin === true,
        isBanned: bannedClaim,
        flagMismatch: bannedClaim !== docFlag,
        createdAtMs: (d.get("createdAt") as { toMillis?: () => number })?.toMillis?.() ?? 0,
      };
    }),
  );

  return rows.sort((a, b) => b.createdAtMs - a.createdAtMs);
}
