import "server-only";

import { getAdminDb } from "@/lib/firebase-admin";

/** Lifecycle states — mirrors Android `PartyStatus`. */
export const PARTY_STATUS = { SCHEDULED: "scheduled", LIVE: "live", ENDED: "ended" } as const;

/** A private watch party. Mirrors the Android `Party` model. */
export interface Party {
  id: string;
  movieId: string;
  movieTitle: string;
  moviePoster: string;
  hostId: string;
  hostName: string;
  status: string;
  scheduledAtMs: number;
  memberCount: number;
  createdAtMs: number;
}

export interface PartyMember {
  uid: string;
  name: string;
  avatar: string;
  role: string;
}

function toMillis(v: unknown): number {
  return (v as { toMillis?: () => number })?.toMillis?.() ?? 0;
}

function mapParty(id: string, x: FirebaseFirestore.DocumentData): Party {
  return {
    id,
    movieId: (x.movieId as string) ?? "",
    movieTitle: (x.movieTitle as string) ?? "",
    moviePoster: (x.moviePoster as string) ?? "",
    hostId: (x.hostId as string) ?? "",
    hostName: (x.hostName as string) ?? "",
    status: (x.status as string) ?? PARTY_STATUS.SCHEDULED,
    scheduledAtMs: toMillis(x.scheduledAt),
    memberCount: (x.memberCount as number) ?? 0,
    createdAtMs: toMillis(x.createdAt),
  };
}

/**
 * A single party. Returns null unless [uid] is the host or a member — parties are private,
 * matching the Firestore read rule.
 */
export async function getParty(id: string, uid?: string): Promise<Party | null> {
  const doc = await getAdminDb().collection("parties").doc(id).get();
  if (!doc.exists) return null;
  const party = mapParty(doc.id, doc.data() ?? {});
  const members = (doc.get("memberIds") as string[]) ?? [];
  if (!uid || (party.hostId !== uid && !members.includes(uid))) return null;
  return party;
}

/** Parties the user hosts or joined that haven't ended, soonest first. */
export async function getMyParties(uid: string, limit = 20): Promise<Party[]> {
  const snap = await getAdminDb()
    .collection("parties")
    .where("memberIds", "array-contains", uid)
    .orderBy("scheduledAt", "asc")
    .limit(limit)
    .get();
  return snap.docs.map((d) => mapParty(d.id, d.data())).filter((p) => p.status !== PARTY_STATUS.ENDED);
}

/** Members joined with their profiles (member docs hold only uid/role, like boards). */
export async function getPartyMembers(partyId: string): Promise<PartyMember[]> {
  const db = getAdminDb();
  const snap = await db.collection("parties").doc(partyId).collection("members").limit(100).get();
  if (snap.empty) return [];

  const profiles = await db.getAll(...snap.docs.map((d) => db.collection("users").doc(d.id)));
  const byId = new Map(profiles.map((p) => [p.id, p]));

  return snap.docs
    .map((d) => {
      const profile = byId.get(d.id);
      return {
        uid: d.id,
        name: (profile?.get("displayName") as string) ?? "",
        avatar: (profile?.get("avatarUrl") as string) ?? "",
        role: (d.get("role") as string) ?? "guest",
      };
    })
    .sort((a, b) => (a.role === b.role ? a.name.localeCompare(b.name) : a.role === "host" ? -1 : 1));
}

export async function isPartyMember(partyId: string, uid: string): Promise<boolean> {
  const doc = await getAdminDb().collection("parties").doc(partyId).collection("members").doc(uid).get();
  return doc.exists;
}
