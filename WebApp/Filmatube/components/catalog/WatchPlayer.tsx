"use client";

import { useCallback, useEffect, useState } from "react";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/**
 * Fetches a short-lived, token-protected R2 playback URL from /api/stream/[id]
 * and plays it in an HTML5 <video>. Day 50 uses native controls; a custom
 * control bar (Day 51) and watch-progress sync (Day 52) build on this.
 */
export function WatchPlayer({
  movieId,
  poster,
  dict,
}: {
  movieId: string;
  poster: string;
  dict: Dictionary["player"];
}) {
  const [src, setSrc] = useState<string | null>(null);
  const [error, setError] = useState(false);

  const load = useCallback(async () => {
    setError(false);
    setSrc(null);
    try {
      const res = await fetch(`/api/stream/${movieId}`);
      if (!res.ok) throw new Error("stream");
      const data = (await res.json()) as { url: string };
      setSrc(data.url);
    } catch {
      setError(true);
    }
  }, [movieId]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="flex min-h-screen items-center justify-center">
      {error ? (
        <div className="flex flex-col items-center gap-3 text-center">
          <p className="text-ink-muted">{dict.error}</p>
          <button
            type="button"
            onClick={load}
            className="h-10 rounded-lg bg-brand-500 px-5 text-sm font-semibold text-white hover:bg-brand-600"
          >
            {dict.retry}
          </button>
        </div>
      ) : src ? (
        // eslint-disable-next-line jsx-a11y/media-has-caption
        <video
          src={src}
          poster={poster || undefined}
          controls
          autoPlay
          className="max-h-screen w-full"
        />
      ) : (
        <p className="animate-pulse text-ink-muted">{dict.loading}</p>
      )}
    </div>
  );
}
