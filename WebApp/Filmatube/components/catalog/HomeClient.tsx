"use client";

import { useEffect, useMemo, useState } from "react";
import {
  collection,
  doc,
  limit,
  onSnapshot,
  orderBy,
  query,
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
  const hero = featured[0] ?? movies[0];
  const genreRows = genrePrefs
    .map((key) => ({ key, movies: pickByGenre(movies, key) }))
    .filter((row) => row.movies.length > 0);

  return (
    <div>
      <Hero movie={hero} locale={locale} dict={dict} />
      <div className="mx-auto max-w-6xl space-y-8 py-8">
        <ContinueWatchingRow title={dict.continueWatching} items={continueWatching} locale={locale} />
        <MovieRow title={dict.trending} movies={pickTrending(movies)} locale={locale} />
        <MovieRow title={dict.newReleases} movies={pickNewReleases(movies)} locale={locale} />
        {genreRows.map((row) => (
          <MovieRow
            key={row.key}
            title={(genresDict as Record<string, string>)[row.key] ?? row.key}
            movies={row.movies}
            locale={locale}
          />
        ))}
        <MovieRow title={dict.comingSoon} movies={pickComingSoon(movies)} locale={locale} />
      </div>
    </div>
  );
}
