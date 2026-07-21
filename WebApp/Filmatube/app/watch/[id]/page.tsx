import { redirect, notFound } from "next/navigation";
import { WatchStage } from "@/components/player/WatchStage";
import { getCurrentUser } from "@/lib/auth/session";
import { getLocale } from "@/lib/i18n/server";
import { getMovie, getPublishedMovies, localized, pickRelated } from "@/lib/movies";
import { getParty } from "@/lib/parties";
import { getShowtime, isOpen } from "@/lib/theater";

/**
 * Full-screen web player route. The actual <video> is rendered by the persistent
 * player in the root layout (so it survives navigation as a mini-player); this
 * page gates access and registers the active movie.
 */
export default async function WatchMoviePage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const [{ id }, sp] = await Promise.all([params, searchParams]);
  const user = await getCurrentUser();
  if (!user) redirect(`/login?next=/watch/${id}`);

  // Only enable the sync engine for a party this viewer is actually in — otherwise a
  // guessed ?party= would let a stranger drive (or spy on) someone else's room.
  const requestedParty = typeof sp.party === "string" ? sp.party : null;
  const party = requestedParty ? await getParty(requestedParty, user.uid) : null;
  const partyId = party && party.status === "live" && party.movieId === id ? party.id : null;

  // Unlike a party, a showtime is public — no membership check, just that the room is
  // actually open and actually for this movie (a guessed ?showtime= for another film
  // shouldn't silently attach the wrong sync engine).
  //
  // Only one engine may drive the playhead: `?party=X&showtime=Y` would otherwise attach
  // both, and they'd fight over currentTime every few seconds. The party wins because it is
  // membership-checked and private, so it's the more specific claim on this viewer.
  const requestedShowtime = partyId ? null : typeof sp.showtime === "string" ? sp.showtime : null;
  const showtime = requestedShowtime ? await getShowtime(requestedShowtime) : null;
  const showtimeId = showtime && showtime.movieId === id && isOpen(showtime) ? showtime.id : null;
  const theaterStartAtMs = showtime?.startAtMs ?? 0;

  const [locale, movie] = await Promise.all([getLocale(), getMovie(id)]);
  if (!movie || movie.isComingSoon) notFound();

  // Recommended "Up Next" title (related → newest fallback).
  const all = await getPublishedMovies();
  const next = pickRelated(all, movie)[0] ?? all.find((m) => m.id !== id && !m.isComingSoon) ?? null;
  const upNext = next
    ? { id: next.id, poster: next.posterUrl, title: localized(next.title, locale) }
    : null;

  return (
    <WatchStage
      movieId={id}
      poster={movie.backdropUrl || movie.posterUrl}
      title={localized(movie.title, locale)}
      subtitles={movie.subtitleTracks}
      upNext={upNext}
      partyId={partyId}
      showtimeId={showtimeId}
      theaterStartAtMs={theaterStartAtMs}
    />
  );
}
