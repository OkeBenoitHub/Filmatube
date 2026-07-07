import { MovieCard } from "@/components/catalog/MovieCard";
import type { CatalogMovie } from "@/lib/movies";
import type { Locale } from "@/lib/i18n/config";

/** Horizontally scrolling row of poster tiles under a section title. */
export function MovieRow({
  title,
  movies,
  locale,
}: {
  title: string;
  movies: CatalogMovie[];
  locale: Locale;
}) {
  if (movies.length === 0) return null;
  return (
    <section className="space-y-3">
      <h2 className="px-4 text-lg font-semibold text-ink md:px-6">{title}</h2>
      <div className="flex snap-x gap-3 overflow-x-auto px-4 pb-2 md:px-6">
        {movies.map((movie) => (
          <MovieCard key={movie.id} movie={movie} locale={locale} className="w-32 shrink-0 snap-start md:w-36" />
        ))}
      </div>
    </section>
  );
}
