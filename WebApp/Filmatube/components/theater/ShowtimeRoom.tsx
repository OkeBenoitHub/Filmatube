"use client";

import { useEffect, useRef, useState, useTransition } from "react";
import Link from "next/link";
import {
  addDoc,
  collection,
  doc,
  limit,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
} from "firebase/firestore";
import { Bell, Check, EyeOff, Play, Send, Users } from "lucide-react";
import { db } from "@/lib/firebase";
import { setRemind, setRsvp } from "@/app/theater/actions";
import { Countdown } from "@/components/theater/Countdown";
import { LiveDot, PremiereBadge } from "@/components/theater/ShowtimeCard";
import { useAuthor } from "@/components/boards/useAuthor";
import { UserAvatar } from "@/components/social/UserList";
import { useAuth } from "@/components/providers/AuthProvider";
import type { Attendance, Showtime, ShowtimeAttendee } from "@/lib/theater-model";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

/** Minimum gap between two messages from this browser. Mirrors the Android throttle. */
const CHAT_INTERVAL_MS = 2_000;
/** Presence goes stale this long after its last heartbeat. Mirrors Android. */
const PRESENCE_STALE_AFTER_MS = 90_000;

interface ChatMessage {
  id: string;
  userId: string;
  userName: string;
  userAvatar: string;
  text: string;
  isSpoiler: boolean;
  isMine: boolean;
}

/**
 * One showtime: what it is, when it starts, who's coming — and the way in.
 *
 * Detail and lobby are one destination rather than two routes, matching Android: the
 * showtime's own status decides whether you see an RSVP, a countdown with pre-show chat,
 * or the door into the room. Two routes would have raced the same status field and
 * navigated out from under you the moment the doors opened.
 */
