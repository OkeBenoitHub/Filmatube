import Link from "next/link";
import { Globe, LibraryBig } from "lucide-react";
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
    <div className="mx-auto max-w-6xl space-y-10 py-8">
      {/* Large left-aligned title with brand icon tile */}
      <div className="flex items-center gap-4 px-4 md:px-6">
        <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-700 shadow-lg shadow-brand-900/40">
          <LibraryBig className="h-7 w-7 text-white" aria-hidden />
        </div>
        <h1 className="text-4xl font-extrabold tracking-tight text-ink md:text-5xl">{c.myStuff}</h1>
      </div>

      <ContinueWatchingRow title={c.continueWatching} items={continueWatching} locale={locale} />

      {/* Collections */}
      <section className="space-y-3">
        <div className="flex items-center justify-between px-4 md:px-6">
          <h2 className="text-lg font-semibold text-ink">{c.collections}</h2>
          <div className="flex items-center gap-2">
            <NewCollectionButton label={c.newCollection} />
            <Link href="/collections" className="text-sm text-brand-400 hover:underline">
              {c.browse}
            </Link>
          </div>
        </div>
        {collections.length > 0 && (
          <div className="flex gap-3 overflow-x-auto px-4 pb-2 md:px-6">
            {collections.slice(0, 12).map((col) => (
              <Link key={col.id} href={`/collections/${col.id}`} className="w-44 shrink-0">
                <div className="relative aspect-video overflow-hidden rounded-xl border border-surface-border bg-surface-hover transition-colors hover:border-brand-700/60">
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
      <section className="space-y-3">
        <h2 className="px-4 text-lg font-semibold text-ink md:px-6">{c.watchLater}</h2>
        {watchlist.length === 0 ? (
          <p className="px-4 py-8 text-ink-muted md:px-6">{c.libraryEmpty}</p>
        ) : (
          <div className="flex gap-3 overflow-x-auto px-4 pb-2 md:px-6">
            {watchlist.map((movie) => (
              <div key={movie.id} className="w-36 shrink-0">
                <MovieCard movie={movie} locale={locale} />
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
