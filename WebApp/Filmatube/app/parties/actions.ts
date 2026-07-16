"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { getCurrentUser } from "@/lib/auth/session";
import { getAdminDb } from "@/lib/firebase-admin";
import { getMovie, localized } from "@/lib/movies";
import { getLocale } from "@/lib/i18n/server";
import { PARTY_STATUS } from "@/lib/parties";

async function requireUser() {
  const user = await getCurrentUser();
  if (!user) throw new Error("Unauthorized");
  return user;
}

/** Host-only guard — mirrors the isPartyHost() rule. */
async function assertHost(partyId: string, uid: string) {
  const doc = await getAdminDb().collection("parties").doc(partyId).get();
  if (!doc.exists || doc.get("hostId") !== uid) throw new Error("Forbidden");
  return doc;
}

/** Offsets offered by the create form (minutes from now). */
const START_OFFSETS = [0, 30, 60, 120];

/**
 * Create a party for a movie and land in its lobby — same write shape as Android
 * `PartyRepository.createParty`, so both clients share the room.
 */
export async function createParty(movieId: string, startInMinutes: number): Promise<never> {
  const user = await requireUser();
  const offset = START_OFFSETS.includes(startInMinutes) ? startInMinutes : 0;

  const [movie, locale] = await Promise.all([getMovie(movieId), getLocale()]);
  if (!movie) throw new Error("Movie not found");

  const db = getAdminDb();
  const profile = await db.collection("users").doc(user.uid).get();
  const ref = db.collection("parties").doc();

  await ref.set({
    movieId,
    movieTitle: localized(movie.title, locale),
    moviePoster: movie.posterUrl,
    hostId: user.uid,
    hostName: (profile.get("displayName") as string) ?? "",
    status: PARTY_STATUS.SCHEDULED,
    scheduledAt: Timestamp.fromMillis(Date.now() + offset * 60_000),
    memberIds: [user.uid],
    memberCount: 1,
    createdAt: FieldValue.serverTimestamp(),
  });
  await ref.collection("members").doc(user.uid).set({
    userId: user.uid,
    role: "host",
    joinedAt: FieldValue.serverTimestamp(),
  });

  revalidatePath("/parties");
  redirect(`/parties/${ref.id}`);
}

/** Host: open the room and reset the sync state to the start, paused. */
export async function startParty(partyId: string): Promise<void> {
  const user = await requireUser();
  await assertHost(partyId, user.uid);
  const db = getAdminDb();
  await db.collection("parties").doc(partyId).update({ status: PARTY_STATUS.LIVE });
  await db.collection("parties").doc(partyId).collection("sync").doc("state").set({
    positionMs: 0,
    isPlaying: false,
    updatedAt: FieldValue.serverTimestamp(),
  });
  revalidatePath(`/parties/${partyId}`);
}

export async function endParty(partyId: string): Promise<void> {
  const user = await requireUser();
  await assertHost(partyId, user.uid);
  await getAdminDb().collection("parties").doc(partyId).update({ status: PARTY_STATUS.ENDED });
  revalidatePath(`/parties/${partyId}`);
}

/**
 * Host: hand the room to a guest and step down. Explicit, never automatic — see
 * PartyRepository.transferHost for why a member can't self-claim the host seat.
 */
export async function transferHost(partyId: string, newHostId: string): Promise<void> {
  const user = await requireUser();
  await assertHost(partyId, user.uid);
  if (newHostId === user.uid) return;

  const db = getAdminDb();
  const party = db.collection("parties").doc(partyId);
  const newHost = await db.collection("users").doc(newHostId).get();

  const batch = db.batch();
  batch.update(party, { hostId: newHostId, hostName: (newHost.get("displayName") as string) ?? "" });
  batch.set(party.collection("members").doc(newHostId), { role: "host" }, { merge: true });
  batch.set(party.collection("members").doc(user.uid), { role: "guest" }, { merge: true });
  await batch.commit();

  revalidatePath(`/parties/${partyId}`);
}

export async function joinParty(partyId: string): Promise<void> {
  const user = await requireUser();
  const db = getAdminDb();
  const party = db.collection("parties").doc(partyId);

  const batch = db.batch();
  batch.update(party, { memberIds: FieldValue.arrayUnion(user.uid), memberCount: FieldValue.increment(1) });
  batch.set(party.collection("members").doc(user.uid), {
    userId: user.uid,
    role: "guest",
    joinedAt: FieldValue.serverTimestamp(),
  });
  await batch.commit();

  revalidatePath(`/parties/${partyId}`);
}

/** Guests only — a host ends the party instead of leaving it. */
export async function leaveParty(partyId: string): Promise<void> {
  const user = await requireUser();
  const db = getAdminDb();
  const party = db.collection("parties").doc(partyId);
  if ((await party.get()).get("hostId") === user.uid) throw new Error("Host cannot leave");

  const batch = db.batch();
  batch.update(party, { memberIds: FieldValue.arrayRemove(user.uid), memberCount: FieldValue.increment(-1) });
  batch.delete(party.collection("members").doc(user.uid));
  await batch.commit();

  revalidatePath(`/parties/${partyId}`);
}

async function writeInvites(partyId: string, toUids: string[], actor: { uid: string; name: string; avatar: string }) {
  const db = getAdminDb();
  const party = await db.collection("parties").doc(partyId).get();
  const existing = ((party.get("memberIds") as string[]) ?? []).concat(actor.uid);
  const targets = toUids.filter((id) => !existing.includes(id));

  await Promise.all(
    targets.map((toUid) =>
      db.collection("users").doc(toUid).collection("notifications").add({
        type: "party_invite",
        actorId: actor.uid,
        actorName: actor.name,
        actorAvatar: actor.avatar,
        partyId,
        movieId: party.get("movieId") ?? "",
        movieTitle: party.get("movieTitle") ?? "",
        read: false,
        createdAt: FieldValue.serverTimestamp(),
      }),
    ),
  );
  return targets.length;
}

async function actorOf(uid: string) {
  const profile = await getAdminDb().collection("users").doc(uid).get();
  return {
    uid,
    name: (profile.get("displayName") as string) ?? "",
    avatar: (profile.get("avatarUrl") as string) ?? "",
  };
}

/** Invite everyone who follows me. Returns how many invites were written. */
export async function inviteFollowers(partyId: string): Promise<number> {
  const user = await requireUser();
  await assertHost(partyId, user.uid);
  const snap = await getAdminDb().collection("follows").where("followedId", "==", user.uid).get();
  const followerIds = snap.docs.map((d) => d.get("followerId") as string).filter(Boolean);
  const count = await writeInvites(partyId, followerIds, await actorOf(user.uid));
  revalidatePath(`/parties/${partyId}`);
  return count;
}

/** Invite every member of one of my boards. */
export async function inviteBoard(partyId: string, boardId: string): Promise<number> {
  const user = await requireUser();
  await assertHost(partyId, user.uid);
  const board = await getAdminDb().collection("boards").doc(boardId).get();
  const memberIds = ((board.get("memberIds") as string[]) ?? []).filter(Boolean);
  const count = await writeInvites(partyId, memberIds, await actorOf(user.uid));
  revalidatePath(`/parties/${partyId}`);
  return count;
}
