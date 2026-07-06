import { MovieForm } from "@/components/admin/MovieForm";
import { emptyMovie } from "@/lib/admin/movie-form";
import { getDict } from "@/lib/i18n/server";

export default async function NewMoviePage() {
  const dict = await getDict();
  return <MovieForm movieId={null} initial={emptyMovie} dict={dict.adminMovies} genres={dict.genres} />;
}
