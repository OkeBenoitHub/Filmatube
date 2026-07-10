import Link from "next/link";
import { Globe } from "lucide-react";
import { MovieCard } from "@/components/catalog/MovieCard";
import { ContinueWatchingRow } from "@/components/catalog/ContinueWatchingRow";
import { NewCollectionButton } from "@/components/collections/NewCollectionButton";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict, getLocale } from "@/lib/i18n/server";
import { getWatchlist } from "@/lib/library";
import { getUserCollections } from "@/lib/collections";
import { getContinueWatching } from "@/lib/movies";

export default async function LibraryPage() {
  const user = await getCurrentUser();
  const [locale, dict, watchlist, collections, continueWatching] = await Promise.all([
    getLocale(),
    getDict(),
    user ? getWatchlist(user.uid) : Promise.resolve([]),
    user ? getUserCollections(user.uid) : Promise.resolve([]),
    user ? getContinueWatching(user.uid) : Promise.resolve([]),
  ]);
  const c = dict.catalog;

  return (
    <div className="mx-auto max-w-6xl space-y-8 py-8">
      <h1 className="px-4 text-2xl font-bold text-ink md:px-6">{c.myStuff}</h1>

      <ContinueWatchingRow title={c.continueWatching} items={continueWatching} locale={locale} />

      {/* Collections */}
      <section className="space-y-3 px-4 md:px-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-ink">{c.collections}</h2>
          <div className="flex items-center gap-2">
            <NewCollectionButton label={c.newCollection} />
            <Link href="/collections" className="text-sm text-brand-400 hover:underline">
              {c.browse}
            </Link>
          </div>
        </div>
        {collections.length > 0 && (
          <div className="flex gap-3 overflow-x-auto pb-2">
            {collections.slice(0, 8).map((col) => (
              <Link key={col.id} href={`/collections/${col.id}`} className="w-40 shrink-0">
                <div className="relative aspect-video overflow-hidden rounded-lg border border-surface-border bg-surface-hover">
                  {col.coverUrl && (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={col.coverUrl} alt="" className="h-full w-full object-cover" />
                  )}
                  {col.isPublic && (
                    <span className="absolute right-1.5 top-1.5 rounded bg-black/60 p-1 text-white">
                      <Globe className="h-3 w-3" aria-hidden />
                    </span>
                  )}
                </div>
                <p className="mt-1.5 truncate text-sm text-ink">{col.title}</p>
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* Watch Later */}
      <section className="space-y-3 px-4 md:px-6">
        <h2 className="text-lg font-semibold text-ink">{c.watchLater}</h2>
        {watchlist.length === 0 ? (
          <p className="py-8 text-ink-muted">{c.libraryEmpty}</p>
        ) : (
          <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
            {watchlist.map((movie) => (
              <MovieCard key={movie.id} movie={movie} locale={locale} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
