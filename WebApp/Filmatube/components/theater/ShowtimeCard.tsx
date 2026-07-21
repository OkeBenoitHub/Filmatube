"use client";

import Link from "next/link";
import { Sparkles, Users } from "lucide-react";
import { Countdown } from "@/components/theater/Countdown";
import type { Showtime } from "@/lib/theater-model";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

/** The "on air" dot — same language the party room uses. */
export function LiveDot() {
  return (
    <span className="relative flex h-2 w-2" aria-hidden>
      <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-brand-400 opacity-75" />
      <span className="relative inline-flex h-2 w-2 rounded-full bg-brand-500" />
    </span>
  );
}

/**
 * Gold, not the brand green: a premiere is the one genuinely rare thing on the lineup — a
 * film's first public screening — and it can't say so while wearing the same colour as every
 * ordinary showtime, the live dot and the RSVP button.
 */
export function PremiereBadge({ label }: { label: string }) {
  return (
    <span className="inline-flex items-center gap-1 rounded-full bg-gold px-2 py-0.5 text-[11px] font-bold text-black/85">
      <Sparkles className="h-3 w-3" aria-hidden />
      {label}
    </span>
  );
}

/** Wide hero card for whatever is on now — or next up. */
export function FeaturedShowtimeCard({ showtime, dict }: { showtime: Showtime; dict: Dictionary["catalog"] }) {
  const live = showtime.status === "live";
  return (
    <Link
      href={`/theater/${showtime.id}`}
      className="group relative block overflow-hidden rounded-2xl border border-surface-border"
    >
      <div className="relative aspect-[16/9] w-full bg-surface-hover">
        {(showtime.backdropUrl || showtime.posterUrl) && (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={showtime.backdropUrl || showtime.posterUrl}
            alt=""
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
        )}
        {/* Scrim so badges and title stay legible over any artwork. */}
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/10 to-black/40" />

        <div className="absolute left-4 top-4 flex flex-wrap items-center gap-2">
          {showtime.isPremiere && <PremiereBadge label={dict.theaterPremiere} />}
          {live && (
            <span className="inline-flex items-center gap-1.5 rounded-full bg-black/60 px-2.5 py-1 text-[11px] font-semibold text-white">
              <LiveDot />
              {dict.theaterLiveNow}
            </span>
          )}
        </div>

        <div className="absolute inset-x-4 bottom-4">
          <h3 className="line-clamp-2 text-xl font-black tracking-tight text-white md:text-2xl">
            {showtime.movieTitle}
          </h3>
          <Countdown
            startAtMs={showtime.startAtMs}
            live={live}
            dict={dict}
            className={cn("text-sm font-semibold", live ? "text-brand-400" : "text-white/90")}
          />
        </div>
      </div>

      <div className="flex items-center gap-2 px-4 py-3 text-sm text-ink-muted">
        <Users className="h-4 w-4" aria-hidden />
        <span>
          {(live ? dict.theaterWatching : dict.theaterGoing).replace("{n}", String(showtime.attendeesCount))}
        </span>
        <span className="ml-auto text-xs" suppressHydrationWarning>
          {new Date(showtime.startAtMs).toLocaleString()}
        </span>
      </div>
    </Link>
  );
}

/** Compact row for the rest of the schedule. */
export function ShowtimeRow({ showtime, dict }: { showtime: Showtime; dict: Dictionary["catalog"] }) {
  const live = showtime.status === "live";
  return (
    <Link
      href={`/theater/${showtime.id}`}
      className="flex items-center gap-4 rounded-xl border border-surface-border p-3 hover:bg-surface-hover"
    >
      <div className="h-20 w-14 shrink-0 overflow-hidden rounded-lg bg-surface-hover">
        {showtime.posterUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={showtime.posterUrl} alt="" className="h-full w-full object-cover" />
        )}
      </div>

      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <p className="truncate text-sm font-semibold text-ink">{showtime.movieTitle}</p>
          {showtime.isPremiere && <PremiereBadge label={dict.theaterPremiere} />}
        </div>
        <p className="mt-0.5 text-xs text-ink-muted" suppressHydrationWarning>
          {new Date(showtime.startAtMs).toLocaleString()}
        </p>
        <p className="mt-0.5 flex items-center gap-1.5 text-xs text-ink-muted">
          <Users className="h-3.5 w-3.5" aria-hidden />
          {(live ? dict.theaterWatching : dict.theaterGoing).replace("{n}", String(showtime.attendeesCount))}
        </p>
      </div>

      <Countdown
        startAtMs={showtime.startAtMs}
        live={live}
        dict={dict}
        className={cn("shrink-0 text-sm font-semibold", live ? "text-brand-400" : "text-ink")}
      />
    </Link>
  );
}
