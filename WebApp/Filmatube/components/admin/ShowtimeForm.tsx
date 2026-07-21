"use client";

import { useState, useTransition } from "react";
import { CalendarPlus } from "lucide-react";
import { createShowtime } from "@/app/admin/theater/actions";
import type { SchedulableMovie } from "@/lib/admin/theater";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Schedule a new screening: movie, when, seats, premiere flag. */
export function ShowtimeForm({
  movies,
  dict,
}: {
  movies: SchedulableMovie[];
  dict: Dictionary["admin"];
}) {
  const [movieId, setMovieId] = useState("");
  const [startAt, setStartAt] = useState("");
  const [capacity, setCapacity] = useState("0");
  const [isPremiere, setIsPremiere] = useState(false);
  const [recurrence, setRecurrence] = useState("none");
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  const submit = () => {
    setError(null);
    if (!movieId || !startAt) {
      setError(dict.showtimeFormIncomplete);
      return;
    }
    // <input type="datetime-local"> has no timezone, so this parses in the admin's own
    // zone — which is what they meant when they typed it.
    const startAtMs = new Date(startAt).getTime();
    if (!Number.isFinite(startAtMs)) {
      setError(dict.showtimeFormIncomplete);
      return;
    }

    startTransition(() => {
      void createShowtime({
        movieId,
        startAtMs,
        capacity: Number(capacity) || 0,
        isPremiere,
        recurrence,
      })
        .then(() => {
          setMovieId("");
          setStartAt("");
          setCapacity("0");
          setIsPremiere(false);
          setRecurrence("none");
        })
        .catch(() => setError(dict.showtimeFormFailed));
    });
  };

  return (
    <div className="space-y-3 rounded-xl border border-surface-border p-4">
      <h2 className="flex items-center gap-2 font-semibold text-ink">
        <CalendarPlus className="h-4 w-4" aria-hidden />
        {dict.showtimeNew}
      </h2>

      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block">
          <span className="mb-1 block text-xs text-ink-muted">{dict.showtimeMovie}</span>
          <select
            value={movieId}
            onChange={(e) => setMovieId(e.target.value)}
            className="h-10 w-full rounded-lg border border-surface-border bg-transparent px-2 text-sm text-ink focus:outline-none focus:ring-1 focus:ring-brand-500"
          >
            <option value="">—</option>
            {movies.map((m) => (
              <option key={m.id} value={m.id}>
                {m.title}
              </option>
            ))}
          </select>
        </label>

        <label className="block">
          <span className="mb-1 block text-xs text-ink-muted">{dict.showtimeStartAt}</span>
          <input
            type="datetime-local"
            value={startAt}
            onChange={(e) => setStartAt(e.target.value)}
            className="h-10 w-full rounded-lg border border-surface-border bg-transparent px-2 text-sm text-ink focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-xs text-ink-muted">{dict.showtimeCapacity}</span>
          <input
            type="number"
            min={0}
            value={capacity}
            onChange={(e) => setCapacity(e.target.value)}
            className="h-10 w-full rounded-lg border border-surface-border bg-transparent px-2 text-sm text-ink focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </label>

        <label className="block">
          <span className="mb-1 block text-xs text-ink-muted">{dict.showtimeRecurrence}</span>
          <select
            value={recurrence}
            onChange={(e) => setRecurrence(e.target.value)}
            className="h-10 w-full rounded-lg border border-surface-border bg-transparent px-2 text-sm text-ink focus:outline-none focus:ring-1 focus:ring-brand-500"
          >
            <option value="none">{dict.showtimeRecurrenceNone}</option>
            <option value="daily">{dict.showtimeRecurrenceDaily}</option>
            <option value="weekly">{dict.showtimeRecurrenceWeekly}</option>
          </select>
        </label>

        <label className="flex items-end gap-2 pb-2">
          <input
            type="checkbox"
            checked={isPremiere}
            onChange={(e) => setIsPremiere(e.target.checked)}
            className="h-4 w-4 accent-brand-500"
          />
          <span className="text-sm text-ink">{dict.showtimePremiere}</span>
        </label>
      </div>

      {error && <p className="text-sm text-red-400">{error}</p>}

      <button
        type="button"
        onClick={submit}
        disabled={pending}
        className="h-10 rounded-lg bg-brand-500 px-5 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
      >
        {dict.showtimeSchedule}
      </button>
    </div>
  );
}
