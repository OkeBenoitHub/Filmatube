import { ShowtimeForm } from "@/components/admin/ShowtimeForm";
import { ShowtimesTable } from "@/components/admin/ShowtimesTable";
import { getAdminShowtimes } from "@/lib/admin/theater";
import { getLocale, getDict } from "@/lib/i18n/server";
import { getPublishedMovies, localized } from "@/lib/movies";

export default async function AdminTheaterPage() {
  const [dict, locale, showtimes, movies] = await Promise.all([
    getDict(),
    getLocale(),
    getAdminShowtimes(),
    getPublishedMovies(),
  ]);

  // Coming-soon titles have nothing to stream, so they can't be scheduled.
  const schedulable = movies
    .filter((m) => !m.isComingSoon)
    .map((m) => ({
      id: m.id,
      title: localized(m.title, locale),
      posterUrl: m.posterUrl,
      backdropUrl: m.backdropUrl ?? "",
    }))
    .sort((a, b) => a.title.localeCompare(b.title));

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-bold text-ink">{dict.admin.theater}</h1>
      <ShowtimeForm movies={schedulable} dict={dict.admin} />
      <ShowtimesTable showtimes={showtimes} dict={dict.admin} />
    </div>
  );
}
