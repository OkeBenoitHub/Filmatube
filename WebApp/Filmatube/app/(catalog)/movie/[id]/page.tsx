import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Play, Film } from "lucide-react";
import { MovieRow } from "@/components/catalog/MovieRow";
import { ShareBar } from "@/components/catalog/ShareBar";
import { SaveButton } from "@/components/catalog/SaveButton";
import { ReactionBar } from "@/components/social/ReactionBar";
import { RecommendButton } from "@/components/social/RecommendButton";
import { StarRating } from "@/components/social/StarRating";
import { ReviewsSection } from "@/components/social/ReviewsSection";
import { CommentsSection } from "@/components/social/CommentsSection";
import { getDict, getLocale } from "@/lib/i18n/server";
import { getMovie, getPublishedMovies, localized, pickRelated } from "@/lib/movies";

export async function generateMetadata({ params }: { params: Promise<{ id: string }> }): Promise<Metadata> {
  const { id } = await params;
  const [locale, movie] = await Promise.all([getLocale(), getMovie(id)]);
  if (!movie) return {};
  const title = localized(movie.title, locale);
  const description = localized(movie.description, locale).slice(0, 200);
  const image = movie.backdropUrl || movie.posterUrl;
  return {
    title,
    description,
    openGraph: {
      title,
      description,
      url: `/movie/${id}`,
      type: "website",
      images: image ? [{ url: image }] : [],
    },
    twitter: {
      card: "summary_large_image",
      title,
      description,
      images: image ? [image] : [],
    },
  };
}

export default async function MovieDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [locale, dict, movie] = await Promise.all([getLocale(), getDict(), getMovie(id)]);
  if (!movie) notFound();
  const c = dict.catalog;

  const all = await getPublishedMovies();
  const related = pickRelated(all, movie);
  const title = localized(movie.title, locale);

  return (
    <div className="pb-8">
      {/* Backdrop */}
      <div className="relative h-[46vh] min-h-[280px] w-full overflow-hidden">
        {movie.backdropUrl && (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={movie.backdropUrl} alt="" className="absolute inset-0 h-full w-full object-cover" />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-surface via-surface/60 to-transparent" />
      </div>

      <div className="mx-auto -mt-28 max-w-5xl px-4 md:px-6">
        <div className="flex flex-col gap-6 md:flex-row">
          {/* Poster */}
          <div className="w-36 shrink-0 md:w-48">
            <div className="aspect-[2/3] overflow-hidden rounded-xl border border-surface-border bg-surface-hover shadow-lg">
              {movie.posterUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={movie.posterUrl} alt="" className="h-full w-full object-cover" />
              )}
            </div>
          </div>

          {/* Meta */}
          <div className="flex-1 space-y-4 pt-2 md:pt-24">
            <h1 className="text-3xl font-extrabold text-ink md:text-4xl">{title}</h1>
            <div className="flex flex-wrap items-center gap-2 text-sm text-ink-muted">
              {movie.year > 0 && <span>{movie.year}</span>}
              {movie.ageRating && (
                <span className="rounded border border-surface-border px-1.5 py-0.5 text-xs">{movie.ageRating}</span>
              )}
              {movie.duration > 0 && <span>{movie.duration} min</span>}
              {movie.ratingsCount > 0 && <span>★ {movie.averageRating.toFixed(1)}</span>}
            </div>

            {movie.genres.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {movie.genres.map((g) => (
                  <span key={g} className="rounded-full bg-surface-hover px-2.5 py-1 text-xs text-ink-muted">
                    {(dict.genres as Record<string, string>)[g] ?? g}
                  </span>
                ))}
              </div>
            )}

            <div className="flex flex-wrap items-center gap-3">
              {movie.isComingSoon ? (
                <span className="inline-flex h-11 items-center rounded-lg bg-surface-hover px-6 text-sm font-semibold text-ink-muted">
                  {c.comingSoon}
                </span>
              ) : (
                <Link
                  href={`/watch/${movie.id}`}
                  className="inline-flex h-11 items-center gap-2 rounded-lg bg-brand-500 px-6 text-sm font-semibold text-white transition-colors hover:bg-brand-600"
                >
                  <Play className="h-4 w-4 fill-current" aria-hidden />
                  {c.play}
                </Link>
              )}
              {movie.trailerUrl && (
                <a
                  href={movie.trailerUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex h-11 items-center gap-2 rounded-lg border border-surface-border px-6 text-sm font-semibold text-ink transition-colors hover:bg-surface-hover"
                >
                  <Film className="h-4 w-4" aria-hidden />
                  {c.trailer}
                </a>
              )}
              <SaveButton movieId={movie.id} dict={c} />
              <RecommendButton movieId={movie.id} movieTitle={title} dict={c} />
              <ShareBar movieId={movie.id} title={title} dict={c} />
            </div>

            <ReactionBar movieId={movie.id} dict={c} />

            <StarRating movieId={movie.id} dict={c} />

            {localized(movie.description, locale) && (
              <p className="max-w-2xl text-sm leading-relaxed text-ink-muted md:text-base">
                {localized(movie.description, locale)}
              </p>
            )}

            {movie.directors.length > 0 && (
              <p className="text-sm text-ink-muted">
                <span className="text-ink-faint">{c.directedBy} </span>
                {movie.directors.join(", ")}
              </p>
            )}
          </div>
        </div>

        {/* Cast */}
        {movie.cast.length > 0 && (
          <section className="mt-10 space-y-3">
            <h2 className="text-lg font-semibold text-ink">{c.cast}</h2>
            <div className="flex gap-4 overflow-x-auto pb-2">
              {movie.cast.map((person) => (
                <div key={person.name} className="w-20 shrink-0 text-center">
                  <div className="mx-auto h-20 w-20 overflow-hidden rounded-full border border-surface-border bg-surface-hover">
                    {person.photoUrl && (
                      // eslint-disable-next-line @next/next/no-img-element
                      <img src={person.photoUrl} alt="" className="h-full w-full object-cover" />
                    )}
                  </div>
                  <p className="mt-1.5 truncate text-xs text-ink">{person.name}</p>
                  {person.character && <p className="truncate text-[11px] text-ink-faint">{person.character}</p>}
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Reviews */}
        <section className="mt-10">
          <ReviewsSection movieId={movie.id} dict={c} />
        </section>

        {/* Comments */}
        <section className="mt-10">
          <CommentsSection movieId={movie.id} dict={c} />
        </section>
      </div>

      {/* Related */}
      {related.length > 0 && (
        <div className="mx-auto mt-10 max-w-6xl">
          <MovieRow title={c.moreLikeThis} movies={related} locale={locale} />
        </div>
      )}
    </div>
  );
}
