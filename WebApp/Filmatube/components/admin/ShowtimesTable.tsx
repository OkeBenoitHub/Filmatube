"use client";

import { useTransition } from "react";
import Link from "next/link";
import {
  MessageSquare,
  Pause,
  Play,
  Rewind,
  FastForward,
  Sparkles,
  Square,
  Trash2,
  Users,
} from "lucide-react";
import {
  deleteShowtime,
  pauseShowtime,
  resumeShowtime,
  setShowtimeStatus,
  skipShowtime,
} from "@/app/admin/theater/actions";
import { SHOWTIME_STATUS } from "@/lib/theater-model";
import type { AdminShowtime } from "@/lib/admin/theater";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

/** How far the skip buttons jump, in seconds. */
const SKIP_SECONDS = 30;

/**
 * The lineup: every showtime with its attendance record and, while it's running, the
 * host controls.
 *
 * Those controls edit the *schedule* rather than driving a playhead — there is no host
 * player to command. Pausing freezes the clock; resuming shifts the start time forward by
 * the paused duration; skipping moves the start time in the opposite direction to the jump.
 */
export function ShowtimesTable({
  showtimes,
  dict,
}: {
  showtimes: AdminShowtime[];
  dict: Dictionary["admin"];
}) {
  const [pending, startTransition] = useTransition();
  const run = (fn: () => Promise<unknown>) => startTransition(() => void fn());

  if (showtimes.length === 0) {
    return <p className="py-16 text-center text-ink-muted">{dict.showtimesEmpty}</p>;
  }

  return (
    <ul className="space-y-2">
      {showtimes.map((s) => {
        const paused = s.pausedAtMs > 0;
        const running = s.status === SHOWTIME_STATUS.LIVE;
        const open = running || s.status === SHOWTIME_STATUS.LOBBY;

        return (
          <li key={s.id} className="rounded-xl border border-surface-border p-3">
            <div className="flex items-start gap-3">
              <div className="h-16 w-11 shrink-0 overflow-hidden rounded bg-surface-hover">
                {s.posterUrl && (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={s.posterUrl} alt="" className="h-full w-full object-cover" />
                )}
              </div>

              <div className="min-w-0 flex-1">
                <Link
                  href={`/theater/${s.id}`}
                  className="flex items-center gap-1.5 truncate font-semibold text-ink hover:underline"
                >
                  {s.movieTitle}
                  {s.isPremiere && <Sparkles className="h-3.5 w-3.5 shrink-0 text-brand-400" aria-hidden />}
                </Link>

                <p className="truncate text-xs text-ink-muted" suppressHydrationWarning>
                  {new Date(s.startAtMs).toLocaleString()} · {s.status}
                  {paused && ` · ${dict.showtimePaused}`}
                  {s.capacity > 0 && ` · ${s.attendeesCount}/${s.capacity}`}
                </p>

                {/* Attendance record */}
                <p className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-ink-muted">
                  <span className="inline-flex items-center gap-1">
                    <Users className="h-3.5 w-3.5" aria-hidden />
                    {dict.showtimeRsvps.replace("{n}", String(s.attendeesCount))}
                  </span>
                  <span className="inline-flex items-center gap-1">
                    <Play className="h-3.5 w-3.5" aria-hidden />
                    {dict.showtimeAttended.replace("{n}", String(s.presentCount))}
                  </span>
                  <span className="inline-flex items-center gap-1">
                    <MessageSquare className="h-3.5 w-3.5" aria-hidden />
                    {dict.showtimeChatLines.replace("{n}", String(s.chatCount))}
                  </span>
                  {/* Null when nobody RSVP'd — 0/0 is "no data", not "0% turnout". */}
                  {s.turnoutRate !== null && (
                    <span className="font-medium text-ink">
                      {dict.showtimeTurnout.replace("{p}", String(Math.round(s.turnoutRate * 100)))}
                    </span>
                  )}
                </p>
              </div>

              <button
                type="button"
                disabled={pending}
                onClick={() => {
                  if (confirm(dict.showtimeDeleteConfirm)) run(() => deleteShowtime(s.id));
                }}
                aria-label={dict.showtimeDelete}
                className="shrink-0 rounded-lg border border-surface-border p-2 text-ink-muted hover:bg-surface-hover hover:text-red-400 disabled:opacity-60"
              >
                <Trash2 className="h-3.5 w-3.5" aria-hidden />
              </button>
            </div>

            {/* ── Lifecycle + host controls ── */}
            <div className="mt-3 flex flex-wrap gap-1.5 border-t border-surface-border pt-3">
              {[SHOWTIME_STATUS.SCHEDULED, SHOWTIME_STATUS.LOBBY, SHOWTIME_STATUS.LIVE].map((st) => (
                <button
                  key={st}
                  type="button"
                  disabled={pending || s.status === st}
                  onClick={() => run(() => setShowtimeStatus(s.id, st))}
                  className={cn(
                    "h-8 rounded-lg border px-2.5 text-xs font-medium transition-colors disabled:opacity-60",
                    s.status === st
                      ? "border-brand-500 bg-brand-500/15 text-ink"
                      : "border-surface-border text-ink-muted hover:bg-surface-hover hover:text-ink",
                  )}
                >
                  {st}
                </button>
              ))}

              {running && (
                <>
                  <button
                    type="button"
                    disabled={pending}
                    onClick={() => run(() => (paused ? resumeShowtime(s.id) : pauseShowtime(s.id)))}
                    className="inline-flex h-8 items-center gap-1.5 rounded-lg border border-surface-border px-2.5 text-xs font-medium text-ink hover:bg-surface-hover disabled:opacity-60"
                  >
                    {paused ? <Play className="h-3.5 w-3.5" aria-hidden /> : <Pause className="h-3.5 w-3.5" aria-hidden />}
                    {paused ? dict.showtimeResume : dict.showtimePause}
                  </button>
                  <button
                    type="button"
                    disabled={pending}
                    onClick={() => run(() => skipShowtime(s.id, -SKIP_SECONDS))}
                    aria-label={dict.showtimeSkipBack.replace("{n}", String(SKIP_SECONDS))}
                    className="inline-flex h-8 items-center gap-1 rounded-lg border border-surface-border px-2.5 text-xs font-medium text-ink hover:bg-surface-hover disabled:opacity-60"
                  >
                    <Rewind className="h-3.5 w-3.5" aria-hidden />
                    {SKIP_SECONDS}s
                  </button>
                  <button
                    type="button"
                    disabled={pending}
                    onClick={() => run(() => skipShowtime(s.id, SKIP_SECONDS))}
                    aria-label={dict.showtimeSkipForward.replace("{n}", String(SKIP_SECONDS))}
                    className="inline-flex h-8 items-center gap-1 rounded-lg border border-surface-border px-2.5 text-xs font-medium text-ink hover:bg-surface-hover disabled:opacity-60"
                  >
                    <FastForward className="h-3.5 w-3.5" aria-hidden />
                    {SKIP_SECONDS}s
                  </button>
                </>
              )}

              {open && (
                <button
                  type="button"
                  disabled={pending}
                  onClick={() => {
                    if (confirm(dict.showtimeEndConfirm)) run(() => setShowtimeStatus(s.id, SHOWTIME_STATUS.ENDED));
                  }}
                  className="inline-flex h-8 items-center gap-1.5 rounded-lg border border-surface-border px-2.5 text-xs font-medium text-ink-muted hover:bg-surface-hover hover:text-red-400 disabled:opacity-60"
                >
                  <Square className="h-3.5 w-3.5" aria-hidden />
                  {dict.showtimeEnd}
                </button>
              )}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
