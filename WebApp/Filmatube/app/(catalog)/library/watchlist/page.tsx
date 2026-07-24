import Link from "next/link";
import { ArrowLeft, Bookmark } from "lucide-react";
import { MovieCard } from "@/components/catalog/MovieCard";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict, getLocale } from "@/lib/i18n/server";
import { getWatchlist } from "@/lib/library";

/** The full Watch Later grid — the "See all" target from the Library preview strip. */
export default async function WatchlistPage() {
  const user = await getCurrentUser();
  const [locale, dict, watchlist] = await Promise.all([
    getLocale(),
    getDict(),
    user ? getWatchlist(user.uid) : Promise.resolve([]),
  ]);
  const c = dict.catalog;

  return (
    <div className="mx-auto max-w-6xl space-y-6 px-4 py-8 md:px-6">
      <Link href="/library" className="inline-flex items-center gap-1.5 text-sm text-ink-muted hover:text-ink">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        {c.libraryTitle}
      </Link>
      <div className="flex items-center gap-3">
        <Bookmark className="h-6 w-6 text-brand-400" aria-hidden />
        <h1 className="text-2xl font-bold text-ink">{c.watchLater}</h1>
      </div>

      {watchlist.length === 0 ? (
        <p className="py-16 text-center text-ink-muted">{c.libraryEmpty}</p>
      ) : (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
          {watchlist.map((movie) => (
            <MovieCard key={movie.id} movie={movie} locale={locale} />
          ))}
        </div>
      )}
    </div>
  );
}
