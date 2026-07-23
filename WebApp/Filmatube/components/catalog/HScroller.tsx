"use client";

import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * A horizontally scrolling strip with left/right arrow buttons — shared by every Home row so
 * they all behave the same.
 *
 * Whenever a row is wide enough to scroll at all, BOTH arrows stay on screen (desktop only);
 * the one at the current end is dimmed rather than hidden, so the control is always findable
 * instead of appearing only mid-scroll. A row that fits entirely shows no arrows — there is
 * nothing to scroll — which is why a small catalogue looks bare.
 *
 * Measurement is driven by a ResizeObserver, not just a one-shot on mount: card widths settle
 * and images/fonts load after the first paint, and a scrollWidth read too early would report
 * "fits" and never show the arrows.
 */
export function HScroller({ children }: { children: ReactNode }) {
  const scroller = useRef<HTMLDivElement>(null);
  const [scrollable, setScrollable] = useState(false);
  const [atStart, setAtStart] = useState(true);
  const [atEnd, setAtEnd] = useState(false);

  const sync = useCallback(() => {
    const el = scroller.current;
    if (!el) return;
    // 1px slack absorbs sub-pixel widths that would never otherwise report "fully scrolled".
    setScrollable(el.scrollWidth > el.clientWidth + 1);
    setAtStart(el.scrollLeft <= 1);
    setAtEnd(el.scrollLeft + el.clientWidth >= el.scrollWidth - 1);
  }, []);

  useEffect(() => {
    const el = scroller.current;
    if (!el) return;
    sync();
    const observer = new ResizeObserver(sync);
    observer.observe(el);
    // Card widths are viewport-relative below md, so a window resize changes overflow too.
    window.addEventListener("resize", sync);
    return () => {
      observer.disconnect();
      window.removeEventListener("resize", sync);
    };
  }, [sync, children]);

  const page = (dir: -1 | 1) => {
    const el = scroller.current;
    if (!el) return;
    el.scrollBy({ left: dir * el.clientWidth * 0.9, behavior: "smooth" });
  };

  return (
    <div className="relative">
      {scrollable && (
        <>
          <Arrow side="left" disabled={atStart} onClick={() => page(-1)} />
          <Arrow side="right" disabled={atEnd} onClick={() => page(1)} />
        </>
      )}
      <div
        ref={scroller}
        onScroll={sync}
        className="flex snap-x gap-3 overflow-x-auto scroll-smooth px-4 pb-2 md:px-6"
      >
        {children}
      </div>
    </div>
  );
}

function Arrow({ side, disabled, onClick }: { side: "left" | "right"; disabled: boolean; onClick: () => void }) {
  const Icon = side === "left" ? ChevronLeft : ChevronRight;
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={side === "left" ? "Scroll left" : "Scroll right"}
      className={cn(
        "absolute top-0 z-10 hidden h-[calc(100%-0.5rem)] w-12 items-center justify-center md:flex",
        // Fade the poster edge under the control so it reads as an affordance, not an overlay.
        side === "left"
          ? "left-0 bg-gradient-to-r from-surface to-transparent"
          : "right-0 bg-gradient-to-l from-surface to-transparent",
        // Dimmed and inert at the end you can't move toward, rather than vanishing.
        disabled ? "cursor-default opacity-30" : "opacity-100",
      )}
    >
      <span className="flex h-9 w-9 items-center justify-center rounded-full bg-surface-card text-ink shadow-lg ring-1 ring-surface-border">
        <Icon className="h-5 w-5" aria-hidden />
      </span>
    </button>
  );
}
