import Link from "next/link";
import { ArrowLeft, PlayCircle } from "lucide-react";
import { ContinueWatchingTile } from "@/components/catalog/ContinueWatchingTile";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict, getLocale } from "@/lib/i18n/server";
import { getContinueWatching } from "@/lib/movies";

/** The full Continue Watching grid — the "See all" target from the Library preview strip. */
export default async function ContinuePage() {
  const user = await getCurrentUser();
  const [locale, dict, items] = await Promise.all([
    getLocale(),
    getDict(),
    user ? getContinueWatching(user.uid, 60) : Promise.resolve([]),
  ]);
  const c = dict.catalog;

  return (
    <div className="mx-auto max-w-6xl space-y-6 px-4 py-8 md:px-6">
      <Link href="/library" className="inline-flex items-center gap-1.5 text-sm text-ink-muted hover:text-ink">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        {c.libraryTitle}
      </Link>
      <div className="flex items-center gap-3">
        <PlayCircle className="h-6 w-6 text-brand-400" aria-hidden />
        <h1 className="text-2xl font-bold text-ink">{c.continueWatching}</h1>
      </div>

      {items.length === 0 ? (
        <p className="py-16 text-center text-ink-muted">{c.libraryEmpty}</p>
      ) : (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
          {items.map((item) => (
            <ContinueWatchingTile key={item.movie.id} item={item} locale={locale} />
          ))}
        </div>
      )}
    </div>
  );
}
