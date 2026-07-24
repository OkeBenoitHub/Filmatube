import Link from "next/link";
import { HScroller } from "@/components/catalog/HScroller";
import type { FeaturedCollection } from "@/lib/collections";

/**
 * The Home marquee of admin-curated editorial collections — wide cover cards that link to the
 * collection page. Server-fetched and passed in (editorial content changes rarely), so unlike
 * the movie rows it doesn't need a live subscription.
 */
export function FeaturedCollections({
  title,
  collections,
  movieCountLabel,
}: {
  title: string;
  collections: FeaturedCollection[];
  /** "{n} movies" template. */
  movieCountLabel: string;
}) {
  if (collections.length === 0) return null;

  return (
    <section className="space-y-3">
      <h2 className="px-4 text-lg font-semibold text-ink md:px-6">{title}</h2>
      <HScroller>
        {collections.map((col) => (
          <Link
            key={col.id}
            href={`/collections/${col.id}`}
            className="group relative aspect-video w-72 shrink-0 snap-start overflow-hidden rounded-xl border border-surface-border bg-surface-hover md:w-80"
          >
            {col.coverUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={col.coverUrl}
                alt=""
                loading="lazy"
                className="absolute inset-0 h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
              />
            ) : (
              // No cover: a poster stack from the collection's first titles.
              <div className="absolute inset-0 flex">
                {col.posters.map((p) => (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img key={p} src={p} alt="" className="h-full flex-1 object-cover opacity-80" />
                ))}
              </div>
            )}
            {/* Legible caption over any artwork. */}
            <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/85 via-black/40 to-transparent p-3">
              <p className="truncate text-base font-bold text-white">{col.title}</p>
              {col.subtitle ? (
                <p className="truncate text-xs text-white/80">{col.subtitle}</p>
              ) : (
                <p className="truncate text-xs text-white/70">{movieCountLabel.replace("{n}", String(col.movieCount))}</p>
              )}
            </div>
          </Link>
        ))}
      </HScroller>
    </section>
  );
}
