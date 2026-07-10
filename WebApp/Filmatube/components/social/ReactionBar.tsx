"use client";

import { useCallback, useEffect, useState } from "react";
import {
  collectionGroup,
  deleteDoc,
  doc,
  getDocs,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  where,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Reaction types — mirror Android ReactionType (value + emoji). */
const REACTIONS = [
  { type: "love", emoji: "❤️" },
  { type: "fire", emoji: "🔥" },
  { type: "mind_blown", emoji: "🤯" },
  { type: "boring", emoji: "😴" },
] as const;

/**
 * Emoji reaction bar — writes reactions/{uid}/items/{movieId} and tallies counts
 * across users via a collectionGroup("items") query (needs the items(movieId,kind) index).
 * Mirrors Android ReactionRepository.
 */
export function ReactionBar({ movieId, dict }: { movieId: string; dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const [mine, setMine] = useState<string | null>(null);
  const [counts, setCounts] = useState<Record<string, number>>({});

  const loadCounts = useCallback(async () => {
    try {
      const snap = await getDocs(
        query(
          collectionGroup(db, "items"),
          where("movieId", "==", movieId),
          where("kind", "==", "reaction"),
        ),
      );
      const tally: Record<string, number> = {};
      snap.forEach((d) => {
        const t = d.get("type") as string | undefined;
        if (t) tally[t] = (tally[t] ?? 0) + 1;
      });
      setCounts(tally);
    } catch {
      /* index not deployed yet — counts stay empty */
    }
  }, [movieId]);

  useEffect(() => {
    loadCounts();
  }, [loadCounts]);

  useEffect(() => {
    if (!user) return;
    const ref = doc(db, "reactions", user.uid, "items", movieId);
    return onSnapshot(ref, (snap) => setMine(snap.exists() ? (snap.get("type") as string) : null));
  }, [user, movieId]);

  const react = async (type: string) => {
    if (!user) return;
    const ref = doc(db, "reactions", user.uid, "items", movieId);
    if (mine === type) {
      await deleteDoc(ref);
    } else {
      await setDoc(ref, { movieId, type, kind: "reaction", updatedAt: serverTimestamp() });
    }
    await loadCounts();
  };

  return (
    <div className="flex flex-wrap items-center gap-2" aria-label={dict.reactions}>
      {REACTIONS.map(({ type, emoji }) => {
        const active = mine === type;
        const count = counts[type] ?? 0;
        return (
          <button
            key={type}
            type="button"
            onClick={() => react(type)}
            disabled={!user}
            className={
              active
                ? "inline-flex items-center gap-1.5 rounded-full border border-brand-500 bg-brand-500/15 px-3 py-1.5 text-sm font-semibold text-ink"
                : "inline-flex items-center gap-1.5 rounded-full border border-surface-border px-3 py-1.5 text-sm text-ink-muted transition-colors hover:bg-surface-hover disabled:opacity-50"
            }
          >
            <span aria-hidden>{emoji}</span>
            {count > 0 && <span className="tabular-nums">{count}</span>}
          </button>
        );
      })}
    </div>
  );
}
