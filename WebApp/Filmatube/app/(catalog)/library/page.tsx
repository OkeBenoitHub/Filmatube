import Link from "next/link";
import { Layers } from "lucide-react";
import { MovieCard } from "@/components/catalog/MovieCard";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict, getLocale } from "@/lib/i18n/server";
import { getWatchlist } from "@/lib/library";

export default async function LibraryPage() {
  const user = await getCurrentUser();
  const [locale, dict, movies] = await Promise.all([
    getLocale(),
    getDict(),
    user ? getWatchlist(user.uid) : Promise.resolve([]),
  ]);
  const c = dict.catalog;

  return (
    <div className="mx-auto max-w-6xl space-y-6 px-4 py-8 md:px-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-ink">{c.library}</h1>
        <Link
          href="/collections"
          className="inline-flex h-9 items-center gap-2 rounded-lg border border-surface-border px-4 text-sm font-medium text-ink hover:bg-surface-hover"
        >
          <Layers className="h-4 w-4" aria-hidden />
          {c.collections}
        </Link>
      </div>

      <h2 className="text-lg font-semibold text-ink">{c.watchLater}</h2>
      {movies.length === 0 ? (
        <p className="py-16 text-center text-ink-muted">{c.libraryEmpty}</p>
      ) : (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
          {movies.map((movie) => (
            <MovieCard key={movie.id} movie={movie} locale={locale} />
          ))}
        </div>
      )}
    </div>
  );
}
