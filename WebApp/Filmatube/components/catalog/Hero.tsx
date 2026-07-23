"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Info } from "lucide-react";
import { localized, type CatalogMovie } from "@/lib/catalog";
import type { Locale } from "@/lib/i18n/config";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

/** How long each featured movie holds before the next slides in — matches Android (5s). */
const ROTATE_MS = 5000;

/**
 * Featured banner at the top of the catalog home.
 *
 * Rotates through every featured movie the way the Android home hero does, rather than
 * showing a single static one. Slides horizontally on a translated track; auto-advances on a
 * timer that pauses on hover/focus and stands down entirely for reduced-motion users, who
 * get a static first slide plus the dots to move themselves.
 */
export function Hero({
  movies,
  locale,
  dict,
}: {
  movies: CatalogMovie[];
  locale: Locale;
  dict: Dictionary["catalog"];
}) {
  const [index, setIndex] = useState(0);
  const paused = useRef(false);

  const count = movies.length;

  useEffect(() => {
    if (count <= 1) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const id = setInterval(() => {
      if (!paused.current) setIndex((i) => (i + 1) % count);
    }, ROTATE_MS);
    return () => clearInterval(id);
  }, [count]);

  if (count === 0) return null;

  return (
    <section
      className="relative h-[56vh] min-h-[340px] w-full overflow-hidden"
      onMouseEnter={() => (paused.current = true)}
      onMouseLeave={() => (paused.current = false)}
      onFocusCapture={() => (paused.current = true)}
      onBlurCapture={() => (paused.current = false)}
      aria-roledescription="carousel"
    >
      {/* One track of full-width slides, shifted by the active index. */}
      <div
        className="flex h-full transition-transform duration-700 ease-out"
        style={{ transform: `translateX(-${index * 100}%)` }}
      >
        {movies.map((movie, i) => (
          <HeroSlide
            key={movie.id}
            movie={movie}
            locale={locale}
            dict={dict}
            // Only the visible slide is in the tab order; the rest are off-screen.
            active={i === index}
          />
        ))}
      </div>

      {count > 1 && (
        // Centered at the bottom, matching the Android pager's dot row.
        <div className="absolute inset-x-0 bottom-4 z-10 flex justify-center gap-1.5">
          {movies.map((movie, i) => (
            <button
              key={movie.id}
              type="button"
              onClick={() => setIndex(i)}
              aria-label={`${localized(movie.title, locale)}`}
              aria-current={i === index}
              className={cn(
                "rounded-full transition-all",
                i === index ? "h-2 w-2 bg-brand-400" : "h-1.5 w-1.5 bg-white/40 hover:bg-white/70",
              )}
            />
          ))}
        </div>
      )}
    </section>
  );
}

function HeroSlide({
  movie,
  locale,
  dict,
  active,
}: {
  movie: CatalogMovie;
  locale: Locale;
  dict: Dictionary["catalog"];
  active: boolean;
}) {
  return (
    // The whole slide is the tap target → detail, exactly as the Android hero slide is one
    // clickable surface. "More info" below is a visual affordance inside the same link, not a
    // nested anchor, and the meta line mirrors Android: year • age • ★ rating, no runtime.
    <Link
      href={`/movie/${movie.id}`}
      tabIndex={active ? 0 : -1}
      className="group/slide relative block h-full w-full shrink-0"
      aria-hidden={!active}
    >
      {movie.backdropUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={movie.backdropUrl} alt="" className="absolute inset-0 h-full w-full object-cover" />
      )}
      <div className="absolute inset-0 bg-gradient-to-t from-surface via-surface/70 to-surface/10" />
      <div className="absolute inset-0 bg-gradient-to-r from-surface/80 to-transparent" />
      <div className="relative flex h-full max-w-6xl flex-col justify-end gap-3 px-4 pb-12 md:mx-auto md:px-6 md:pb-16">
        <h1 className="max-w-xl text-3xl font-extrabold text-ink md:text-5xl">{localized(movie.title, locale)}</h1>
        <div className="flex flex-wrap items-center gap-2 text-sm text-white/85">
          {movie.year > 0 && <span>{movie.year}</span>}
          {movie.ageRating && <span>• {movie.ageRating}</span>}
          {movie.averageRating > 0 && <span>• ★ {movie.averageRating.toFixed(1)}</span>}
        </div>
        <p className="max-w-xl text-sm text-ink-muted line-clamp-2 md:text-base">
          {localized(movie.description, locale)}
        </p>
        <span className="mt-1 inline-flex h-11 w-fit items-center gap-2 rounded-lg bg-brand-500 px-6 text-sm font-semibold text-white transition-colors group-hover/slide:bg-brand-600">
          <Info className="h-4 w-4" aria-hidden />
          {dict.details}
        </span>
      </div>
    </Link>
  );
}
