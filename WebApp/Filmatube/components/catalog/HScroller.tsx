"use client";

import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * A horizontally scrolling strip with persistent left/right arrow buttons — shared by every
 * Home row so they all behave the same.
 *
 * The arrows are always visible (not hover-gated) whenever there is something to scroll in
 * that direction, and disappear only at each end; a row that fits on screen shows none, which
 * is why a small catalogue looks bare. Desktop only — touch and trackpad scroll directly, so
 * the arrows would just be clutter there.
 */
export function HScroller({ children }: { children: ReactNode }) {
  const scroller = useRef<HTMLDivElement>(null);
  const [canLeft, setCanLeft] = useState(false);
  const [canRight, setCanRight] = useState(false);

  const sync = useCallback(() => {
    const el = scroller.current;
    if (!el) return;
    setCanLeft(el.scrollLeft > 1);
    // 1px slack absorbs sub-pixel widths that would never otherwise report "fully scrolled".
    setCanRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 1);
  }, []);

  useEffect(() => {
    sync();
    // The end position depends on width and on children arriving, so recompute on resize too.
    window.addEventListener("resize", sync);
    return () => window.removeEventListener("resize", sync);
  }, [sync, children]);

  const page = (dir: -1 | 1) => {
    const el = scroller.current;
    if (!el) return;
    el.scrollBy({ left: dir * el.clientWidth * 0.9, behavior: "smooth" });
  };

  return (
    <div className="relative">
      <Arrow side="left" show={canLeft} onClick={() => page(-1)} />
      <Arrow side="right" show={canRight} onClick={() => page(1)} />
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

function Arrow({ side, show, onClick }: { side: "left" | "right"; show: boolean; onClick: () => void }) {
  const Icon = side === "left" ? ChevronLeft : ChevronRight;
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={side === "left" ? "Scroll left" : "Scroll right"}
      tabIndex={show ? 0 : -1}
      className={cn(
        "absolute top-0 z-10 hidden h-[calc(100%-0.5rem)] w-12 items-center justify-center transition-opacity md:flex",
        // Fade the poster edge under the control so it reads as an affordance, not an overlay.
        side === "left"
          ? "left-0 bg-gradient-to-r from-surface to-transparent"
          : "right-0 bg-gradient-to-l from-surface to-transparent",
        show ? "opacity-100" : "pointer-events-none opacity-0",
      )}
    >
      <span className="flex h-9 w-9 items-center justify-center rounded-full bg-surface-card text-ink shadow-lg ring-1 ring-surface-border">
        <Icon className="h-5 w-5" aria-hidden />
      </span>
    </button>
  );
}
