import { notFound } from "next/navigation";
import { MovieForm } from "@/components/admin/MovieForm";
import { emptyMovie, type MovieFormValues } from "@/lib/admin/movie-form";
import { getMovieAdmin } from "@/lib/admin/movies";
import { getDict } from "@/lib/i18n/server";

export default async function EditMoviePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const dict = await getDict();
  const movie = await getMovieAdmin(id);
  if (!movie) notFound();

  const title = (movie.title as { en?: string; fr?: string }) ?? {};
  const description = (movie.description as { en?: string; fr?: string }) ?? {};
  const initial: MovieFormValues = {
    ...emptyMovie,
    imdbId: (movie.imdbId as string) ?? "",
    tmdbId: (movie.tmdbId as string) ?? "",
    titleEn: title.en ?? "",
    titleFr: title.fr ?? "",
    descEn: description.en ?? "",
    descFr: description.fr ?? "",
    year: (movie.year as number) ?? emptyMovie.year,
    duration: (movie.duration as number) ?? 0,
    ageRating: (movie.ageRating as string) ?? "",
    genres: (movie.genres as string[]) ?? [],
    directors: (movie.directors as string[]) ?? [],
    cast: (movie.cast as MovieFormValues["cast"]) ?? [],
    posterUrl: (movie.posterUrl as string) ?? "",
    backdropUrl: (movie.backdropUrl as string) ?? "",
    trailerUrl: (movie.trailerUrl as string) ?? "",
    videoKey: (movie.videoKey as string) ?? "",
    subtitleTracks: (movie.subtitleTracks as MovieFormValues["subtitleTracks"]) ?? [],
    status: ((movie.status as string) === "published" ? "published" : "draft"),
    isFeatured: !!movie.isFeatured,
    isPinned: !!movie.isPinned,
    isComingSoon: !!movie.isComingSoon,
  };

  return <MovieForm movieId={id} initial={initial} dict={dict.adminMovies} genres={dict.genres} />;
}
