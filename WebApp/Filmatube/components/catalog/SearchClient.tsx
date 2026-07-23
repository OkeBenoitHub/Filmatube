"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Search, X } from "lucide-react";
import { GENRE_KEYS } from "@/lib/genres";
import { Chip } from "@/components/account/Chip";
import type { Dictionary } from "@/lib/i18n/dictionaries";

export interface SearchResult {
  id: string;
  title: string;
  posterUrl: string;
  year: number;
  isComingSoon: boolean;
}

/** Persisted recent search terms — the web stand-in for Android's DataStore recents. */
const RECENT_KEY = "filmatube.recentSearches";
const RECENT_MAX = 8;

function loadRecent(): string[] {
  try {
    const raw = localStorage.getItem(RECENT_KEY);
    return raw ? (JSON.parse(raw) as string[]) : [];
  } catch {
    return [];
  }
}

/**
 * Movie search mirroring the Android screen: a search field, genre chips plus year and rating
 * filters, recent-term chips, and a trending poster grid while idle. Debounced; re-queries
 * whenever the term or any filter changes.
 */
export function SearchClient({
  dict,
  genres,
  trending,
  years,
}: {
  dict: Dictionary["catalog"];
  genres: Dictionary["genres"];
  trending: SearchResult[];
  years: number[];
}) {
  const [query, setQuery] = useState("");
  const [genre, setGenre] = useState("");
  const [year, setYear] = useState("");
  const [minRating, setMinRating] = useState(0);
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [recent, setRecent] = useState<string[]>([]);

  useEffect(() => setRecent(loadRecent()), []);

  const remember = (term: string) => {
    const next = [term, ...recent.filter((t) => t.toLowerCase() !== term.toLowerCase())].slice(0, RECENT_MAX);
    setRecent(next);
    try {
      localStorage.setItem(RECENT_KEY, JSON.stringify(next));
    } catch {
      /* private mode / quota — recents are a convenience, not load-bearing */
    }
  };

  const clearRecent = () => {
    setRecent([]);
    try {
      localStorage.removeItem(RECENT_KEY);
    } catch {
      /* ignore */
    }
  };

  useEffect(() => {
    const term = query.trim();
    if (!term) {
      setResults([]);
      setSearched(false);
      return;
    }
    setLoading(true);
    const handle = setTimeout(async () => {
      try {
        const params = new URLSearchParams({ q: term });
        if (genre) params.set("genre", genre);
        if (year) params.set("year", year);
        if (minRating > 0) params.set("minRating", String(minRating));
        const res = await fetch(`/api/movies/search?${params.toString()}`);
        const data = (await res.json()) as { results?: SearchResult[] };
        setResults(data.results ?? []);
      } catch {
        setResults([]);
      } finally {
        setLoading(false);
        setSearched(true);
      }
    }, 300);
    return () => clearTimeout(handle);
    // Re-run on any filter change, not just the term.
  }, [query, genre, year, minRating]);

  const idle = query.trim() === "";
  const selectClass = "h-9 rounded-lg border border-surface-border bg-surface px-3 text-sm text-ink";

  return (
    <div className="space-y-5">
      <div className="relative max-w-lg">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-faint" aria-hidden />
        <input
          type="search"
          autoFocus
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && query.trim()) remember(query.trim());
          }}
          placeholder={dict.searchHint}
          className="h-11 w-full rounded-lg border border-surface-border bg-surface pl-10 pr-3 text-sm text-ink placeholder:text-ink-faint focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400"
        />
      </div>

      {/* Genre chips — the Android filter row. Horizontal scroll rather than wrap so it reads
          as one filter strip. */}
      <div className="-mx-1 flex gap-2 overflow-x-auto px-1 pb-1">
        <Chip label={dict.all} selected={genre === ""} onClick={() => setGenre("")} />
        {GENRE_KEYS.map((key) => (
          <Chip key={key} label={genres[key]} selected={genre === key} onClick={() => setGenre(genre === key ? "" : key)} />
        ))}
      </div>

      {/* Year + rating, mirroring the Android dropdown filters. */}
      <div className="flex flex-wrap gap-3">
        <select value={year} onChange={(e) => setYear(e.target.value)} className={selectClass} aria-label={dict.allYears}>
          <option value="">{dict.allYears}</option>
          {years.map((y) => (
            <option key={y} value={String(y)}>
              {y}
            </option>
          ))}
        </select>
        <select
          value={String(minRating)}
          onChange={(e) => setMinRating(Number(e.target.value))}
          className={selectClass}
          aria-label={dict.searchRatingAny}
        >
          <option value="0">{dict.searchRatingAny}</option>
          <option value="3">{dict.searchRating3}</option>
          <option value="4">{dict.searchRating4}</option>
        </select>
      </div>

      {idle ? (
        <div className="space-y-6">
          {recent.length > 0 && (
            <section className="space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-medium text-ink-muted">{dict.searchRecent}</h2>
                <button type="button" onClick={clearRecent} className="text-xs text-ink-faint hover:text-ink">
                  {dict.searchClear}
                </button>
              </div>
              <div className="flex flex-wrap gap-2">
                {recent.map((term) => (
                  <button
                    key={term}
                    type="button"
                    onClick={() => setQuery(term)}
                    className="inline-flex items-center gap-1.5 rounded-full border border-surface-border px-3 py-1 text-sm text-ink hover:bg-surface-hover"
                  >
                    {term}
                    <X
                      className="h-3 w-3 text-ink-faint"
                      aria-hidden
                      onClick={(e) => {
                        e.stopPropagation();
                        const next = recent.filter((t) => t !== term);
                        setRecent(next);
                        try {
                          localStorage.setItem(RECENT_KEY, JSON.stringify(next));
                        } catch {
                          /* ignore */
                        }
                      }}
                    />
                  </button>
                ))}
              </div>
            </section>
          )}

          {/* Initial movies while idle — a poster grid, not just text, so the page is useful
              before you type. (Android now shows this too.) */}
          {trending.length > 0 && (
            <section className="space-y-3">
              <h2 className="text-sm font-medium text-ink-muted">{dict.searchTrending}</h2>
              <Grid items={trending} />
            </section>
          )}
        </div>
      ) : loading && results.length === 0 ? (
        <p className="py-10 text-center text-ink-muted">…</p>
      ) : results.length === 0 && searched ? (
        <p className="py-10 text-center text-ink-muted">{dict.noResults}</p>
      ) : (
        <Grid items={results} />
      )}
    </div>
  );
}

function Grid({ items }: { items: SearchResult[] }) {
  return (
    <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
      {items.map((m) => (
        <Link key={m.id} href={`/movie/${m.id}`} className="group block">
          <div className="aspect-[2/3] overflow-hidden rounded-lg border border-surface-border bg-surface-hover">
            {m.posterUrl && (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={m.posterUrl}
                alt=""
                loading="lazy"
                className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
              />
            )}
          </div>
          <p className="mt-1.5 truncate text-sm text-ink">{m.title}</p>
          {m.year > 0 && <p className="text-xs text-ink-faint">{m.year}</p>}
        </Link>
      ))}
    </div>
  );
}
