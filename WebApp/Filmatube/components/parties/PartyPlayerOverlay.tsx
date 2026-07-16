"use client";

import { useEffect, useMemo, useState } from "react";
import { addDoc, collection, limit, onSnapshot, orderBy, query, serverTimestamp, type Timestamp } from "firebase/firestore";
import { Send } from "lucide-react";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import { useAuthor } from "@/components/boards/useAuthor";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Must match Android PARTY_REACTIONS. */
const REACTIONS = ["😂", "😮", "❤️", "🔥", "😢", "👏"];
/** How long a floating emoji lives — matches Android REACTION_TTL_MS. */
const REACTION_TTL_MS = 4_000;

interface Msg {
  id: string;
  userName: string;
  text: string;
}
interface Reax {
  id: string;
  emoji: string;
  atMs: number;
}

/**
 * Floating chat + emoji over the party video — the web half of the Android PartyOverlay,
 * reading/writing the same parties/{id}/messages and /reactions.
 */
export function PartyPlayerOverlay({ partyId, dict }: { partyId: string; dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const author = useAuthor();
  const [messages, setMessages] = useState<Msg[]>([]);
  const [reactions, setReactions] = useState<Reax[]>([]);
  const [text, setText] = useState("");
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const q = query(collection(db, "parties", partyId, "messages"), orderBy("createdAt", "desc"), limit(20));
    return onSnapshot(q, (snap) => {
      setMessages(
        snap.docs
          .map((d) => ({ id: d.id, userName: (d.get("userName") as string) ?? "", text: (d.get("text") as string) ?? "" }))
          .reverse(),
      );
    });
  }, [partyId]);

  useEffect(() => {
    const q = query(collection(db, "parties", partyId, "reactions"), orderBy("createdAt", "desc"), limit(12));
    return onSnapshot(q, (snap) => {
      setReactions(
        snap.docs.map((d) => ({
          id: d.id,
          emoji: (d.get("emoji") as string) ?? "",
          atMs: (d.get("createdAt") as Timestamp | null)?.toMillis?.() ?? Date.now(),
        })),
      );
    });
  }, [partyId]);

  // Emoji expire on a timer, not a snapshot — tick so they fade out.
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, []);

  const live = useMemo(() => reactions.filter((r) => now - r.atMs < REACTION_TTL_MS), [reactions, now]);

  const send = async () => {
    if (!user || !text.trim()) return;
    const body = text.trim().slice(0, 200);
    setText("");
    await addDoc(collection(db, "parties", partyId, "messages"), {
      userId: user.uid,
      userName: author.name,
      text: body,
      createdAt: serverTimestamp(),
    });
  };

  const react = async (emoji: string) => {
    if (!user) return;
    await addDoc(collection(db, "parties", partyId, "reactions"), {
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
        {messages.slice(-4).map((m) => (
          <p key={m.id} className="w-fit rounded-xl bg-black/50 px-2.5 py-1 text-xs text-white backdrop-blur-sm">
            <span className="font-bold text-brand-300">{m.userName}</span> {m.text}
          </p>
        ))}

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
            placeholder={dict.partyChatHint}
            className="min-w-0 flex-1 bg-transparent py-2 text-xs text-white placeholder:text-white/40 focus:outline-none"
          />
          <button
            type="button"
            onClick={send}
            disabled={!text.trim()}
            aria-label={dict.sendAction}
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-brand-300 disabled:opacity-40"
          >
            <Send className="h-4 w-4" aria-hidden />
          </button>
        </div>
      </div>
    </>
  );
}
