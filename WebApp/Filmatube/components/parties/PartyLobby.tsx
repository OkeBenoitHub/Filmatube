"use client";

import { useEffect, useState, useTransition } from "react";
import Link from "next/link";
import { collection, doc, getDocs, limit, onSnapshot, query, where } from "firebase/firestore";
import { Crown, Play, UserPlus, Users } from "lucide-react";
import { db } from "@/lib/firebase";
import {
  endParty,
  inviteBoard,
  inviteFollowers,
  joinParty,
  leaveParty,
  startParty,
  transferHost,
} from "@/app/parties/actions";
import { Modal } from "@/components/ui/Modal";
import { UserAvatar } from "@/components/social/UserList";
import { useAuth } from "@/components/providers/AuthProvider";
import type { Party, PartyMember } from "@/lib/parties";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

/**
 * Watch-party lobby. Server-rendered once, then live: the party doc and member list stream in,
 * so a guest sees the host hit Start (and a handoff) without reloading.
 */
export function PartyLobby({
  initialParty,
  initialMembers,
  initialIsMember,
  dict,
}: {
  initialParty: Party;
  initialMembers: PartyMember[];
  initialIsMember: boolean;
  dict: Dictionary["catalog"];
}) {
  const { user } = useAuth();
  const [party, setParty] = useState(initialParty);
  const [members, setMembers] = useState(initialMembers);
  const [isMember, setIsMember] = useState(initialIsMember);
  const [boards, setBoards] = useState<{ id: string; title: string }[] | null>(null);
  const [picker, setPicker] = useState(false);
  const [invited, setInvited] = useState<number | null>(null);
  const [pending, startTransition] = useTransition();

  const isHost = !!user && party.hostId === user.uid;
  const isLive = party.status === "live";
  const isEnded = party.status === "ended";

  // Live party doc — status, host and member count all change under the guests' feet.
  useEffect(() => {
    return onSnapshot(doc(db, "parties", party.id), (snap) => {
      if (!snap.exists()) return;
      setParty((p) => ({
        ...p,
        hostId: (snap.get("hostId") as string) ?? p.hostId,
        hostName: (snap.get("hostName") as string) ?? p.hostName,
        status: (snap.get("status") as string) ?? p.status,
        memberCount: (snap.get("memberCount") as number) ?? p.memberCount,
      }));
    });
  }, [party.id]);

  // Live roles/membership. Names come from the server render; new joiners show as "guest"
  // until the next full load, which is enough for a lobby.
  useEffect(() => {
    return onSnapshot(collection(db, "parties", party.id, "members"), (snap) => {
      const roles = new Map(snap.docs.map((d) => [d.id, (d.get("role") as string) ?? "guest"]));
      setMembers((prev) => {
        const known = prev.filter((m) => roles.has(m.uid)).map((m) => ({ ...m, role: roles.get(m.uid)! }));
        const extra = [...roles.keys()]
          .filter((uid) => !prev.some((m) => m.uid === uid))
          .map((uid) => ({ uid, name: "", avatar: "", role: roles.get(uid)! }));
        return [...known, ...extra].sort((a, b) => (a.role === b.role ? 0 : a.role === "host" ? -1 : 1));
      });
      if (user) setIsMember(roles.has(user.uid));
    });
  }, [party.id, user]);

  const openBoards = async () => {
    setPicker(true);
    if (boards || !user) return;
    const snap = await getDocs(
      query(collection(db, "boards"), where("memberIds", "array-contains", user.uid), limit(50)),
    );
    setBoards(snap.docs.map((d) => ({ id: d.id, title: (d.get("title") as string) ?? "" })));
  };

  const run = (fn: () => Promise<unknown>) => startTransition(() => void fn());

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-5 sm:flex-row">
        <div className="w-28 shrink-0 sm:w-36">
          <div className="aspect-[2/3] overflow-hidden rounded-xl border border-surface-border bg-surface-hover">
            {party.moviePoster && (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={party.moviePoster} alt="" className="h-full w-full object-cover" />
            )}
          </div>
        </div>

        <div className="min-w-0 flex-1 space-y-2">
          <h1 className="text-2xl font-black tracking-tight text-ink md:text-3xl">{party.movieTitle}</h1>
          <p className="text-sm text-ink-muted">
            {dict.partyHostedBy} <span className="font-semibold text-ink">{party.hostName}</span>
          </p>
          <p className={cn("text-sm font-semibold", isLive ? "text-brand-400" : "text-ink-muted")}>
            {isLive
              ? dict.partyStatusLive
              : isEnded
                ? dict.partyStatusEnded
                : new Date(party.scheduledAtMs).toLocaleString()}
          </p>
          <p className="flex items-center gap-1.5 text-xs text-ink-muted">
            <Users className="h-3.5 w-3.5" aria-hidden />
            {party.memberCount}
          </p>

          {/* Primary action */}
          <div className="flex flex-wrap gap-2 pt-2">
            {isEnded ? (
              <p className="text-sm text-ink-muted">{dict.partyOver}</p>
            ) : isLive && isMember ? (
              <Link
                href={`/watch/${party.movieId}?party=${party.id}`}
                className="inline-flex h-10 items-center gap-2 whitespace-nowrap rounded-lg bg-brand-500 px-5 text-sm font-semibold text-white hover:bg-brand-600"
              >
                <Play className="h-4 w-4 fill-current" aria-hidden />
                {dict.partyWatchTogether}
              </Link>
            ) : isHost ? (
              <button
                type="button"
                onClick={() => run(() => startParty(party.id))}
                disabled={pending}
                className="h-10 whitespace-nowrap rounded-lg bg-brand-500 px-5 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
              >
                {dict.partyStart}
              </button>
            ) : !isMember ? (
              <button
                type="button"
                onClick={() => run(() => joinParty(party.id))}
                disabled={pending}
                className="h-10 whitespace-nowrap rounded-lg bg-brand-500 px-5 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
              >
                {dict.partyJoin}
              </button>
            ) : (
              <p className="text-sm text-ink-muted">{dict.partyWaitingHost}</p>
            )}

            {isHost && !isEnded && (
              <>
                <button
                  type="button"
                  onClick={() => run(async () => setInvited(await inviteFollowers(party.id)))}
                  disabled={pending}
                  className="inline-flex h-10 items-center gap-2 whitespace-nowrap rounded-lg border border-surface-border px-4 text-sm font-medium text-ink hover:bg-surface-hover disabled:opacity-60"
                >
                  <UserPlus className="h-4 w-4" aria-hidden />
                  {dict.partyInviteFollowers}
                </button>
                <button
                  type="button"
                  onClick={openBoards}
                  disabled={pending}
                  className="inline-flex h-10 items-center gap-2 whitespace-nowrap rounded-lg border border-surface-border px-4 text-sm font-medium text-ink hover:bg-surface-hover disabled:opacity-60"
                >
                  <Users className="h-4 w-4" aria-hidden />
                  {dict.partyInviteBoard}
                </button>
                {isLive && (
                  <button
                    type="button"
                    onClick={() => run(() => endParty(party.id))}
                    disabled={pending}
                    className="h-10 whitespace-nowrap rounded-lg border border-surface-border px-4 text-sm font-medium text-ink hover:bg-surface-hover disabled:opacity-60"
                  >
                    {dict.partyEnd}
                  </button>
                )}
              </>
            )}

            {isMember && !isHost && !isEnded && (
              <button
                type="button"
                onClick={() => run(() => leaveParty(party.id))}
                disabled={pending}
                className="h-10 whitespace-nowrap rounded-lg border border-surface-border px-4 text-sm font-medium text-ink hover:bg-surface-hover disabled:opacity-60"
              >
                {dict.partyLeave}
              </button>
            )}
          </div>

          {invited !== null && (
            <p className="text-sm text-brand-400">
              {dict.partyInvitedCount} {invited}
            </p>
          )}
        </div>
      </div>

      {/* Guests */}
      <section>
        <h2 className="text-lg font-bold text-ink">{dict.partyMembers}</h2>
        <ul className="mt-3 space-y-2">
          {members.map((m) => (
            <li key={m.uid} className="flex items-center gap-3 rounded-xl border border-surface-border p-3">
              <UserAvatar name={m.name} url={m.avatar} size={36} />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-semibold text-ink">{m.name || m.uid.slice(0, 8)}</p>
                <p className="flex items-center gap-1 text-xs text-ink-muted">
                  {m.role === "host" && <Crown className="h-3 w-3 text-gold" aria-hidden />}
                  {m.role === "host" ? dict.partyRoleHost : dict.partyRoleGuest}
                </p>
              </div>
              {isHost && m.role !== "host" && !isEnded && (
                <button
                  type="button"
                  onClick={() => run(() => transferHost(party.id, m.uid))}
                  disabled={pending}
                  className="h-8 shrink-0 whitespace-nowrap rounded-lg border border-surface-border px-3 text-xs font-medium text-ink-muted hover:bg-surface-hover hover:text-ink disabled:opacity-60"
                >
                  {dict.partyMakeHost}
                </button>
              )}
            </li>
          ))}
        </ul>
      </section>

      <Modal open={picker} onClose={() => setPicker(false)}>
        <h2 className="text-lg font-bold text-ink">{dict.partyInviteBoard}</h2>
        <div className="mt-4 max-h-72 space-y-2 overflow-y-auto">
          {boards === null ? (
            <p className="py-6 text-center text-sm text-ink-muted">{dict.loadingLabel}</p>
          ) : boards.length === 0 ? (
            <p className="py-6 text-center text-sm text-ink-muted">{dict.boardsMineEmpty}</p>
          ) : (
            boards.map((b) => (
              <button
                key={b.id}
                type="button"
                onClick={() => {
                  setPicker(false);
                  run(async () => setInvited(await inviteBoard(party.id, b.id)));
                }}
                className="w-full truncate rounded-lg border border-surface-border px-3 py-2 text-left text-sm text-ink hover:bg-surface-hover"
              >
                {b.title}
              </button>
            ))
          )}
        </div>
      </Modal>
    </div>
  );
}
