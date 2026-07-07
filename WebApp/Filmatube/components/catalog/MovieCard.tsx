import Link from "next/link";
import { localized, type CatalogMovie } from "@/lib/movies";
import type { Locale } from "@/lib/i18n/config";
import { cn } from "@/lib/utils";

/** Poster tile linking to the movie detail page. */
export function MovieCard({
  movie,
  locale,
  className,
}: {
  movie: CatalogMovie;
  locale: Locale;
  className?: string;
}) {
  return (
    <Link href={`/movie/${movie.id}`} className={cn("group block", className)}>
      <div className="relative aspect-[2/3] overflow-hidden rounded-lg border border-surface-border bg-surface-hover">
        {movie.posterUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={movie.posterUrl}
            alt=""
            loading="lazy"
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full items-center justify-center p-2 text-center text-xs text-ink-faint">
            {localized(movie.title, locale)}
          </div>
        )}
        {movie.isComingSoon && (
          <span className="absolute left-1.5 top-1.5 rounded bg-surface/90 px-1.5 py-0.5 text-[10px] font-medium text-ink">
            ●
          </span>
        )}
      </div>
      <p className="mt-1.5 truncate text-sm text-ink">{localized(movie.title, locale)}</p>
      {movie.year > 0 && <p className="text-xs text-ink-faint">{movie.year}</p>}
    </Link>
  );
}
