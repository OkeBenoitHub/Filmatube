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
}: {
  movieId: string;
  poster: string;
  title: string;
  subtitles: ActiveSubtitle[];
  upNext: UpNextMovie | null;
}) {
  const { open } = useMiniPlayer();

  useEffect(() => {
    open({ id: movieId, poster, title, subtitles, upNext });
  }, [open, movieId, poster, title, subtitles, upNext]);

  return <div className="min-h-screen bg-black" />;
}
