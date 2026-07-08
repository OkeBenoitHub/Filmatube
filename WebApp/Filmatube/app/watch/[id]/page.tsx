import Link from "next/link";
import { redirect, notFound } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { WatchPlayer } from "@/components/catalog/WatchPlayer";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict, getLocale } from "@/lib/i18n/server";
import { getMovie, localized } from "@/lib/movies";

/** Full-screen web player. Streams the token-protected R2 video via /api/stream/[id]. */
export default async function WatchMoviePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const user = await getCurrentUser();
  if (!user) redirect(`/login?next=/watch/${id}`);

  const [locale, dict, movie] = await Promise.all([getLocale(), getDict(), getMovie(id)]);
  if (!movie || movie.isComingSoon) notFound();
  const title = localized(movie.title, locale);

  return (
    <div className="relative min-h-screen bg-black">
      <Link
        href={`/movie/${id}`}
        aria-label={dict.player.back}
        className="absolute left-4 top-4 z-20 inline-flex h-10 items-center gap-2 rounded-lg bg-black/50 px-3 text-sm font-medium text-white backdrop-blur transition-colors hover:bg-black/70"
      >
        <ArrowLeft className="h-4 w-4" aria-hidden />
        <span className="max-w-[60vw] truncate">{title}</span>
      </Link>
      <WatchPlayer movieId={id} poster={movie.backdropUrl || movie.posterUrl} dict={dict.player} />
    </div>
  );
}
