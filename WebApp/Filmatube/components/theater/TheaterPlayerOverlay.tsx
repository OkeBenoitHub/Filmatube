"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import {
  addDoc,
  collection,
  limit,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  type Timestamp,
} from "firebase/firestore";
import { EyeOff, Send } from "lucide-react";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import { useAuthor } from "@/components/boards/useAuthor";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

/** Same set as the watch-party overlay (Android `THEATER_REACTIONS`). */
const REACTIONS = ["😂", "😮", "❤️", "🔥", "😢", "👏"];
/** How long a floating emoji lives — matches Android REACTION_TTL_MS. */
const REACTION_TTL_MS = 4_000;
/** Minimum gap between two messages from this browser — a courtesy throttle only; the
 * server-side limit lands with the Day 170 automation, same caveat as Android. */
const CHAT_INTERVAL_MS = 2_000;

interface Msg {
  id: string;
  userId: string;
  userName: string;
  userAvatar: string;
  text: string;
  isSpoiler: boolean;
  isMine: boolean;
}
interface Reax {
  id: string;
  emoji: string;
  atMs: number;
}

/**
 * Floating chat + emoji over the theater video — the web half of Android's Days 158-159,
 * reading/writing the same `showtimes/{id}/chat` and `/reactions`.
 *
 * Unlike the party overlay this carries spoiler tags: a public screening has people at
 * different points of attention (and late joiners), so spoiler tagging earns its place here
 * in a way it doesn't in a private room everyone entered together.
 */
export function TheaterPlayerOverlay({ showtimeId, dict }: { showtimeId: string; dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const author = useAuthor();
  const [messages, setMessages] = useState<Msg[]>([]);
  const [reactions, setReactions] = useState<Reax[]>([]);
  const [revealed, setRevealed] = useState<Record<string, boolean>>({});
  const [text, setText] = useState("");
  const [spoiler, setSpoiler] = useState(false);
  const [throttled, setThrottled] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  const lastSentAt = useRef(0);

  useEffect(() => {
    const q = query(collection(db, "showtimes", showtimeId, "chat"), orderBy("createdAt", "desc"), limit(20));
    return onSnapshot(q, (snap) => {
      setMessages(
        snap.docs
          .map((d) => ({
            id: d.id,
            userId: (d.get("userId") as string) ?? "",
            userName: (d.get("userName") as string) ?? "",
            userAvatar: (d.get("userAvatar") as string) ?? "",
            text: (d.get("text") as string) ?? "",
            isSpoiler: (d.get("isSpoiler") as boolean) ?? false,
            isMine: !!user && d.get("userId") === user.uid,
          }))
          .reverse(),
      );
    });
  }, [showtimeId, user]);

  useEffect(() => {
    const q = query(collection(db, "showtimes", showtimeId, "reactions"), orderBy("createdAt", "desc"), limit(12));
    return onSnapshot(q, (snap) => {
      setReactions(
        snap.docs.map((d) => ({
          id: d.id,
          emoji: (d.get("emoji") as string) ?? "",
          atMs: (d.get("createdAt") as Timestamp | null)?.toMillis?.() ?? Date.now(),
        })),
      );
    });
  }, [showtimeId]);

  // Emoji expire on a timer, not a snapshot — tick so they fade out.
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, []);

  const live = useMemo(() => reactions.filter((r) => now - r.atMs < REACTION_TTL_MS), [reactions, now]);

  const send = async () => {
    if (!user || !text.trim()) return;

    const at = Date.now();
    if (at - lastSentAt.current < CHAT_INTERVAL_MS) {
      setThrottled(true);
      setTimeout(() => setThrottled(false), 2000);
      return; // Keep the draft — the viewer should be able to resend, not retype.
    }
    lastSentAt.current = at;

    const body = text.trim().slice(0, 200);
    const marked = spoiler;
    setText("");
    setSpoiler(false);
    try {
      await addDoc(collection(db, "showtimes", showtimeId, "chat"), {
        userId: user.uid,
        userName: author.name,
        userAvatar: author.avatar,
        text: body,
        isSpoiler: marked,
        createdAt: serverTimestamp(),
      });
    } catch {
      // Don't hold the viewer to a throttle they never got the benefit of.
      lastSentAt.current = 0;
      setText(body);
    }
  };

  const react = async (emoji: string) => {
    if (!user) return;
    await addDoc(collection(db, "showtimes", showtimeId, "reactions"), {
      userId: user.uid,
      userName: author.name,
      emoji,
      createdAt: serverTimestamp(),
    });
  };

  if (!user) return null;

  return (
    <>
      {/* Floating emoji */}
      <div className="pointer-events-none absolute bottom-32 right-6 z-30 flex flex-col items-center gap-1">
        {live.slice(0, 6).map((r) => (
          <span key={r.id} className="animate-bounce text-3xl drop-shadow-lg" aria-hidden>
            {r.emoji}
          </span>
        ))}
      </div>

      {/* Chat + composer */}
      <div className="absolute bottom-24 left-4 z-30 w-72 max-w-[70vw] space-y-1.5">
        {messages.slice(-4).map((m) =>
          m.isSpoiler && !revealed[m.id] ? (
            <button
              key={m.id}
              type="button"
              onClick={() => setRevealed((r) => ({ ...r, [m.id]: true }))}
              className="flex w-fit items-center gap-1.5 rounded-xl bg-black/50 px-2.5 py-1 text-xs text-white/80 backdrop-blur-sm hover:text-white"
            >
              <EyeOff className="h-3 w-3" aria-hidden />
              <span className="font-bold text-brand-300">{m.userName}</span> {dict.theaterSpoilerHidden}
            </button>
          ) : (
            <p key={m.id} className="w-fit rounded-xl bg-black/50 px-2.5 py-1 text-xs text-white backdrop-blur-sm">
              <span className={cn("font-bold", m.isMine ? "text-brand-300" : "text-brand-300")}>{m.userName}</span>{" "}
              {m.text}
            </p>
          ),
        )}

        <div className="flex gap-1">
          {REACTIONS.map((e) => (
            <button
              key={e}
              type="button"
              onClick={() => react(e)}
              className="rounded-full bg-black/50 px-1.5 py-0.5 text-base transition-transform hover:scale-125"
            >
              {e}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-1 rounded-full bg-black/60 pl-3 backdrop-blur-sm">
          <input
            value={text}
            onChange={(e) => setText(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                void send();
              }
            }}
            maxLength={200}
            placeholder={dict.theaterChatHint}
            className="min-w-0 flex-1 bg-transparent py-2 text-xs text-white placeholder:text-white/40 focus:outline-none"
          />
          <button
            type="button"
            onClick={() => setSpoiler((s) => !s)}
            aria-pressed={spoiler}
            aria-label={dict.theaterSpoilerToggle}
            className={cn(
              "flex h-8 w-8 shrink-0 items-center justify-center rounded-full",
              spoiler ? "text-brand-300" : "text-white/40 hover:text-white/70",
            )}
          >
            <EyeOff className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={send}
            disabled={!text.trim()}
            aria-label={dict.theaterChatSend}
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-brand-300 disabled:opacity-40"
          >
            <Send className="h-4 w-4" aria-hidden />
          </button>
        </div>
        {throttled && <p className="text-[11px] text-red-300">{dict.theaterChatTooFast}</p>}
      </div>
    </>
  );
}
