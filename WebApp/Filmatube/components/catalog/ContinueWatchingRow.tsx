import Link from "next/link";
import { HScroller } from "@/components/catalog/HScroller";
import { ContinueWatchingTile } from "@/components/catalog/ContinueWatchingTile";
import { type ContinueWatchingItem } from "@/lib/catalog";
import type { Locale } from "@/lib/i18n/config";

/** Continue Watching row: posters with a progress bar, linking straight to the player (resume). */
export function ContinueWatchingRow({
  title,
  items,
  locale,
  seeAllHref,
  seeAllLabel,
}: {
  title: string;
  items: ContinueWatchingItem[];
  locale: Locale;
  seeAllHref?: string;
  seeAllLabel?: string;
}) {
  if (items.length === 0) return null;
  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between gap-3 px-4 md:px-6">
        <h2 className="text-lg font-semibold text-ink">{title}</h2>
        {seeAllHref && (
          <Link
            href={seeAllHref}
            className="shrink-0 text-sm font-semibold text-brand-400 transition-colors hover:text-brand-300"
          >
            {seeAllLabel ?? "See all"}
          </Link>
        )}
      </div>
      <HScroller>
        {items.map((item) => (
          <ContinueWatchingTile key={item.movie.id} item={item} locale={locale} className="w-32 shrink-0 snap-start md:w-36" />
        ))}
      </HScroller>
    </section>
  );
}