export function ShowtimeRoom({
  initialShowtime,
  initialAttendees,
  initialAttendance,
  dict,
}: {
  initialShowtime: Showtime;
  initialAttendees: ShowtimeAttendee[];
  initialAttendance: Attendance;
  dict: Dictionary["catalog"];
}) {
  const { user } = useAuth();
  const author = useAuthor();

  const [showtime, setShowtime] = useState(initialShowtime);
  const [attendees, setAttendees] = useState(initialAttendees);
  const [attendance, setAttendance] = useState(initialAttendance);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [presentCount, setPresentCount] = useState(0);
  const [revealed, setRevealed] = useState<Record<string, boolean>>({});
  const [text, setText] = useState("");
  const [spoiler, setSpoiler] = useState(false);
  const [throttled, setThrottled] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  const lastSentAt = useRef(0);
  const bottom = useRef<HTMLDivElement>(null);

  const live = showtime.status === "live";
  const inLobby = showtime.status === "lobby";
  const ended = showtime.status === "ended";
  const open = live || inLobby;
  const full = showtime.capacity > 0 && showtime.attendeesCount >= showtime.capacity;

  // Live showtime doc — status and attendee count both move under the visitor's feet, and
  // the doors opening has to take effect without a reload.
  useEffect(() => {
    return onSnapshot(doc(db, "showtimes", showtime.id), (snap) => {
      if (!snap.exists()) return;
      setShowtime((s) => ({
        ...s,
        status: (snap.get("status") as string) ?? s.status,
        attendeesCount: (snap.get("attendeesCount") as number) ?? s.attendeesCount,
      }));
    });
  }, [showtime.id]);

  // My own RSVP, realtime — so an RSVP made on the phone shows here too.
  useEffect(() => {
    if (!user) return;
    return onSnapshot(doc(db, "showtimes", showtime.id, "attendees", user.uid), (snap) =>
      setAttendance({
        going: snap.exists() ? ((snap.get("rsvp") as boolean) ?? false) : false,
        remind: snap.exists() ? ((snap.get("remind") as boolean) ?? false) : false,
      }),
    );
  }, [showtime.id, user]);

  // Faces. Names come from the server render; someone who RSVPs while you're looking shows
  // up without a name until the next full load, which is fine for a lobby.
  useEffect(() => {
    return onSnapshot(collection(db, "showtimes", showtime.id, "attendees"), (snap) => {
      const ids = snap.docs.map((d) => d.id);
      setAttendees((prev) => {
        const known = prev.filter((a) => ids.includes(a.uid));
        const extra = ids
          .filter((uid) => !prev.some((a) => a.uid === uid))
          .map((uid) => ({ uid, name: "", avatar: "" }));
        return [...known, ...extra];
      });
    });
  }, [showtime.id]);

  // Who's actually in the room. Freshness is judged here against the ticking clock rather
  // than by a query bound, since the cutoff moves every second.
  useEffect(() => {
    if (!open) return;
    return onSnapshot(collection(db, "showtimes", showtime.id, "presence"), (snap) => {
      const cutoff = Date.now() - PRESENCE_STALE_AFTER_MS;
      setPresentCount(
        snap.docs.filter((d) => {
          const at = (d.get("presentAt") as { toMillis?: () => number })?.toMillis?.() ?? 0;
          return at > cutoff;
        }).length,
      );
    });
  }, [showtime.id, open]);

  // Pre-show / live chat. Newest-first + reverse, matching Android and boards: an ascending
  // limit would pin the window to the room's oldest messages.
  useEffect(() => {
    if (!open) return;
    const q = query(
      collection(db, "showtimes", showtime.id, "chat"),
      orderBy("createdAt", "desc"),
      limit(100),
    );
    return onSnapshot(q, (snap) => {
      setMessages(
        snap.docs.reverse().map((d) => ({
          id: d.id,
          userId: (d.get("userId") as string) ?? "",
          userName: (d.get("userName") as string) ?? "",
          userAvatar: (d.get("userAvatar") as string) ?? "",
          text: (d.get("text") as string) ?? "",
          isSpoiler: (d.get("isSpoiler") as boolean) ?? false,
          isMine: !!user && d.get("userId") === user.uid,
        })),
      );
    });
  }, [showtime.id, open, user]);

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length]);

  const run = (fn: () => Promise<unknown>) =>
    startTransition(() => {
      setError(null);
      void fn().catch(() => setError(dict.theaterActionFailed));
    });

  const send = async () => {
    if (!user || !text.trim()) return;

    const at = Date.now();
    if (at - lastSentAt.current < CHAT_INTERVAL_MS) {
      setThrottled(true);
      setTimeout(() => setThrottled(false), 2000);
      return; // Keep the draft — the user should be able to resend, not retype.
    }
    lastSentAt.current = at;

    const body = text.trim().slice(0, 200);
    const marked = spoiler;
    setText("");
    setSpoiler(false);
    try {
      await addDoc(collection(db, "showtimes", showtime.id, "chat"), {
        userId: user.uid,
        userName: author.name,
        userAvatar: author.avatar,
        text: body,
        isSpoiler: marked,
        createdAt: serverTimestamp(),
      });
    } catch {
      // Don't hold the user to a throttle they never got the benefit of.
      lastSentAt.current = 0;
      setText(body);
      setError(dict.theaterActionFailed);
    }
  };

  const attendeeLabel = (live ? dict.theaterWatching : dict.theaterGoing).replace(
    "{n}",
    String(showtime.attendeesCount),
  );

  return (
    <div className="space-y-8">
      {/* ── Header ─────────────────────────────────────── */}
      <div className="flex flex-col gap-5 sm:flex-row">
        <div className="w-28 shrink-0 sm:w-36">
          <div className="aspect-[2/3] overflow-hidden rounded-xl border border-surface-border bg-surface-hover">
            {showtime.posterUrl && (
              // eslint-disable-next-line @next/next/no-img-element
              <img src={showtime.posterUrl} alt="" className="h-full w-full object-cover" />
            )}
          </div>
        </div>

        <div className="min-w-0 flex-1 space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-2xl font-black tracking-tight text-ink md:text-3xl">{showtime.movieTitle}</h1>
            {showtime.isPremiere && <PremiereBadge label={dict.theaterPremiere} />}
          </div>

          <p className="text-sm text-ink-muted" suppressHydrationWarning>
            {new Date(showtime.startAtMs).toLocaleString()}
          </p>

          <p className="flex items-center gap-2">
            {live && <LiveDot />}
            <Countdown
              startAtMs={showtime.startAtMs}
              live={live}
              dict={dict}
              className={cn("text-base font-semibold", live ? "text-brand-400" : "text-ink")}
            />
          </p>

          {showtime.capacity > 0 && (
            <p className="text-xs text-ink-muted">
              {dict.theaterCapacity
                .replace("{n}", String(showtime.attendeesCount))
                .replace("{total}", String(showtime.capacity))}
            </p>
          )}

          {/* ── Primary action ── */}
          <div className="flex flex-wrap items-center gap-2 pt-2">
            {ended ? (
              <p className="text-sm text-ink-muted">{dict.theaterOver}</p>
            ) : open ? (
              <Link
                href={`/watch/${showtime.movieId}?showtime=${showtime.id}`}
                className="inline-flex h-10 items-center gap-2 whitespace-nowrap rounded-lg bg-brand-500 px-5 text-sm font-semibold text-white hover:bg-brand-600"
              >
                <Play className="h-4 w-4 fill-current" aria-hidden />
                {live ? dict.theaterEnter : dict.theaterEnterLobby}
              </Link>
            ) : full && !attendance.going ? (
              <p className="text-sm font-medium text-red-400">{dict.theaterFullMessage}</p>
            ) : attendance.going ? (
              <button
                type="button"
                onClick={() => run(() => setRsvp(showtime.id, false))}
                disabled={pending}
                className="inline-flex h-10 items-center gap-2 whitespace-nowrap rounded-lg border border-surface-border px-5 text-sm font-medium text-ink hover:bg-surface-hover disabled:opacity-60"
              >
                <Check className="h-4 w-4 text-brand-400" aria-hidden />
                {dict.theaterRsvpCancel}
              </button>
            ) : (
              <button
                type="button"
                onClick={() => run(() => setRsvp(showtime.id, true))}
                disabled={pending || !user}
                className="h-10 whitespace-nowrap rounded-lg bg-brand-500 px-5 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
              >
                {dict.theaterRsvp}
              </button>
            )}
          </div>

          {/* ── Remind me ── */}
          {attendance.going && !open && !ended && (
            <label className="flex cursor-pointer items-center gap-3 pt-2">
              <Bell className="h-4 w-4 shrink-0 text-ink-muted" aria-hidden />
              <span className="min-w-0 flex-1">
                <span className="block text-sm text-ink">{dict.theaterRemindTitle}</span>
                <span className="block text-xs text-ink-muted">{dict.theaterRemindSubtitle}</span>
              </span>
              <input
                type="checkbox"
                checked={attendance.remind}
                onChange={(e) => run(() => setRemind(showtime.id, e.target.checked))}
                className="h-4 w-4 shrink-0 accent-brand-500"
              />
            </label>
          )}

          {error && <p className="pt-1 text-sm text-red-400">{error}</p>}
        </div>
      </div>

      {/* ── Who's coming / who's here ──────────────────── */}
      <section>
        <h2 className="flex items-center gap-2 text-lg font-bold text-ink">
          <Users className="h-4 w-4" aria-hidden />
          {attendeeLabel}
        </h2>
        {open && <p className="mt-0.5 text-sm text-brand-400">{dict.theaterInRoomNow.replace("{n}", String(presentCount))}</p>}

        {attendees.length > 0 && (
          <ul className="mt-3 flex flex-wrap gap-3">
            {attendees.map((a) => (
              <li key={a.uid} className="flex w-16 flex-col items-center gap-1">
                <UserAvatar name={a.name} url={a.avatar} size={44} />
                <span className="w-full truncate text-center text-[11px] text-ink-muted">
                  {a.name || a.uid.slice(0, 6)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* ── Pre-show / live chat ───────────────────────── */}
      {open && (
        <section>
          <h2 className="text-lg font-bold text-ink">{dict.theaterPreshowChat}</h2>

          <div className="mt-3 max-h-96 space-y-3 overflow-y-auto rounded-xl border border-surface-border p-4">
            {messages.length === 0 ? (
              <p className="py-6 text-center text-sm text-ink-muted">{dict.theaterChatEmpty}</p>
            ) : (
              messages.map((m) => (
                <div key={m.id} className="flex gap-3">
                  <UserAvatar name={m.userName} url={m.userAvatar} size={28} />
                  <div className="min-w-0">
                    <p
                      className={cn(
                        "text-xs font-semibold",
                        m.isMine ? "text-brand-400" : "text-ink-muted",
                      )}
                    >
                      {m.userName || m.userId.slice(0, 6)}
                    </p>
                    {m.isSpoiler && !revealed[m.id] ? (
                      <button
                        type="button"
                        onClick={() => setRevealed((r) => ({ ...r, [m.id]: true }))}
                        className="mt-0.5 inline-flex items-center gap-1.5 rounded-lg bg-surface-hover px-2.5 py-1 text-sm text-ink-muted hover:text-ink"
                      >
                        <EyeOff className="h-3.5 w-3.5" aria-hidden />
                        {dict.theaterSpoilerHidden}
                      </button>
                    ) : (
                      <p className="break-words text-sm text-ink">{m.text}</p>
                    )}
                  </div>
                </div>
              ))
            )}
            <div ref={bottom} />
          </div>

          {user ? (
            <div className="mt-3 space-y-2">
              <div className="flex items-center gap-2">
                <input
                  value={text}
                  onChange={(e) => setText(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !e.shiftKey) {
                      e.preventDefault();
                      void send();
                    }
                  }}
                  maxLength={200}
                  placeholder={dict.theaterChatHint}
                  className="h-10 min-w-0 flex-1 rounded-lg border border-surface-border bg-transparent px-3 text-sm text-ink placeholder:text-ink-muted focus:outline-none focus:ring-1 focus:ring-brand-500"
                />
                <button
                  type="button"
                  onClick={() => void send()}
                  disabled={!text.trim()}
                  aria-label={dict.theaterChatSend}
                  className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-brand-500 text-white hover:bg-brand-600 disabled:opacity-40"
                >
                  <Send className="h-4 w-4" aria-hidden />
                </button>
              </div>
              <label className="flex cursor-pointer items-center gap-2 text-xs text-ink-muted">
                <input
                  type="checkbox"
                  checked={spoiler}
                  onChange={(e) => setSpoiler(e.target.checked)}
                  className="h-3.5 w-3.5 accent-brand-500"
                />
                {dict.theaterSpoilerToggle}
              </label>
              {throttled && <p className="text-xs text-red-400">{dict.theaterChatTooFast}</p>}
            </div>
          ) : (
            <p className="mt-3 text-sm text-ink-muted">{dict.theaterSignInToChat}</p>
          )}
        </section>
      )}
    </div>
  );
}
