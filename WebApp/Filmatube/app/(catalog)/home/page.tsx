import { Hero } from "@/components/catalog/Hero";
import { MovieRow } from "@/components/catalog/MovieRow";
import { ContinueWatchingRow } from "@/components/catalog/ContinueWatchingRow";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict, getLocale } from "@/lib/i18n/server";
import { getUserProfile } from "@/lib/user";
import {
  getContinueWatching,
  getPublishedMovies,
  pickByGenre,
  pickComingSoon,
  pickFeatured,
  pickNewReleases,
  pickTrending,
} from "@/lib/movies";

export default async function CatalogHomePage() {
  const [locale, dict, user, movies] = await Promise.all([
    getLocale(),
    getDict(),
    getCurrentUser(),
    getPublishedMovies(),
  ]);
  const c = dict.catalog;

  if (movies.length === 0) {
    return <p className="px-4 py-24 text-center text-ink-muted md:px-6">{c.empty}</p>;
  }

  const featured = pickFeatured(movies);
  const hero = featured[0] ?? movies[0];

  // Genre rows follow the viewer's taste preferences, falling back to top genres.
  const profile = user ? await getUserProfile(user.uid) : null;
  const genreKeys = (profile?.genrePreferences ?? []).slice(0, 4);
  const genreRows = genreKeys
    .map((key) => ({ key, movies: pickByGenre(movies, key) }))
    .filter((row) => row.movies.length > 0);

  const continueWatching = user ? await getContinueWatching(user.uid) : [];

  return (
    <div>
      <Hero movie={hero} locale={locale} dict={c} />
      <div className="mx-auto max-w-6xl space-y-8 py-8">
        <ContinueWatchingRow title={c.continueWatching} items={continueWatching} locale={locale} />
        <MovieRow title={c.trending} movies={pickTrending(movies)} locale={locale} />
        <MovieRow title={c.newReleases} movies={pickNewReleases(movies)} locale={locale} />
        {genreRows.map((row) => (
          <MovieRow
            key={row.key}
            title={(dict.genres as Record<string, string>)[row.key] ?? row.key}
            movies={row.movies}
            locale={locale}
          />
        ))}
        <MovieRow title={c.comingSoon} movies={pickComingSoon(movies)} locale={locale} />
      </div>
    </div>
  );
}
