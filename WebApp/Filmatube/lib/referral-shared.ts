/** Referral constants safe to import from client and server (no admin SDK). */
export const REF_COOKIE = "filmatube.ref";
/** How long a captured invite lasts before a signup no longer attributes to it. */
export const REF_COOKIE_MAX_AGE = 60 * 60 * 24 * 30; // 30 days

/** The invite link a user shares. Code = their uid. */
export function inviteUrl(origin: string, uid: string): string {
  return `${origin}/invite/${uid}`;
}
