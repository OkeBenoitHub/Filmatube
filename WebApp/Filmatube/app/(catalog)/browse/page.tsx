import { Compass } from "lucide-react";
import { BrowseControls } from "@/components/catalog/BrowseControls";
import { MovieCard } from "@/components/catalog/MovieCard";
import { PageHero } from "@/components/ui/PageHero";
import { getDict, getLocale } from "@/lib/i18n/server";
import { getPublishedMovies, localized } from "@/lib/movies";

export default async function BrowsePage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const [locale, dict, movies, sp] = await Promise.all([
    getLocale(),
    getDict(),
    getPublishedMovies(),
    searchParams,
  ]);
  const c = dict.catalog;

  const genre = typeof sp.genre === "string" ? sp.genre : "";
  const year = typeof sp.year === "string" ? sp.year : "";
  const sort = typeof sp.sort === "string" ? sp.sort : "newest";

  let list = movies.filter((m) => !m.isComingSoon);
  if (genre) list = list.filter((m) => m.genres.includes(genre));
  if (year) list = list.filter((m) => String(m.year) === year);
  if (sort === "rating") list = [...list].sort((a, b) => b.averageRating - a.averageRating);
  else if (sort === "az")
    list = [...list].sort((a, b) => localized(a.title, locale).localeCompare(localized(b.title, locale)));

  const years = [...new Set(movies.map((m) => m.year).filter(Boolean))].sort((a, b) => b - a);

  return (
    <div className="mx-auto max-w-6xl space-y-6 px-4 py-8 md:px-6">
      <PageHero icon={Compass} eyebrow={c.requestsEyebrow} title={c.browse} subtitle={c.browseSubtitle} />
      <div className="pt-4">
        <BrowseControls dict={c} genres={dict.genres} years={years} />
      </div>
      {list.length === 0 ? (
        <p className="py-16 text-center text-ink-muted">{c.empty}</p>
      ) : (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
          {list.map((movie) => (
            <MovieCard key={movie.id} movie={movie} locale={locale} />
          ))}
        </div>
      )}
    </div>
  );
}
