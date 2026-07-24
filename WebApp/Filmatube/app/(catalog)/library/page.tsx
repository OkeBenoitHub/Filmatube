import Link from "next/link";
import { LibraryBig } from "lucide-react";
import { MovieCard } from "@/components/catalog/MovieCard";
import { ContinueWatchingRow } from "@/components/catalog/ContinueWatchingRow";
import { HScroller } from "@/components/catalog/HScroller";
import { CollectionCard } from "@/components/collections/CollectionCard";
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
        <h1 className="text-4xl font-extrabold tracking-tight text-ink md:text-5xl">{c.libraryTitle}</h1>
      </div>

      <ContinueWatchingRow
        title={c.continueWatching}
        items={continueWatching}
        locale={locale}
        seeAllHref={continueWatching.length > 0 ? "/library/continue" : undefined}
        seeAllLabel={c.seeAll}
      />

      {/* Collections */}
      <section className="space-y-3">
        <div className="flex items-center justify-between gap-3 px-4 md:px-6">
          <h2 className="text-lg font-semibold text-ink">{c.collections}</h2>
          <div className="flex items-center gap-3">
            <NewCollectionButton label={c.newCollection} />
            {collections.length > 0 && (
              <Link href="/collections" className="shrink-0 text-sm font-semibold text-brand-400 transition-colors hover:text-brand-300">
                {c.seeAll}
              </Link>
            )}
          </div>
        </div>
        {collections.length > 0 && (
          <HScroller>
            {collections.slice(0, 12).map((col) => (
              <CollectionCard key={col.id} collection={col} isOwner className="w-44 shrink-0 snap-start" />
            ))}
          </HScroller>
        )}
      </section>

      {/* Watch Later */}
      <section className="space-y-3">
        <div className="flex items-center justify-between gap-3 px-4 md:px-6">
          <h2 className="text-lg font-semibold text-ink">{c.watchLater}</h2>
          {watchlist.length > 0 && (
            <Link href="/library/watchlist" className="shrink-0 text-sm font-semibold text-brand-400 transition-colors hover:text-brand-300">
              {c.seeAll}
            </Link>
          )}
        </div>
        {watchlist.length === 0 ? (
          <p className="px-4 py-8 text-ink-muted md:px-6">{c.libraryEmpty}</p>
        ) : (
          <HScroller>
            {watchlist.slice(0, 18).map((movie) => (
              <MovieCard key={movie.id} movie={movie} locale={locale} className="w-36 shrink-0 snap-start" />
            ))}
          </HScroller>
        )}
      </section>
    </div>
  );
}
