import { MoviesTable } from "@/components/admin/MoviesTable";
import { listMovies } from "@/lib/admin/movies";
import { getDict } from "@/lib/i18n/server";

export default async function AdminMoviesPage() {
  const dict = await getDict();
  const movies = await listMovies();
  return <MoviesTable movies={movies} dict={dict.adminMovies} heading={dict.admin.movies} />;
}
