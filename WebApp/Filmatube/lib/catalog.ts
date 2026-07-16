// Pure catalog domain: types, the doc→model mapper and the row selectors.
// NO server-only imports — this module is shared by server code (lib/movies.ts,
// via the Admin SDK) and client components (HomeClient, via the client SDK), so
// both platforms derive rows from identical logic.
import type { Locale } from "@/lib/i18n/config";

export interface CatalogCast {
  name: string;
  character: string;
  photoUrl: string;
}

export interface CatalogSubtitle {
  lang: string;
  url: string;
}

export interface CatalogMovie {
  id: string;
  title: { en: string; fr: string };
  description: { en: string; fr: string };
  posterUrl: string;
  backdropUrl: string;
  trailerUrl: string;
  genres: string[];
  year: number;
  duration: number;
  ageRating: string;
  cast: CatalogCast[];
  directors: string[];
  averageRating: number;
  ratingsCount: number;
  viewsCount: number;
  isFeatured: boolean;
  isComingSoon: boolean;
  hasVideo: boolean;
  subtitleTracks: CatalogSubtitle[];
  addedAtMs: number;
}

export interface ContinueWatchingItem {
  movie: CatalogMovie;
  progress: number;
}

/** Pick the copy for the current locale, falling back to the other language. */
export function localized(text: { en: string; fr: string } | undefined, locale: Locale): string {
  if (!text) return "";
  if (locale === "fr") return text.fr || text.en;
  return text.en || text.fr;
}

/** Maps a raw movie document (Admin or client SDK — both yield plain data) to the model. */
export function mapMovieDoc(id: string, x: Record<string, unknown>): CatalogMovie {
  const title = (x.title as { en?: string; fr?: string }) ?? {};
  const description = (x.description as { en?: string; fr?: string }) ?? {};
  return {
    id,
    title: { en: title.en ?? "", fr: title.fr ?? "" },
    description: { en: description.en ?? "", fr: description.fr ?? "" },
    posterUrl: (x.posterUrl as string) ?? "",
    backdropUrl: (x.backdropUrl as string) ?? "",
    trailerUrl: (x.trailerUrl as string) ?? "",
    genres: (x.genres as string[]) ?? [],
    year: (x.year as number) ?? 0,
    duration: (x.duration as number) ?? 0,
    ageRating: (x.ageRating as string) ?? "",
    cast: ((x.cast as CatalogCast[]) ?? []).map((c) => ({
      name: c?.name ?? "",
      character: c?.character ?? "",
      photoUrl: c?.photoUrl ?? "",
    })),
    directors: (x.directors as string[]) ?? [],
    averageRating: (x.averageRating as number) ?? 0,
    ratingsCount: (x.ratingsCount as number) ?? 0,
    viewsCount: (x.viewsCount as number) ?? 0,
    isFeatured: !!x.isFeatured,
    isComingSoon: !!x.isComingSoon,
    hasVideo: Boolean(x.videoKey),
    subtitleTracks: ((x.subtitleTracks as CatalogSubtitle[]) ?? [])
      .filter((t) => t?.lang && t?.url)
      .map((t) => ({ lang: t.lang, url: t.url })),
    addedAtMs:
      (x.addedAt as { toMillis?: () => number })?.toMillis?.() ??
      (x.updatedAt as { toMillis?: () => number })?.toMillis?.() ??
      0,
  };
}

// --- pure row selectors (operate on the published catalog, newest first) ---

export const pickFeatured = (m: CatalogMovie[]) => m.filter((x) => x.isFeatured && !x.isComingSoon);

export const pickTrending = (m: CatalogMovie[]) =>
  [...m].filter((x) => !x.isComingSoon).sort((a, b) => b.viewsCount - a.viewsCount).slice(0, 20);

export const pickNewReleases = (m: CatalogMovie[]) => m.filter((x) => !x.isComingSoon).slice(0, 20);

export const pickComingSoon = (m: CatalogMovie[]) => m.filter((x) => x.isComingSoon);

export const pickByGenre = (m: CatalogMovie[], genre: string) =>
  m.filter((x) => !x.isComingSoon && x.genres.includes(genre));

export function pickRelated(m: CatalogMovie[], movie: CatalogMovie): CatalogMovie[] {
  return m
    .filter((x) => x.id !== movie.id && !x.isComingSoon && x.genres.some((g) => movie.genres.includes(g)))
    .slice(0, 15);
}

export function searchMovies(m: CatalogMovie[], query: string): CatalogMovie[] {
  const q = query.trim().toLowerCase();
  if (!q) return [];
  return m
    .filter(
      (x) =>
        x.title.en.toLowerCase().includes(q) ||
        x.title.fr.toLowerCase().includes(q) ||
        x.directors.some((d) => d.toLowerCase().includes(q)) ||
        x.cast.some((c) => c.name.toLowerCase().includes(q)),
    )
    .slice(0, 40);
}
