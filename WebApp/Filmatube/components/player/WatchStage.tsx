"use client";

import { useEffect } from "react";
import { useMiniPlayer } from "@/components/player/MiniPlayerProvider";

/**
 * Registers the movie as the active player on mount; the PersistentPlayer (root
 * layout) renders the actual full-screen video over this page. Leaving the route
 * turns it into the mini-player.
 */
export function WatchStage({
  movieId,
  poster,
  title,
}: {
  movieId: string;
  poster: string;
  title: string;
}) {
  const { open } = useMiniPlayer();

  useEffect(() => {
    open({ id: movieId, poster, title });
  }, [open, movieId, poster, title]);

  return <div className="min-h-screen bg-black" />;
}
