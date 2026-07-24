import Link from "next/link";
import { MovieCard } from "@/components/catalog/MovieCard";
import { HScroller } from "@/components/catalog/HScroller";
import type { CatalogMovie } from "@/lib/catalog";
import type { Locale } from "@/lib/i18n/config";

/**
 * A titled row of poster tiles that scrolls horizontally, with left/right arrows (via
 * [HScroller]) and an optional green "See all" link matching the Android home rows.
 */
export function MovieRow({
  title,
  movies,
  locale,
  seeAllHref,
  seeAllLabel,
  onNotInterested,
}: {
  title: string;
  movies: CatalogMovie[];
  locale: Locale;
  seeAllHref?: string;
  seeAllLabel?: string;
  /** Passed only by recommended rows; each card's menu then offers "Not interested". */
  onNotInterested?: (movieId: string) => void;
}) {
  if (movies.length === 0) return null;

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between gap-3 px-4 md:px-6">
        <h2 className="text-lg font-semibold text-ink">{title}</h2>
        {seeAllHref && (
          // brand-400 is the green the Android "See all" TextButton uses (primary in the
          // forced-dark theme) — kept identical so the two homes read as one product.
          <Link
            href={seeAllHref}
            className="shrink-0 text-sm font-semibold text-brand-400 transition-colors hover:text-brand-300"
          >
            {seeAllLabel ?? "See all"}
          </Link>
        )}
      </div>

      <HScroller>
        {movies.map((movie) => (
          <MovieCard
            key={movie.id}
            movie={movie}
            locale={locale}
            className="w-32 shrink-0 snap-start md:w-36"
            onNotInterested={onNotInterested ? () => onNotInterested(movie.id) : undefined}
          />
        ))}
      </HScroller>
    </section>
  );
}
