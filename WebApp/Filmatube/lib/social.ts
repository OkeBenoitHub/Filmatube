import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";
import { getUserProfile } from "@/lib/user";

/** A user as shown in follower/following lists and suggestion cards. */
export interface FollowUserCard {
  uid: string;
  displayName: string;
  avatarUrl: string;
  bio: string;
}

/** Ids of everyone following `uid` — mirrors Android FollowRepository.observeFollowerIds. */
export async function getFollowerIds(uid: string): Promise<string[]> {
  const snap = await getAdminDb().collection("follows").where("followedId", "==", uid).get();
  return snap.docs.map((d) => d.get("followerId") as string).filter(Boolean);
}

/** Ids `uid` follows — mirrors Android FollowRepository.observeFollowingIds. */
export async function getFollowingIds(uid: string): Promise<string[]> {
  const snap = await getAdminDb().collection("follows").where("followerId", "==", uid).get();
  return snap.docs.map((d) => d.get("followedId") as string).filter(Boolean);
}

/** Resolves user ids into display cards, dropping any that no longer exist. */
export async function getFollowUsers(ids: string[]): Promise<FollowUserCard[]> {
  const profiles = await Promise.all(ids.map((id) => getUserProfile(id)));
  return profiles
    .filter((p): p is NonNullable<typeof p> => p !== null)
    .map((p) => ({ uid: p.uid, displayName: p.displayName, avatarUrl: p.avatarUrl, bio: p.bio }));
}

/** A suggested user to follow, ranked by taste overlap. */
export interface SuggestedUser extends FollowUserCard {
  tasteMatch: number;
}

/**
 * "People with your taste" — scans users, ranks by genre overlap with `myUid`,
 * excludes self + already-followed + banned. Mirrors Android SocialRepository.suggestedUsers.
 */
export async function getSuggestedUsers(myUid: string, max = 20): Promise<SuggestedUser[]> {
  const [mine, followingIds] = await Promise.all([getUserProfile(myUid), getFollowingIds(myUid)]);
  const exclude = new Set<string>([myUid, ...followingIds]);
  const myGenres = mine?.genrePreferences ?? [];

  const snap = await getAdminDb().collection("users").limit(60).get();
  return snap.docs
    .filter((d) => !exclude.has(d.id) && d.get("isBanned") !== true && (d.get("displayName") ?? "") !== "")
    .map((d) => ({
      uid: d.id,
      displayName: (d.get("displayName") as string) ?? "",
      avatarUrl: (d.get("avatarUrl") as string) ?? "",
      bio: (d.get("bio") as string) ?? "",
      tasteMatch: tasteMatch(myGenres, (d.get("genrePreferences") as string[]) ?? []),
    }))
    .sort((a, b) => b.tasteMatch - a.tasteMatch)
    .slice(0, max);
}

/** Jaccard similarity of two genre-preference sets as a 0–100 percentage. */
export function tasteMatch(mine: string[], theirs: string[]): number {
  if (mine.length === 0 || theirs.length === 0) return 0;
  const a = new Set(mine);
  const b = new Set(theirs);
  let intersection = 0;
  for (const g of a) if (b.has(g)) intersection += 1;
  const union = new Set([...a, ...b]).size;
  return union === 0 ? 0 : Math.round((intersection / union) * 100);
}
