"use client";

import { useEffect, useState } from "react";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/**
 * A once-a-second clock shared by every countdown on the page.
 *
 * Starts from `null` and only begins ticking after mount: the server and the client would
 * otherwise render different "now" values and React would report a hydration mismatch.
 */
export function useNow(): number | null {
  const [now, setNow] = useState<number | null>(null);
  useEffect(() => {
    setNow(Date.now());
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);
  return now;
}

/** "Live now" / "in 2d 4h" / "in 12m 30s" — coarse far out, precise up close. */
export function countdownLabel(
  startAtMs: number,
  nowMs: number,
  live: boolean,
  dict: Dictionary["catalog"],
): string {
  if (live) return dict.theaterLiveNow;
  const remaining = Math.max(0, startAtMs - nowMs);
  if (remaining === 0) return dict.theaterStartingNow;

  const totalSeconds = Math.floor(remaining / 1000);
  const days = Math.floor(totalSeconds / 86_400);
  const hours = Math.floor((totalSeconds % 86_400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  if (days > 0) return dict.theaterCountdownDays.replace("{d}", String(days)).replace("{h}", String(hours));
  if (hours > 0) return dict.theaterCountdownHours.replace("{h}", String(hours)).replace("{m}", String(minutes));
  return dict.theaterCountdownMinutes.replace("{m}", String(minutes)).replace("{s}", String(seconds));
}

/** Live-updating countdown text. Renders a stable placeholder until the clock starts. */
export function Countdown({
  startAtMs,
  live,
  dict,
  className,
}: {
  startAtMs: number;
  live: boolean;
  dict: Dictionary["catalog"];
  className?: string;
}) {
  const now = useNow();
  return (
    <span className={className} suppressHydrationWarning>
      {now === null ? (live ? dict.theaterLiveNow : "—") : countdownLabel(startAtMs, now, live, dict)}
    </span>
  );
}
