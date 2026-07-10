"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { collection, limit, onSnapshot, orderBy, query } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import { UserAvatar } from "@/components/social/UserList";
import type { Dictionary } from "@/lib/i18n/dictionaries";

interface Recommendation {
  id: string;
  fromName: string;
  fromAvatar: string;
  movieId: string;
  movieTitle: string;
  message: string;
}

/** Realtime list of movies recommended to the current user (recommendations/{uid}/items). */
export function RecommendationInbox({ dict }: { dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const [items, setItems] = useState<Recommendation[]>([]);

  useEffect(() => {
    if (!user) return;
    const q = query(
      collection(db, "recommendations", user.uid, "items"),
      orderBy("createdAt", "desc"),
      limit(50),
    );
    return onSnapshot(q, (snap) => {
      setItems(
        snap.docs.map((d) => ({
          id: d.id,
          fromName: d.get("fromName") ?? "",
          fromAvatar: d.get("fromAvatar") ?? "",
          movieId: d.get("movieId") ?? "",
          movieTitle: d.get("movieTitle") ?? "",
          message: d.get("message") ?? "",
        })),
      );
    });
  }, [user]);

  return (
    <div className="mx-auto max-w-2xl px-4 py-6 md:px-6">
      <h1 className="text-2xl font-bold text-ink">{dict.inbox}</h1>

      {items.length === 0 ? (
        <p className="py-12 text-center text-ink-muted">{dict.inboxEmpty}</p>
      ) : (
        <ul className="mt-6 space-y-3">
          {items.map((r) => (
            <li key={r.id} className="flex gap-3 rounded-xl border border-surface-border p-3">
              <UserAvatar name={r.fromName} url={r.fromAvatar} size={44} />
              <div className="min-w-0 flex-1">
                <p className="text-sm text-ink-muted">
                  <span className="font-semibold text-ink">{r.fromName}</span> {dict.recommendedYou}
                </p>
                {r.movieId ? (
                  <Link href={`/movie/${r.movieId}`} className="text-sm font-semibold text-ink hover:underline">
                    {r.movieTitle}
                  </Link>
                ) : (
                  <span className="text-sm font-semibold text-ink">{r.movieTitle}</span>
                )}
                {r.message && <p className="mt-1 text-sm text-ink-muted">“{r.message}”</p>}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
