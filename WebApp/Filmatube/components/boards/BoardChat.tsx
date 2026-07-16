"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { Flag, Pin, Send, SmilePlus, Trash2, Users } from "lucide-react";
import {
  addDoc,
  arrayRemove,
  arrayUnion,
  collection,
  deleteDoc,
  deleteField,
  doc,
  increment,
  limit,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  writeBatch,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import { UserAvatar } from "@/components/social/UserList";
import type { Board } from "@/lib/boards";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

/** Emoji available as reactions — must match Android `BOARD_REACTIONS`. */
const REACTIONS = ["👍", "❤️", "😂", "😮", "😢", "🔥"];
/** A typing doc is "fresh" for this long — matches the Android 6s window. */
const TYPING_TTL_MS = 6_000;

interface ChatMessage {
  id: string;
  userId: string;
  userName: string;
  userAvatar: string;
  text: string;
  hasSpoiler: boolean;
  reactions: Record<string, string>;
  replyToName: string;
  replyToText: string;
  movieId: string;
  movieTitle: string;
  moviePoster: string;
  isMine: boolean;
}

interface TypingUser {
  uid: string;
  name: string;
  updatedAtMs: number;
}

/**
 * Real-time board chat — reads/writes the same `boards/{id}/messages`, `/typing` and `/members`
 * documents as the Android client, so both platforms see the same conversation live.
 */
export function BoardChat({
  board,
  initialIsMember,
  dict,
}: {
  board: Board;
  initialIsMember: boolean;
  dict: Dictionary["catalog"];
}) {
  const { user } = useAuth();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [typing, setTyping] = useState<TypingUser[]>([]);
  const [now, setNow] = useState(0);
  const [isMember, setIsMember] = useState(initialIsMember);
  const [pinnedId, setPinnedId] = useState(board.pinnedMessageId);
  const [muted, setMuted] = useState(false);
  const [text, setText] = useState("");
  const [replyTo, setReplyTo] = useState<ChatMessage | null>(null);
  const [revealed, setRevealed] = useState<Record<string, boolean>>({});
  const [reported, setReported] = useState<Record<string, boolean>>({});
  const [pickerFor, setPickerFor] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const bottom = useRef<HTMLDivElement>(null);
  const lastTypingAt = useRef(0);

  const isOwner = !!user && board.ownerId === user.uid;

  // ── Messages (realtime) ────────────────────────────────────
  useEffect(() => {
    const q = query(collection(db, "boards", board.id, "messages"), orderBy("createdAt", "asc"), limit(200));
    return onSnapshot(q, (snap) => {
      setMessages(
        snap.docs.map((d) => ({
          id: d.id,
          userId: d.get("userId") ?? "",
          userName: d.get("userName") ?? "",
          userAvatar: d.get("userAvatar") ?? "",
          text: d.get("text") ?? "",
          hasSpoiler: d.get("hasSpoiler") ?? false,
          reactions: (d.get("reactions") as Record<string, string>) ?? {},
          replyToName: d.get("replyToName") ?? "",
          replyToText: d.get("replyToText") ?? "",
          movieId: d.get("movieId") ?? "",
          movieTitle: d.get("movieTitle") ?? "",
          moviePoster: d.get("moviePoster") ?? "",
          isMine: !!user && d.get("userId") === user.uid,
        })),
      );
    });
  }, [board.id, user]);

  // ── Pinned message (realtime — the owner can pin from either client) ──
  useEffect(() => {
    return onSnapshot(doc(db, "boards", board.id), (snap) => setPinnedId(snap.get("pinnedMessageId") ?? ""));
  }, [board.id]);

  // ── My membership + mute state (realtime) ──────────────────
  useEffect(() => {
    if (!user) return;
    return onSnapshot(doc(db, "boards", board.id, "members", user.uid), (snap) => {
      setIsMember(snap.exists());
      setMuted(snap.get("muted") === true);
    });
  }, [board.id, user]);

  // ── Typing indicator ───────────────────────────────────────
  useEffect(() => {
    return onSnapshot(collection(db, "boards", board.id, "typing"), (snap) => {
      setTyping(
        snap.docs.map((d) => ({
          uid: d.id,
          name: d.get("name") ?? "",
          updatedAtMs: (d.get("updatedAt") as { toMillis?: () => number })?.toMillis?.() ?? 0,
        })),
      );
    });
  }, [board.id]);

  // Typing docs expire on a timer, not on a snapshot — tick so stale ones disappear.
  useEffect(() => {
    setNow(Date.now());
    const t = setInterval(() => setNow(Date.now()), 3_000);
    return () => clearInterval(t);
  }, []);

  const typingNow = useMemo(
    () => typing.filter((t) => t.uid !== user?.uid && now - t.updatedAtMs < TYPING_TTL_MS),
    [typing, user, now],
  );

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length]);

  // ── Actions ────────────────────────────────────────────────
  const onType = (value: string) => {
    setText(value);
    if (!user || !isMember || muted) return;
    // Throttle: one typing write every ~3s while the composer is active.
    const at = Date.now();
    if (at - lastTypingAt.current < 3_000) return;
    lastTypingAt.current = at;
    void setDoc(doc(db, "boards", board.id, "typing", user.uid), {
      name: user.displayName ?? "",
      updatedAt: serverTimestamp(),
    });
  };

  const send = async () => {
    if (!user || !text.trim() || muted) return;
    const body = text.trim();
    setText("");
    const quoted = replyTo;
    setReplyTo(null);
    await addDoc(collection(db, "boards", board.id, "messages"), {
      userId: user.uid,
      userName: user.displayName ?? "",
      userAvatar: user.photoURL ?? "",
      text: body,
      hasSpoiler: false,
      replyToName: quoted?.userName ?? "",
      replyToText: quoted?.text ?? "",
      createdAt: serverTimestamp(),
    });
    void deleteDoc(doc(db, "boards", board.id, "typing", user.uid));
  };

  const toggleReaction = async (m: ChatMessage, emoji: string) => {
    if (!user) return;
    setPickerFor(null);
    const mine = m.reactions[user.uid];
    await updateDoc(doc(db, "boards", board.id, "messages", m.id), {
      // Dotted path — a reactions-only update, which the security rules allow for any member.
      [`reactions.${user.uid}`]: mine === emoji ? deleteField() : emoji,
    });
  };

  const remove = async (m: ChatMessage) => {
    await deleteDoc(doc(db, "boards", board.id, "messages", m.id));
  };

  /** Owner-only: the board update rule lets the owner write any field. */
  const togglePin = async (m: ChatMessage) => {
    if (!isOwner) return;
    await updateDoc(doc(db, "boards", board.id), {
      pinnedMessageId: pinnedId === m.id ? "" : m.id,
    });
  };

  const report = async (m: ChatMessage) => {
    if (!user) return;
    await addDoc(collection(db, "reports"), {
      type: "board_message",
      boardId: board.id,
      targetId: m.id,
      reportedUserId: m.userId,
      reporterId: user.uid,
      reason: "",
      status: "pending",
      createdAt: serverTimestamp(),
    });
    setReported((prev) => ({ ...prev, [m.id]: true }));
  };

  const join = async () => {
    if (!user) return;
    setBusy(true);
    try {
      const batch = writeBatch(db);
      // Membership-only board update (memberIds + memberCount) — the only board write the rules
      // allow a non-owner to make. arrayUnion keeps it idempotent across both clients.
      batch.update(doc(db, "boards", board.id), {
        memberIds: arrayUnion(user.uid),
        memberCount: increment(1),
      });
      batch.set(doc(db, "boards", board.id, "members", user.uid), {
        userId: user.uid,
        role: "member",
        joinedAt: serverTimestamp(),
      });
      await batch.commit();
    } finally {
      setBusy(false);
    }
  };

  const leave = async () => {
    if (!user || isOwner) return;
    setBusy(true);
    try {
      const batch = writeBatch(db);
      batch.update(doc(db, "boards", board.id), {
        memberIds: arrayRemove(user.uid),
        memberCount: increment(-1),
      });
      batch.delete(doc(db, "boards", board.id, "members", user.uid));
      await batch.commit();
    } finally {
      setBusy(false);
    }
  };

  // ── Render ─────────────────────────────────────────────────
  const pinned = messages.find((m) => m.id === pinnedId);

  const reactionSummary = (m: ChatMessage) => {
    const counts = new Map<string, number>();
    Object.values(m.reactions).forEach((e) => counts.set(e, (counts.get(e) ?? 0) + 1));
    return [...counts.entries()];
  };

  return (
    <section className="mt-8 space-y-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-lg font-bold text-ink">{board.title}</h2>
        <div className="flex shrink-0 items-center gap-2">
          <Link
            href={`/boards/${board.id}/members`}
            className="inline-flex h-9 items-center gap-1.5 rounded-full border border-surface-border px-4 text-sm font-medium text-ink-muted transition-colors hover:bg-surface-hover hover:text-ink"
          >
            <Users className="h-4 w-4" aria-hidden />
            {dict.membersAction}
          </Link>
          {user && !isOwner && (
            <button
              type="button"
              onClick={isMember ? leave : join}
              disabled={busy}
              className={cn(
                "h-9 rounded-full px-5 text-sm font-semibold transition-colors disabled:opacity-60",
                isMember
                  ? "border border-surface-border text-ink hover:bg-surface-hover"
                  : "bg-brand-500 text-white hover:bg-brand-600",
              )}
            >
              {isMember ? dict.boardLeave : dict.boardJoin}
            </button>
          )}
        </div>
      </div>

      {pinned && (
        <div className="flex items-start gap-2 rounded-xl border border-brand-500/40 bg-brand-500/10 p-3">
          <Pin className="mt-0.5 h-4 w-4 shrink-0 text-brand-400" aria-hidden />
          <div className="min-w-0">
            <p className="text-xs font-semibold uppercase tracking-wide text-brand-400">{dict.pinnedLabel}</p>
            <p className="truncate text-sm text-ink">
              <span className="font-semibold">{pinned.userName}</span>{" "}
              {pinned.hasSpoiler ? dict.spoilerHidden : pinned.text || pinned.movieTitle}
            </p>
          </div>
        </div>
      )}

      <div className="space-y-5 rounded-xl border border-surface-border p-4">
        {messages.length === 0 ? (
          <p className="py-10 text-center text-sm text-ink-muted">{dict.boardChatEmpty}</p>
        ) : (
          messages.map((m) => (
            <div key={m.id} className="space-y-1.5">
              <div className="flex items-center gap-2">
                <Link href={`/u/${m.userId}`}>
                  <UserAvatar name={m.userName} url={m.userAvatar} size={28} />
                </Link>
                <Link href={`/u/${m.userId}`} className="text-sm font-semibold text-ink hover:underline">
                  {m.userName}
                </Link>
                {m.isMine && <span className="text-xs text-brand-400">{dict.youLabel}</span>}
              </div>

              {m.replyToName && (
                <div className="ml-9 border-l-2 border-brand-500/60 pl-2 text-xs text-ink-muted">
                  <span className="font-semibold text-ink-muted">{m.replyToName}</span>
                  <p className="truncate">{m.replyToText}</p>
                </div>
              )}

              <div className="ml-9 space-y-1.5">
                {m.movieId && (
                  <Link
                    href={`/movie/${m.movieId}`}
                    className="flex items-center gap-3 rounded-lg border border-surface-border bg-surface-hover p-2 hover:border-brand-500"
                  >
                    {m.moviePoster && (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img src={m.moviePoster} alt="" className="h-16 w-11 rounded object-cover" />
                    )}
                    <span className="min-w-0">
                      <span className="block text-xs text-ink-muted">{dict.boardMovieCard}</span>
                      <span className="block truncate text-sm font-semibold text-ink">{m.movieTitle}</span>
                    </span>
                  </Link>
                )}

                {m.hasSpoiler && !revealed[m.id] ? (
                  <button
                    type="button"
                    onClick={() => setRevealed((prev) => ({ ...prev, [m.id]: true }))}
                    className="w-full rounded-lg bg-surface-hover px-3 py-2 text-left text-sm text-ink-muted"
                  >
                    {dict.spoilerHidden}
                  </button>
                ) : (
                  m.text && (
                    <>
                      {m.hasSpoiler && <span className="text-xs font-semibold text-red-400">{dict.spoilerChip}</span>}
                      <p className="whitespace-pre-wrap break-words text-sm text-ink">{m.text}</p>
                    </>
                  )
                )}

                {reactionSummary(m).length > 0 && (
                  <div className="flex flex-wrap gap-1">
                    {reactionSummary(m).map(([emoji, count]) => (
                      <button
                        key={emoji}
                        type="button"
                        onClick={() => toggleReaction(m, emoji)}
                        disabled={!user}
                        className={cn(
                          "rounded-full border px-2 py-0.5 text-xs transition-colors disabled:opacity-60",
                          user && m.reactions[user.uid] === emoji
                            ? "border-brand-500 bg-brand-500/15 text-ink"
                            : "border-surface-border text-ink-muted hover:bg-surface-hover",
                        )}
                      >
                        {emoji} {count}
                      </button>
                    ))}
                  </div>
                )}

                {pickerFor === m.id && (
                  <div className="flex gap-1 rounded-full border border-surface-border bg-surface p-1">
                    {REACTIONS.map((e) => (
                      <button
                        key={e}
                        type="button"
                        onClick={() => toggleReaction(m, e)}
                        className="rounded-full px-1.5 py-0.5 text-base hover:bg-surface-hover"
                      >
                        {e}
                      </button>
                    ))}
                  </div>
                )}

                {user && (
                  <div className="flex items-center gap-3 text-xs text-ink-muted">
                    <button
                      type="button"
                      onClick={() => setPickerFor(pickerFor === m.id ? null : m.id)}
                      className="inline-flex items-center gap-1 hover:text-ink"
                    >
                      <SmilePlus className="h-3.5 w-3.5" aria-hidden />
                      {dict.boardReact}
                    </button>
                    {isMember && !muted && (
                      <button type="button" onClick={() => setReplyTo(m)} className="hover:text-ink">
                        {dict.replyAction}
                      </button>
                    )}
                    {isOwner && (
                      <button
                        type="button"
                        onClick={() => togglePin(m)}
                        className="inline-flex items-center gap-1 hover:text-ink"
                      >
                        <Pin className="h-3.5 w-3.5" aria-hidden />
                        {pinnedId === m.id ? dict.unpinAction : dict.pinAction}
                      </button>
                    )}
                    {m.isMine || isOwner ? (
                      <button
                        type="button"
                        onClick={() => remove(m)}
                        className="inline-flex items-center gap-1 hover:text-ink"
                      >
                        <Trash2 className="h-3.5 w-3.5" aria-hidden />
                        {dict.deleteLabel}
                      </button>
                    ) : (
                      <button
                        type="button"
                        onClick={() => report(m)}
                        disabled={reported[m.id]}
                        className="inline-flex items-center gap-1 hover:text-ink disabled:opacity-50"
                      >
                        <Flag className="h-3.5 w-3.5" aria-hidden />
                        {reported[m.id] ? dict.reportedAction : dict.reportAction}
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          ))
        )}
        <div ref={bottom} />
      </div>

      <p className="h-4 text-xs text-ink-muted" aria-live="polite">
        {typingNow.length === 1
          ? `${typingNow[0].name} ${dict.boardTyping}`
          : typingNow.length > 1
            ? dict.boardTypingMany
            : ""}
      </p>

      {/* ── Composer (gated on membership + mute) ─────────────── */}
      {!user ? null : muted ? (
        <p className="rounded-xl border border-surface-border bg-surface-hover/40 p-4 text-center text-sm text-ink-muted">
          {dict.boardMuted}
        </p>
      ) : !isMember ? (
        <p className="rounded-xl border border-surface-border bg-surface-hover/40 p-4 text-center text-sm text-ink-muted">
          {dict.boardJoinToChat}
        </p>
      ) : (
        <div className="space-y-2 rounded-xl border border-surface-border p-4">
          {replyTo && (
            <div className="flex items-center justify-between text-sm text-brand-400">
              <span className="truncate">
                {dict.replyingTo} {replyTo.userName}
              </span>
              <button type="button" onClick={() => setReplyTo(null)} className="text-ink-muted hover:text-ink">
                {dict.cancelReply}
              </button>
            </div>
          )}
          <div className="flex items-end gap-2">
            <textarea
              value={text}
              onChange={(e) => onType(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  void send();
                }
              }}
              placeholder={dict.boardMessagePlaceholder}
              rows={1}
              className="max-h-32 flex-1 resize-none rounded-lg border border-surface-border bg-surface-hover px-3 py-2 text-sm text-ink placeholder:text-ink-faint focus:border-brand-500 focus:outline-none"
            />
            <button
              type="button"
              onClick={send}
              disabled={!text.trim()}
              aria-label={dict.sendAction}
              className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-brand-500 text-white hover:bg-brand-600 disabled:opacity-50"
            >
              <Send className="h-4 w-4" aria-hidden />
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
