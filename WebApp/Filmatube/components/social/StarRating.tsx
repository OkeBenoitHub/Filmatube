"use client";

import { useEffect, useState } from "react";
import { Star } from "lucide-react";
import { collection, deleteDoc, doc, onSnapshot, serverTimestamp, setDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/**
 * 1–5 star rating — writes ratings/{movieId}/items/{uid} and shows a live community
 * average computed from the subtree. Mirrors Android RatingRepository.
 */
export function StarRating({ movieId, dict }: { movieId: string; dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const [mine, setMine] = useState<number | null>(null);
  const [hover, setHover] = useState<number | null>(null);
  const [average, setAverage] = useState(0);
  const [count, setCount] = useState(0);

  useEffect(() => {
    return onSnapshot(collection(db, "ratings", movieId, "items"), (snap) => {
      const values = snap.docs.map((d) => d.get("value") as number).filter((v) => typeof v === "number");
      setCount(values.length);
      setAverage(values.length ? values.reduce((a, b) => a + b, 0) / values.length : 0);
    });
  }, [movieId]);

  useEffect(() => {
    if (!user) return;
    return onSnapshot(doc(db, "ratings", movieId, "items", user.uid), (snap) =>
      setMine(snap.exists() ? (snap.get("value") as number) : null),
    );
  }, [user, movieId]);

  const rate = async (value: number) => {
    if (!user) return;
    const ref = doc(db, "ratings", movieId, "items", user.uid);
    if (mine === value) await deleteDoc(ref);
    else await setDoc(ref, { movieId, userId: user.uid, value, updatedAt: serverTimestamp() });
  };

  const shown = hover ?? mine ?? 0;

  return (
    <div className="flex flex-wrap items-center gap-3">
      <div className="flex items-center gap-1" onMouseLeave={() => setHover(null)}>
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            onClick={() => rate(star)}
            onMouseEnter={() => setHover(star)}
            disabled={!user}
            aria-label={`${dict.rateThis} ${star}`}
            className="p-0.5 disabled:cursor-default"
          >
            <Star
              className={
                star <= shown ? "h-6 w-6 fill-amber-400 text-amber-400" : "h-6 w-6 text-ink-faint"
              }
              aria-hidden
            />
          </button>
        ))}
      </div>
      <span className="text-sm text-ink-muted">
        {mine ? dict.yourRating : dict.rateThis}
        {count > 0 && (
          <span className="ml-2 text-ink-faint">
            ★ {average.toFixed(1)} · {count} {dict.ratingsCount}
          </span>
        )}
      </span>
    </div>
  );
}
