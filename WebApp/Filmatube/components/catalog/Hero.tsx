import Link from "next/link";
import { Play, Info } from "lucide-react";
import { localized, type CatalogMovie } from "@/lib/catalog";
import type { Locale } from "@/lib/i18n/config";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Featured banner at the top of the catalog home. */
export function Hero({
  movie,
  locale,
  dict,
}: {
  movie: CatalogMovie;
  locale: Locale;
  dict: Dictionary["catalog"];
}) {
  return (
    <section className="relative h-[56vh] min-h-[340px] w-full overflow-hidden">
      {movie.backdropUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={movie.backdropUrl} alt="" className="absolute inset-0 h-full w-full object-cover" />
      )}
      <div className="absolute inset-0 bg-gradient-to-t from-surface via-surface/70 to-surface/10" />
      <div className="absolute inset-0 bg-gradient-to-r from-surface/80 to-transparent" />
      <div className="relative flex h-full max-w-6xl flex-col justify-end gap-3 px-4 pb-10 md:mx-auto md:px-6 md:pb-14">
        <h1 className="max-w-xl text-3xl font-extrabold text-ink md:text-5xl">{localized(movie.title, locale)}</h1>
        <div className="flex flex-wrap items-center gap-2 text-sm text-ink-muted">
          {movie.year > 0 && <span>{movie.year}</span>}
          {movie.ageRating && (
            <span className="rounded border border-surface-border px-1.5 py-0.5 text-xs">{movie.ageRating}</span>
          )}
          {movie.duration > 0 && <span>{movie.duration} min</span>}
        </div>
        <p className="max-w-xl text-sm text-ink-muted line-clamp-2 md:text-base">
          {localized(movie.description, locale)}
        </p>
        <div className="mt-1 flex items-center gap-3">
          <Link
            href={`/watch/${movie.id}`}
            className="inline-flex h-11 items-center gap-2 rounded-lg bg-brand-500 px-6 text-sm font-semibold text-white transition-colors hover:bg-brand-600"
          >
            <Play className="h-4 w-4 fill-current" aria-hidden />
            {dict.play}
          </Link>
          <Link
            href={`/movie/${movie.id}`}
            className="inline-flex h-11 items-center gap-2 rounded-lg border border-surface-border bg-surface/60 px-6 text-sm font-semibold text-ink transition-colors hover:bg-surface-hover"
          >
            <Info className="h-4 w-4" aria-hidden />
            {dict.details}
          </Link>
        </div>
      </div>
    </section>
  );
}
