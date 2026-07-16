import Link from "next/link";
import { localized, type ContinueWatchingItem } from "@/lib/catalog";
import type { Locale } from "@/lib/i18n/config";

/** Continue Watching row: posters with a progress bar, linking straight to the player (resume). */
export function ContinueWatchingRow({
  title,
  items,
  locale,
}: {
  title: string;
  items: ContinueWatchingItem[];
  locale: Locale;
}) {
  if (items.length === 0) return null;
  return (
    <section className="space-y-3">
      <h2 className="px-4 text-lg font-semibold text-ink md:px-6">{title}</h2>
      <div className="flex snap-x gap-3 overflow-x-auto px-4 pb-2 md:px-6">
        {items.map(({ movie, progress }) => (
          <Link
            key={movie.id}
            href={`/watch/${movie.id}`}
            className="group block w-32 shrink-0 snap-start md:w-36"
          >
            <div className="relative aspect-[2/3] overflow-hidden rounded-lg border border-surface-border bg-surface-hover">
              {movie.posterUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={movie.posterUrl}
                  alt=""
                  loading="lazy"
                  className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                />
              )}
              <div className="absolute inset-x-0 bottom-0 h-1 bg-black/50">
                <div
                  className="h-full bg-brand-500"
                  style={{ width: `${Math.min(100, Math.max(0, progress * 100))}%` }}
                />
              </div>
            </div>
            <p className="mt-1.5 truncate text-sm text-ink">{localized(movie.title, locale)}</p>
          </Link>
        ))}
      </div>
    </section>
  );
}
