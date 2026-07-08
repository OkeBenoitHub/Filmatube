import { redirect, notFound } from "next/navigation";
import { WatchStage } from "@/components/player/WatchStage";
import { getCurrentUser } from "@/lib/auth/session";
import { getLocale } from "@/lib/i18n/server";
import { getMovie, localized } from "@/lib/movies";

/**
 * Full-screen web player route. The actual <video> is rendered by the persistent
 * player in the root layout (so it survives navigation as a mini-player); this
 * page gates access and registers the active movie.
 */
export default async function WatchMoviePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const user = await getCurrentUser();
  if (!user) redirect(`/login?next=/watch/${id}`);

  const [locale, movie] = await Promise.all([getLocale(), getMovie(id)]);
  if (!movie || movie.isComingSoon) notFound();

  return (
    <WatchStage
      movieId={id}
      poster={movie.backdropUrl || movie.posterUrl}
      title={localized(movie.title, locale)}
    />
  );
}
