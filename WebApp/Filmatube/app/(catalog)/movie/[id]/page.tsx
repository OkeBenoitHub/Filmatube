import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Play } from "lucide-react";
import { MovieRow } from "@/components/catalog/MovieRow";
import { ShareBar } from "@/components/catalog/ShareBar";
import { TrailerButton } from "@/components/catalog/TrailerButton";
import { CreatePartyButton } from "@/components/parties/CreatePartyButton";
import { SaveButton } from "@/components/catalog/SaveButton";
import { ReactionBar } from "@/components/social/ReactionBar";
import { RecommendButton } from "@/components/social/RecommendButton";
import { ShareToBoardButton } from "@/components/boards/ShareToBoardButton";
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

      {/*
        `relative z-10` is load-bearing: the gradient above is absolutely positioned, and
        positioned elements paint above static ones whatever the DOM order — without this, the
        gradient's opaque bottom covers the top of the poster and the title.
      */}
      <div className="relative z-10 mx-auto max-w-5xl px-4 md:px-6">
        {/* Poster overlaps the banner; the title sits beside it — both on the solid gradient base. */}
        <div className="-mt-24 flex flex-col gap-5 sm:flex-row sm:items-end md:-mt-28">
          {/* Poster */}
          <div className="w-32 shrink-0 sm:w-40 md:w-48">
            <div className="aspect-[2/3] overflow-hidden rounded-xl border border-surface-border bg-surface-hover shadow-2xl shadow-black/40">
              {movie.posterUrl && (
                // eslint-disable-next-line @next/next/no-img-element
                <img src={movie.posterUrl} alt="" className="h-full w-full object-cover" />
              )}
            </div>
          </div>

          {/* Title + key facts */}
          <div className="min-w-0 flex-1 space-y-3 pb-1">
            <h1 className="text-3xl font-extrabold leading-tight text-ink md:text-4xl">{title}</h1>
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
          </div>
        </div>

        {/* Actions + details, full width below the header */}
        <div className="mt-6 space-y-4">
          <div className="flex flex-wrap items-center gap-3">
            {movie.isComingSoon ? (
              <span className="inline-flex h-11 items-center whitespace-nowrap rounded-lg bg-surface-hover px-6 text-sm font-semibold text-ink-muted">
                {c.comingSoon}
              </span>
            ) : (
              <Link
                href={`/watch/${movie.id}`}
                className="inline-flex h-11 items-center whitespace-nowrap gap-2 rounded-lg bg-brand-500 px-6 text-sm font-semibold text-white transition-colors hover:bg-brand-600"
              >
                <Play className="h-4 w-4 fill-current" aria-hidden />
                {c.play}
              </Link>
            )}
            {movie.trailerUrl && <TrailerButton url={movie.trailerUrl} dict={c} />}
            <CreatePartyButton movieId={movie.id} dict={c} />
            <SaveButton movieId={movie.id} dict={c} />
            <RecommendButton movieId={movie.id} movieTitle={title} dict={c} />
            <ShareToBoardButton movieId={movie.id} movieTitle={title} moviePoster={movie.posterUrl} dict={c} />
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
