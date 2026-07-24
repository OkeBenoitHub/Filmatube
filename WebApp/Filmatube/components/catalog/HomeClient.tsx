"use client";

import { useEffect, useMemo, useState } from "react";
import {
  collection,
  doc,
  limit,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  where,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/components/providers/AuthProvider";
import { Hero } from "@/components/catalog/Hero";
import { MovieRow } from "@/components/catalog/MovieRow";
import { ContinueWatchingRow } from "@/components/catalog/ContinueWatchingRow";
import { Skeleton } from "@/components/ui/Skeleton";
import {
  mapMovieDoc,
  pickByGenre,
  pickComingSoon,
  pickFeatured,
  pickNewReleases,
  pickTrending,
  type CatalogMovie,
  type ContinueWatchingItem,
} from "@/lib/catalog";
import type { Locale } from "@/lib/i18n/config";
import type { Dictionary } from "@/lib/i18n/dictionaries";

interface ProgressEntry {
  movieId: string;
  progress: number;
  completed: boolean;
}

/**
 * Client-side Home: subscribes to the catalog, the viewer's taste profile and their watch
 * progress with the client SDK. With IndexedDB persistence on, every snapshot below resolves
 * from local disk first (instant paint) and then live-updates from the server — the SPA data
 * model, while the page shell stays server-rendered for auth gating and i18n.
 */
export function HomeClient({
  dict,
  genresDict,
  locale,
}: {
  dict: Dictionary["catalog"];
  genresDict: Dictionary["genres"];
  locale: Locale;
}) {
  const { user, loading: authLoading } = useAuth();
  const [movies, setMovies] = useState<CatalogMovie[] | null>(null);
  const [genrePrefs, setGenrePrefs] = useState<string[]>([]);
  const [progress, setProgress] = useState<ProgressEntry[]>([]);
  const [recRows, setRecRows] = useState<{ seedTitle: string; movieIds: string[] }[]>([]);
  const [topPickIds, setTopPickIds] = useState<string[]>([]);
  const [followFeedIds, setFollowFeedIds] = useState<string[]>([]);
  const [dismissed, setDismissed] = useState<Set<string>>(new Set());

  // Catalog — waits for client auth (rules require a signed-in reader).
  useEffect(() => {
    if (!user) return;
    const q = query(collection(db, "movies"), where("status", "==", "published"), limit(500));
    return onSnapshot(q, { includeMetadataChanges: false }, (snap) => {
      setMovies(
        snap.docs
          .map((d) => mapMovieDoc(d.id, d.data()))
          .sort((a, b) => b.addedAtMs - a.addedAtMs),
      );
    });
  }, [user]);

  // Taste profile → personalised genre rows.
  useEffect(() => {
    if (!user) return;
    return onSnapshot(doc(db, "users", user.uid), (snap) => {
      setGenrePrefs(((snap.get("genrePreferences") as string[]) ?? []).slice(0, 4));
    });
  }, [user]);

  // Recommendations → "Because you watched X" rows, from the nightly rec doc (Day 183). Read
  // only; the client never writes recs. Absent until the function has built them for this user.
  useEffect(() => {
    if (!user) return;
    return onSnapshot(doc(db, "recs", user.uid), (snap) => {
      const rows = (snap.get("rows") as { seedTitle?: string; movieIds?: string[] }[]) ?? [];
      setRecRows(
        rows
          .map((r) => ({ seedTitle: r.seedTitle ?? "", movieIds: r.movieIds ?? [] }))
          .filter((r) => r.movieIds.length > 0),
      );
      setTopPickIds((snap.get("topPicks") as string[]) ?? []);
    });
  }, [user]);

  // "From people you follow" — the viewer's feed is fanned out from the accounts they follow, so
  // aggregating its recent events by movie surfaces what that circle is watching, most-mentioned
  // first. Social, live, and needs no extra reads beyond the feed we already have rules for.
  useEffect(() => {
    if (!user) return;
    const q = query(
      collection(db, "feed", user.uid, "events"),
      orderBy("createdAt", "desc"),
      limit(100),
    );
    return onSnapshot(q, (snap) => {
      const counts = new Map<string, number>();
      for (const d of snap.docs) {
        const id = d.get("movieId") as string | undefined;
        if (id) counts.set(id, (counts.get(id) ?? 0) + 1);
      }
      setFollowFeedIds([...counts.entries()].sort((a, b) => b[1] - a[1]).map(([id]) => id));
    });
  }, [user]);

  // Watch progress → Continue Watching (live: finish a movie on Android, it drops off here).
  useEffect(() => {
    if (!user) return;
    const q = query(
      collection(db, "watchProgress", user.uid, "items"),
      orderBy("updatedAt", "desc"),
      limit(20),
    );
    return onSnapshot(q, (snap) => {
      setProgress(
        snap.docs.map((d) => ({
          movieId: (d.get("movieId") as string) ?? d.id,
          progress: Number(d.get("progress") ?? 0),
          completed: d.get("completed") === true,
        })),
      );
    });
  }, [user]);

  const continueWatching: ContinueWatchingItem[] = useMemo(() => {
    if (!movies) return [];
    const byId = new Map(movies.map((m) => [m.id, m]));
    return progress
      .filter((e) => !e.completed)
      .slice(0, 12)
      .map((e) => {
        const movie = byId.get(e.movieId);
        return movie ? { movie, progress: e.progress } : null;
      })
      .filter((x): x is ContinueWatchingItem => x !== null);
  }, [movies, progress]);

  // "Not interested": hide the title from every rec rail at once, then persist the dismissal so
  // the next nightly build excludes it. Local hiding is tracked in its own set rather than by
  // mutating the rec state, so a re-fire of the recs snapshot can't bring the poster back.
  // Mirrors the Android option-sheet action (Day 188); write shape matches RecsRepository.dismiss.
  const notInterested = (movieId: string) => {
    setDismissed((prev) => new Set(prev).add(movieId));
    if (!user) return;
    void setDoc(doc(db, "recFeedback", user.uid, "items", movieId), {
      action: "dismissed",
      createdAt: serverTimestamp(),
    });
  };

  // First-ever visit (nothing on disk yet): skeleton. Every visit after paints instantly.
  if (movies === null) {
    if (!authLoading && !user) {
      // Session cookie exists but the client SDK isn't signed in — data can't load.
      return <p className="px-4 py-24 text-center text-ink-muted md:px-6">{dict.homeSignInAgain}</p>;
    }
    return (
      <div>
        <Skeleton className="h-[420px] w-full rounded-none" />
        <div className="mx-auto max-w-6xl space-y-8 py-8">
          {[0, 1].map((i) => (
            <div key={i} className="space-y-3 px-4 md:px-6">
              <Skeleton className="h-5 w-40" />
              <div className="flex gap-3 overflow-hidden">
                {[0, 1, 2, 3, 4, 5].map((j) => (
                  <Skeleton key={j} className="aspect-[2/3] w-36 shrink-0 rounded-xl" />
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (movies.length === 0) {
    return <p className="px-4 py-24 text-center text-ink-muted md:px-6">{dict.empty}</p>;
  }

  const featured = pickFeatured(movies);
  const genreRows = genrePrefs
    .map((key) => ({ key, movies: pickByGenre(movies, key) }))
    .filter((row) => row.movies.length > 0);

  // Resolve ids against the loaded catalogue, keeping the ranking, dropping any title
  // unpublished since the nightly build, and dropping anything dismissed this session.
  const byId = new Map(movies.map((m) => [m.id, m]));
  const resolve = (ids: string[]) =>
    ids.map((id) => byId.get(id)).filter((m): m is CatalogMovie => !!m && !dismissed.has(m.id));

  // Rows under 3 aren't worth a rail.
  const becauseYouWatched = recRows
    .map((row) => ({ seedTitle: row.seedTitle, movies: resolve(row.movieIds) }))
    .filter((row) => row.movies.length >= 3);

  // Top picks — the rec doc's overall ranking for this viewer.
  const topPicks = resolve(topPickIds);
  // What the people you follow are watching, minus anything you've already finished.
  const finished = new Set(progress.filter((e) => e.completed).map((e) => e.movieId));
  const fromPeopleYouFollow = resolve(followFeedIds).filter((m) => !finished.has(m.id));

  return (
    <div>
      <Hero movies={featured.length > 0 ? featured : movies.slice(0, 1)} locale={locale} dict={dict} />
      <div className="mx-auto max-w-6xl space-y-8 py-8">
        {/* Continue Watching gets no "See all" — it isn't a browsable slice, matching Android.
            Every other row's target mirrors the Android home rows exactly. */}
        <ContinueWatchingRow title={dict.continueWatching} items={continueWatching} locale={locale} />
        {/* Personalised rails, high on the page — most relevant thing on screen. Mirrors Android.
            Each is absent until it has enough to show, so a new account still gets a full page. */}
        {/* "Not interested" is offered only on the rec-doc rows, the ones recFeedback actually
            tunes — matching the Android option sheet. */}
        {topPicks.length >= 3 && (
          <MovieRow title={dict.topPicks} movies={topPicks} locale={locale} onNotInterested={notInterested} />
        )}
        {fromPeopleYouFollow.length >= 3 && (
          <MovieRow title={dict.fromPeopleYouFollow} movies={fromPeopleYouFollow} locale={locale} />
        )}
        {becauseYouWatched.map((row) => (
          <MovieRow
            key={row.seedTitle}
            title={dict.becauseYouWatched.replace("{title}", row.seedTitle)}
            movies={row.movies}
            locale={locale}
            onNotInterested={notInterested}
          />
        ))}
        <MovieRow
          title={dict.trending}
          movies={pickTrending(movies)}
          locale={locale}
          seeAllHref="/browse?sort=popular"
          seeAllLabel={dict.seeAll}
        />
        <MovieRow
          title={dict.newReleases}
          movies={pickNewReleases(movies)}
          locale={locale}
          seeAllHref="/browse"
          seeAllLabel={dict.seeAll}
        />
        {genreRows.map((row) => (
          <MovieRow
            key={row.key}
            title={(genresDict as Record<string, string>)[row.key] ?? row.key}
            movies={row.movies}
            locale={locale}
            seeAllHref={`/browse?genre=${row.key}`}
            seeAllLabel={dict.seeAll}
          />
        ))}
        <MovieRow
          title={dict.comingSoon}
          movies={pickComingSoon(movies)}
          locale={locale}
          seeAllHref="/browse?comingSoon=1"
          seeAllLabel={dict.seeAll}
        />
      </div>
    </div>
  );
}
