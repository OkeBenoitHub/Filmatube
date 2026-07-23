import { notFound } from "next/navigation";
import { User } from "lucide-react";
import { MovieCard } from "@/components/catalog/MovieCard";
import { getPublishedMovies } from "@/lib/movies";
import { getDict, getLocale } from "@/lib/i18n/server";

/**
 * An actor's filmography — every published movie whose cast (or director credit) includes the
 * name. Reached from a tappable cast member on a movie detail page, and from searching a name.
 */
export default async function ActorPage({ params }: { params: Promise<{ name: string }> }) {
  const [{ name: raw }, locale, dict, movies] = await Promise.all([
    params,
    getLocale(),
    getDict(),
    getPublishedMovies(),
  ]);
  const name = decodeURIComponent(raw);
  const c = dict.catalog;

  const needle = name.toLowerCase();
  const films = movies.filter(
    (m) =>
      m.cast.some((person) => person.name.toLowerCase() === needle) ||
      m.directors.some((d) => d.toLowerCase() === needle),
  );
  if (films.length === 0) notFound();

  // Reuse a cast photo for the header if any of these movies carry one for this person.
  const photo = films
    .flatMap((m) => m.cast)
    .find((person) => person.name.toLowerCase() === needle && person.photoUrl)?.photoUrl;

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 md:px-6">
      <div className="flex items-center gap-5">
        <div className="flex h-24 w-24 shrink-0 items-center justify-center overflow-hidden rounded-full border border-surface-border bg-surface-hover shadow-xl shadow-brand-900/30 sm:h-28 sm:w-28">
          {photo ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={photo} alt="" className="h-full w-full object-cover" />
          ) : (
            <User className="h-10 w-10 text-ink-faint" aria-hidden />
          )}
        </div>
        <div className="min-w-0">
          <p className="text-xs font-bold uppercase tracking-widest text-ink-muted">{c.cast}</p>
          <h1 className="mt-1 text-3xl font-black leading-tight tracking-tight text-ink md:text-4xl">{name}</h1>
          <p className="mt-1 text-sm text-ink-muted">
            {c.actorFilmography.replace("{n}", String(films.length))}
          </p>
        </div>
      </div>

      <div className="mt-10 grid grid-cols-3 gap-3 sm:grid-cols-4 md:grid-cols-6">
        {films.map((movie) => (
          <MovieCard key={movie.id} movie={movie} locale={locale} />
        ))}
      </div>
    </div>
  );
}

export async function generateMetadata({ params }: { params: Promise<{ name: string }> }) {
  const { name } = await params;
  return { title: `${decodeURIComponent(name)} — Filmatube` };
}

