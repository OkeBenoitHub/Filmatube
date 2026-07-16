"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { Film, Rss, UserPlus, VolumeX } from "lucide-react";
import { collection, doc, getDoc, limit, onSnapshot, orderBy, query, Timestamp } from "firebase/firestore";
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

/**
 * Activity page — Spotitube-style layout in Filmatube green: hero header (gradient icon
 * tile + eyebrow + huge title), "Trending in your circle" horizontal row, then the
 * realtime feed with Today/Week/All filters and per-actor mute.
 */
export function ActivityFeed({ dict }: { dict: Dictionary["catalog"] }) {
  const { user } = useAuth();
  const [events, setEvents] = useState<FeedEvent[]>([]);
  const [filter, setFilter] = useState<Filter>("all");
  const [muted, setMuted] = useState<string[]>([]);
  const [posters, setPosters] = useState<Record<string, string>>({});

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

  /** Top movies among your circle's (non-muted) events, most-mentioned first. */
  const trending = useMemo(() => {
    const counts = new Map<string, { movieId: string; movieTitle: string; count: number }>();
    for (const e of events) {
      if (!e.movieId || muted.includes(e.actorId)) continue;
      const cur = counts.get(e.movieId);
      if (cur) cur.count += 1;
      else counts.set(e.movieId, { movieId: e.movieId, movieTitle: e.movieTitle, count: 1 });
    }
    return [...counts.values()].sort((a, b) => b.count - a.count).slice(0, 10);
  }, [events, muted]);

  // Fetch posters for the trending row (once per movie; SDK caches repeats).
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const missing = trending.filter((t) => posters[t.movieId] === undefined);
      if (missing.length === 0) return;
      const fetched: Record<string, string> = {};
      await Promise.all(
        missing.map(async (t) => {
          try {
            const snap = await getDoc(doc(db, "movies", t.movieId));
            fetched[t.movieId] = (snap.get("posterUrl") as string) ?? "";
          } catch {
            fetched[t.movieId] = "";
          }
        }),
      );
      if (!cancelled) setPosters((prev) => ({ ...prev, ...fetched }));
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trending]);

  const chips: { key: Filter; label: string }[] = [
    { key: "today", label: dict.filterToday },
    { key: "week", label: dict.filterWeek },
    { key: "all", label: dict.filterAll },
  ];

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 md:px-6">
      {/* ── Hero header (Spotitube pattern, green) ─────────────── */}
      <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-end">
        <div className="flex h-36 w-36 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-900 shadow-2xl shadow-brand-900/50 sm:h-48 sm:w-48">
          <Rss className="h-16 w-16 text-white sm:h-20 sm:w-20" aria-hidden />
        </div>
        <div className="text-center sm:text-left">
          <p className="text-xs font-bold uppercase tracking-widest text-ink-muted">{dict.activityEyebrow}</p>
          <h1 className="mt-1 text-4xl font-black leading-none tracking-tight text-ink md:text-6xl">
            {dict.activity}
          </h1>
          <p className="mt-2 text-sm text-ink-muted">{dict.activitySubtitle}</p>
          <div className="mt-4 flex justify-center sm:justify-start">
            <Link
              href="/discover"
              className="inline-flex items-center gap-1.5 rounded-full border border-surface-border px-4 py-2 text-sm font-semibold text-ink transition-colors hover:border-brand-500 hover:text-brand-400"
            >
              <UserPlus className="h-4 w-4" aria-hidden />
              {dict.discoverPeople}
            </Link>
          </div>
        </div>
      </div>

      {/* ── Trending in your circle ────────────────────────────── */}
      {trending.length > 0 && (
        <section className="mt-12">
          <h2 className="text-lg font-bold text-ink">{dict.trendingCircle}</h2>
          <div className="mt-3 flex gap-3 overflow-x-auto pb-2">
            {trending.map((t) => (
              <Link key={t.movieId} href={`/movie/${t.movieId}`} className="w-32 shrink-0">
                <div className="flex aspect-[2/3] items-center justify-center overflow-hidden rounded-xl border border-surface-border bg-gradient-to-b from-surface-hover to-surface-card transition-colors hover:border-brand-700/60">
                  {posters[t.movieId] ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={posters[t.movieId]} alt="" className="h-full w-full object-cover" />
                  ) : (
                    <Film className="h-6 w-6 text-ink-faint/50" aria-hidden />
                  )}
                </div>
                <p className="mt-1.5 truncate text-sm text-ink">{t.movieTitle}</p>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* ── Recent activity ────────────────────────────────────── */}
      <section className="mt-12">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-lg font-bold text-ink">{dict.recentActivity}</h2>
          <div className="flex gap-2">
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
        </div>

        {visible.length === 0 ? (
          <div className="py-12 text-center">
            <p className="text-ink-muted">{dict.feedEmpty}</p>
            <Link
              href="/discover"
              className="mt-4 inline-flex items-center gap-1.5 rounded-lg bg-brand-500 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brand-600"
            >
              <UserPlus className="h-4 w-4" aria-hidden />
              {dict.discoverPeople}
            </Link>
          </div>
        ) : (
          <ul className="mt-4 divide-y divide-surface-border">
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
      </section>
    </div>
  );
}
