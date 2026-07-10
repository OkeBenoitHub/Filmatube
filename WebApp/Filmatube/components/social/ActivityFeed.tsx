"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { VolumeX } from "lucide-react";
import { collection, limit, onSnapshot, orderBy, query, Timestamp } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import { UserAvatar } from "@/components/social/UserList";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Feed event types — mirror Android FeedEventTypes. */
type FeedType = "watching" | "watched" | "added_watchlist" | "liked" | "reacted" | "added_collection";

interface FeedEvent {
  id: string;
  actorId: string;
  actorName: string;
  actorAvatar: string;
  type: string;
  movieId: string;
  movieTitle: string;
  createdAtMs: number;
}

type Filter = "today" | "week" | "all";

const MUTED_KEY = "filmatube.mutedActors";

function actionText(type: string, dict: Dictionary["catalog"]): string {
  switch (type as FeedType) {
    case "watching":
      return dict.feedWatching;
    case "watched":
      return dict.feedWatched;
    case "added_watchlist":
      return dict.feedAddedWatchlist;
    case "liked":
      return dict.feedLiked;
    case "reacted":
      return dict.feedReacted;
    case "added_collection":
      return dict.feedAddedCollection;
    default:
      return dict.feedWatched;
  }
}

/** Realtime activity feed reading feed/{uid}/events, with Today/Week/All filters and per-actor mute. */
export function ActivityFeed({ dict }: { dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const [events, setEvents] = useState<FeedEvent[]>([]);
  const [filter, setFilter] = useState<Filter>("all");
  const [muted, setMuted] = useState<string[]>([]);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(MUTED_KEY);
      if (raw) setMuted(JSON.parse(raw));
    } catch {
      /* ignore */
    }
  }, []);

  useEffect(() => {
    if (!user) return;
    const q = query(
      collection(db, "feed", user.uid, "events"),
      orderBy("createdAt", "desc"),
      limit(50),
    );
    return onSnapshot(q, (snap) => {
      setEvents(
        snap.docs.map((d) => {
          const data = d.data();
          const ts = data.createdAt as Timestamp | undefined;
          return {
            id: d.id,
            actorId: data.actorId ?? "",
            actorName: data.actorName ?? "",
            actorAvatar: data.actorAvatar ?? "",
            type: data.type ?? "watched",
            movieId: data.movieId ?? "",
            movieTitle: data.movieTitle ?? "",
            createdAtMs: ts ? ts.toMillis() : 0,
          };
        }),
      );
    });
  }, [user]);

  const toggleMute = (actorId: string) => {
    setMuted((prev) => {
      const next = prev.includes(actorId) ? prev.filter((x) => x !== actorId) : [...prev, actorId];
      try {
        localStorage.setItem(MUTED_KEY, JSON.stringify(next));
      } catch {
        /* ignore */
      }
      return next;
    });
  };

  const visible = useMemo(() => {
    const now = Date.now();
    const cutoff = filter === "today" ? now - 24 * 3600e3 : filter === "week" ? now - 7 * 24 * 3600e3 : 0;
    return events.filter((e) => !muted.includes(e.actorId) && e.createdAtMs >= cutoff);
  }, [events, filter, muted]);

  const chips: { key: Filter; label: string }[] = [
    { key: "today", label: dict.filterToday },
    { key: "week", label: dict.filterWeek },
    { key: "all", label: dict.filterAll },
  ];

  return (
    <div className="mx-auto max-w-2xl px-4 py-6 md:px-6">
      <h1 className="text-2xl font-bold text-ink">{dict.activity}</h1>

      <div className="mt-4 flex gap-2">
        {chips.map(({ key, label }) => (
          <button
            key={key}
            type="button"
            onClick={() => setFilter(key)}
            className={
              filter === key
                ? "rounded-full bg-brand-500 px-4 py-1.5 text-sm font-semibold text-white"
                : "rounded-full border border-surface-border px-4 py-1.5 text-sm text-ink-muted hover:bg-surface-hover"
            }
          >
            {label}
          </button>
        ))}
      </div>

      {visible.length === 0 ? (
        <p className="py-12 text-center text-ink-muted">{dict.feedEmpty}</p>
      ) : (
        <ul className="mt-6 divide-y divide-surface-border">
          {visible.map((e) => (
            <li key={e.id} className="flex items-center gap-3 py-3">
              <Link href={`/u/${e.actorId}`}>
                <UserAvatar name={e.actorName} url={e.actorAvatar} size={40} />
              </Link>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm text-ink-muted">
                  <Link href={`/u/${e.actorId}`} className="font-semibold text-ink hover:underline">
                    {e.actorName}
                  </Link>{" "}
                  {actionText(e.type, dict)}
                </p>
                {e.movieId ? (
                  <Link href={`/movie/${e.movieId}`} className="truncate text-sm font-medium text-ink hover:underline">
                    {e.movieTitle}
                  </Link>
                ) : (
                  <span className="truncate text-sm font-medium text-ink">{e.movieTitle}</span>
                )}
              </div>
              <button
                type="button"
                onClick={() => toggleMute(e.actorId)}
                aria-label={dict.feedMute}
                className="text-ink-faint transition-colors hover:text-ink"
              >
                <VolumeX className="h-4 w-4" aria-hidden />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
