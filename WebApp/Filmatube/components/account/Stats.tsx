import { watchHours, type UserStats } from "@/lib/stats";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/**
 * Watch stats. These are single headline numbers, so they're stat tiles rather than charts —
 * there's no distribution to plot. Values wear text tokens (no series colour), and the top-genre
 * chips carry identity in their labels, not in colour.
 */
export function Stats({
  stats,
  dict,
  genres,
}: {
  stats: UserStats;
  dict: Dictionary["catalog"];
  genres: Dictionary["genres"];
}) {
  const tiles = [
    { value: watchHours(stats), label: dict.statHours },
    { value: stats.moviesCompleted, label: dict.statMovies },
    { value: stats.reviewsWritten, label: dict.statReviews },
  ];

  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold text-ink">{dict.statsTitle}</h2>
      <div className="grid grid-cols-3 gap-3">
        {tiles.map((t) => (
          <div key={t.label} className="rounded-2xl border border-surface-border bg-surface-card p-4 text-center">
            <p className="text-3xl font-black tabular-nums text-ink">{t.value}</p>
            <p className="mt-1 text-xs text-ink-muted">{t.label}</p>
          </div>
        ))}
      </div>

      {stats.topGenres.length > 0 && (
        <div className="flex flex-wrap items-center gap-2 pt-1">
          <span className="text-sm text-ink-muted">{dict.statTopGenres}</span>
          {stats.topGenres.map((g) => (
            <span
              key={g}
              className="rounded-full border border-surface-border bg-surface px-2.5 py-0.5 text-sm font-medium text-ink"
            >
              {(genres as Record<string, string>)[g] ?? g}
            </span>
          ))}
        </div>
      )}
    </section>
  );
}
