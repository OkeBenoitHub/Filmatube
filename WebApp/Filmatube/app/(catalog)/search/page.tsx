import { SearchClient } from "@/components/catalog/SearchClient";
import { getDict, getLocale } from "@/lib/i18n/server";
import { getPublishedMovies, localized, pickTrending } from "@/lib/movies";

export default async function SearchPage() {
  const [locale, dict, movies] = await Promise.all([getLocale(), getDict(), getPublishedMovies()]);

  const trending = pickTrending(movies)
    .slice(0, 12)
    .map((m) => ({
      id: m.id,
      title: localized(m.title, locale),
      posterUrl: m.posterUrl,
      year: m.year,
      isComingSoon: m.isComingSoon,
    }));

  return (
    <div className="mx-auto max-w-6xl space-y-6 px-4 py-8 md:px-6">
      <h1 className="text-2xl font-bold text-ink">{dict.catalog.search}</h1>
      <SearchClient dict={dict.catalog} trending={trending} />
    </div>
  );
}
