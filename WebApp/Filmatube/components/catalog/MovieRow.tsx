"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { MovieCard } from "@/components/catalog/MovieCard";
import type { CatalogMovie } from "@/lib/catalog";
import type { Locale } from "@/lib/i18n/config";
import { cn } from "@/lib/utils";

/**
 * Horizontally scrolling row of poster tiles under a section title.
 *
 * Client-side because of the scroll arrows and the "See all" link: [seeAllHref], when given,
 * renders the same green link the Android home rows use, pointing at the browse slice that
 * continues this row. The arrows page the strip by roughly one screen and hide themselves at
 * each end, and stay out of the way of touch/trackpad scrolling — they're an addition, not
 * the only way to move.
 */
export function MovieRow({
  title,
  movies,
  locale,
  seeAllHref,
  seeAllLabel,
}: {
  title: string;
  movies: CatalogMovie[];
  locale: Locale;
  seeAllHref?: string;
  seeAllLabel?: string;
}) {
  const scroller = useRef<HTMLDivElement>(null);
  const [atStart, setAtStart] = useState(true);
  const [atEnd, setAtEnd] = useState(false);

  const sync = useCallback(() => {
    const el = scroller.current;
    if (!el) return;
    setAtStart(el.scrollLeft <= 1);
    // 1px of slack absorbs sub-pixel widths that would otherwise never report "fully scrolled".
    setAtEnd(el.scrollLeft + el.clientWidth >= el.scrollWidth - 1);
  }, []);

  useEffect(() => {
    sync();
    const el = scroller.current;
    if (!el) return;
    // The end position depends on width, so recompute when the viewport changes, not just on scroll.
    window.addEventListener("resize", sync);
    return () => window.removeEventListener("resize", sync);
  }, [sync, movies.length]);

  const page = (dir: -1 | 1) => {
    const el = scroller.current;
    if (!el) return;
    el.scrollBy({ left: dir * el.clientWidth * 0.9, behavior: "smooth" });
  };

  if (movies.length === 0) return null;

  return (
    <section className="group/row space-y-3">
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

      <div className="relative">
        {/* Arrows: desktop only (touch devices scroll directly), and each hides at its end. */}
        <RowArrow side="left" hidden={atStart} onClick={() => page(-1)} />
        <RowArrow side="right" hidden={atEnd} onClick={() => page(1)} />

        <div
          ref={scroller}
          onScroll={sync}
          className="flex snap-x gap-3 overflow-x-auto scroll-smooth px-4 pb-2 md:px-6"
        >
          {movies.map((movie) => (
            <MovieCard key={movie.id} movie={movie} locale={locale} className="w-32 shrink-0 snap-start md:w-36" />
          ))}
        </div>
      </div>
    </section>
  );
}

function RowArrow({ side, hidden, onClick }: { side: "left" | "right"; hidden: boolean; onClick: () => void }) {
  const Icon = side === "left" ? ChevronLeft : ChevronRight;
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={side === "left" ? "Scroll left" : "Scroll right"}
      tabIndex={hidden ? -1 : 0}
      className={cn(
        "absolute top-0 z-10 hidden h-[calc(100%-0.5rem)] w-12 items-center justify-center md:flex",
        // Fade the poster edge under the control so it reads as an affordance, not an overlay.
        side === "left"
          ? "left-0 bg-gradient-to-r from-surface to-transparent"
          : "right-0 bg-gradient-to-l from-surface to-transparent",
        // Revealed on row hover so a resting page stays clean; always reachable by keyboard.
        "opacity-0 transition-opacity focus-within:opacity-100 group-hover/row:opacity-100",
        hidden && "pointer-events-none !opacity-0",
      )}
    >
      <span className="flex h-9 w-9 items-center justify-center rounded-full bg-surface-card/90 text-ink shadow-lg ring-1 ring-surface-border">
        <Icon className="h-5 w-5" aria-hidden />
      </span>
    </button>
  );
}
