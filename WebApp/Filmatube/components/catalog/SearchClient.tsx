"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Search } from "lucide-react";
import type { Dictionary } from "@/lib/i18n/dictionaries";

export interface SearchResult {
  id: string;
  title: string;
  posterUrl: string;
  year: number;
  isComingSoon: boolean;
}

/** Debounced movie search hitting /api/movies/search; shows trending when idle. */
export function SearchClient({
  dict,
  trending,
}: {
  dict: Dictionary["catalog"];
  trending: SearchResult[];
}) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

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
        const res = await fetch(`/api/movies/search?q=${encodeURIComponent(term)}`);
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
  }, [query]);

  const showTrending = query.trim() === "";

  return (
    <div className="space-y-6">
      <div className="relative max-w-lg">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-faint" aria-hidden />
        <input
          type="search"
          autoFocus
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder={dict.searchHint}
          className="h-11 w-full rounded-lg border border-surface-border bg-surface pl-10 pr-3 text-sm text-ink placeholder:text-ink-faint focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400"
        />
      </div>

      {showTrending ? (
        trending.length > 0 && (
          <section className="space-y-3">
            <h2 className="text-sm font-medium text-ink-muted">{dict.searchTrending}</h2>
            <Grid items={trending} />
          </section>
        )
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
