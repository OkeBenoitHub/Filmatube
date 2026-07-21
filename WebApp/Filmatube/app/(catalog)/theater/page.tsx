import { Clapperboard } from "lucide-react";
import { FeaturedShowtimeCard, ShowtimeRow } from "@/components/theater/ShowtimeCard";
import { PageHero } from "@/components/ui/PageHero";
import { getLineup, isOpen } from "@/lib/theater";
import { getDict } from "@/lib/i18n/server";

/**
 * Theater discovery: the public lineup of scheduled showtimes and premieres.
 *
 * Leads with whatever is on right now — or the next thing up — then splits the rest into
 * "now showing" and "coming up", because "on air" and "worth planning for" are different
 * decisions and shouldn't share one list.
 */
export default async function TheaterPage() {
  const [lineup, dict] = await Promise.all([getLineup(), getDict()]);
  const c = dict.catalog;

  const nowShowing = lineup.filter(isOpen);
  const upcoming = lineup.filter((s) => !isOpen(s));
  const featured = nowShowing[0] ?? upcoming[0] ?? null;
  const liveRest = nowShowing.filter((s) => s.id !== featured?.id);
  const upcomingRest = upcoming.filter((s) => s.id !== featured?.id);

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 md:px-6">
      <PageHero
        icon={Clapperboard}
        eyebrow={c.theaterEyebrow}
        title={c.theaterTitle}
        subtitle={c.theaterSubtitle}
      />

      {lineup.length === 0 ? (
        <div className="mt-10 rounded-2xl border border-surface-border px-6 py-16 text-center">
          <p className="text-sm font-semibold text-ink">{c.theaterEmptyTitle}</p>
          <p className="mt-1 text-sm text-ink-muted">{c.theaterEmptyMessage}</p>
        </div>
      ) : (
        <div className="mt-10 space-y-10">
          {featured && <FeaturedShowtimeCard showtime={featured} dict={c} />}

          {liveRest.length > 0 && (
            <section>
              <h2 className="text-lg font-bold text-ink">{c.theaterNowShowing}</h2>
              <div className="mt-3 space-y-2">
                {liveRest.map((s) => (
                  <ShowtimeRow key={s.id} showtime={s} dict={c} />
                ))}
              </div>
            </section>
          )}

          {upcomingRest.length > 0 && (
            <section>
              <h2 className="text-lg font-bold text-ink">{c.theaterUpcoming}</h2>
              <div className="mt-3 space-y-2">
                {upcomingRest.map((s) => (
                  <ShowtimeRow key={s.id} showtime={s} dict={c} />
                ))}
              </div>
            </section>
          )}
        </div>
      )}
    </div>
  );
}
