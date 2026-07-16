"use client";

import { useState, type MouseEvent } from "react";
import Link from "next/link";
import { MoreVertical } from "lucide-react";
import { MovieMenu } from "@/components/catalog/MovieMenu";
import { useI18n } from "@/components/providers/LocaleProvider";
import { localized, type CatalogMovie } from "@/lib/catalog";
import type { Locale } from "@/lib/i18n/config";
import { cn } from "@/lib/utils";

/** Poster tile linking to the movie detail page, with a 3-dot / right-click options menu. */
export function MovieCard({
  movie,
  locale,
  className,
}: {
  movie: CatalogMovie;
  locale: Locale;
  className?: string;
}) {
  const { dict } = useI18n();
  const [menuAt, setMenuAt] = useState<{ top: number; left: number } | null>(null);
  const title = localized(movie.title, locale);

  const openAt = (e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setMenuAt({ top: e.clientY, left: e.clientX });
  };

  return (
    <>
      {/* The button is a sibling of the link (never nested inside the anchor). */}
      <div className={cn("group", className)}>
        <div className="relative aspect-[2/3] overflow-hidden rounded-lg border border-surface-border bg-surface-hover">
          <Link href={`/movie/${movie.id}`} onContextMenu={openAt} className="block h-full w-full" aria-label={title}>
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
                {title}
              </div>
            )}
          </Link>

          {movie.isComingSoon && (
            <span className="pointer-events-none absolute left-1.5 top-1.5 rounded bg-surface/90 px-1.5 py-0.5 text-[10px] font-medium text-ink">
              ●
            </span>
          )}

          {/* Always visible on touch; fades in on hover for pointer devices. */}
          <button
            type="button"
            aria-label={dict.catalog.movieOptions}
            onClick={openAt}
            className="absolute right-1.5 top-1.5 z-10 flex h-7 w-7 items-center justify-center rounded-full bg-black/55 text-white opacity-0 transition-opacity hover:bg-black/75 focus-visible:opacity-100 group-hover:opacity-100 max-[768px]:opacity-100"
          >
            <MoreVertical className="h-4 w-4" aria-hidden />
          </button>
        </div>

        <Link href={`/movie/${movie.id}`} className="mt-1.5 block" onContextMenu={openAt}>
          <p className="truncate text-sm text-ink">{title}</p>
          {movie.year > 0 && <p className="text-xs text-ink-faint">{movie.year}</p>}
        </Link>
      </div>

      {menuAt && (
        <MovieMenu
          movie={movie}
          locale={locale}
          position={menuAt}
          dict={dict.catalog}
          onClose={() => setMenuAt(null)}
        />
      )}
    </>
  );
}
