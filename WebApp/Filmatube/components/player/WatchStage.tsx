"use client";

import { useEffect } from "react";
import { useMiniPlayer, type ActiveSubtitle, type UpNextMovie } from "@/components/player/MiniPlayerProvider";

/**
 * Registers the movie as the active player on mount; the PersistentPlayer (root
 * layout) renders the actual full-screen video over this page. Leaving the route
 * turns it into the mini-player.
 */
export function WatchStage({
  movieId,
  poster,
  title,
  subtitles,
  upNext,
  partyId = null,
  showtimeId = null,
  theaterStartAtMs = 0,
}: {
  movieId: string;
  poster: string;
  title: string;
  subtitles: ActiveSubtitle[];
  upNext: UpNextMovie | null;
  /** Set when arriving from a live party lobby (`/watch/[id]?party=…`). */
  partyId?: string | null;
  /** Set when arriving from an open showtime lobby (`/watch/[id]?showtime=…`). */
  showtimeId?: string | null;
  theaterStartAtMs?: number;
}) {
  const { open } = useMiniPlayer();

  useEffect(() => {
    open({ id: movieId, poster, title, subtitles, upNext, partyId, showtimeId, theaterStartAtMs });
  }, [open, movieId, poster, title, subtitles, upNext, partyId, showtimeId, theaterStartAtMs]);

  return <div className="min-h-screen bg-black" />;
}
