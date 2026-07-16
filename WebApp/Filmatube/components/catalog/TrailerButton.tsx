"use client";

import { useState } from "react";
import { Film, X } from "lucide-react";
import { Modal } from "@/components/ui/Modal";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Extract an 11-char YouTube id from watch / youtu.be / embed / shorts URLs. */
function youtubeId(url: string): string | null {
  const patterns = [
    /youtube\.com\/watch\?(?:.*&)?v=([\w-]{11})/,
    /youtu\.be\/([\w-]{11})/,
    /youtube\.com\/embed\/([\w-]{11})/,
    /youtube\.com\/shorts\/([\w-]{11})/,
  ];
  for (const p of patterns) {
    const m = url.match(p);
    if (m) return m[1];
  }
  return null;
}

/**
 * Trailer button that opens a clean in-page dialog with the trailer playing inside — a YouTube
 * embed when the URL is YouTube, otherwise an inline HTML5 video. The Modal unmounts its children
 * when closed, so the video only loads (and autoplays) while open and stops the moment it closes.
 */
export function TrailerButton({ url, dict }: { url: string; dict: Dictionary["catalog"] }) {
  const [open, setOpen] = useState(false);
  const ytId = youtubeId(url);

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="inline-flex h-11 items-center gap-2 rounded-lg border border-surface-border px-6 text-sm font-semibold text-ink transition-colors hover:bg-surface-hover"
      >
        <Film className="h-4 w-4" aria-hidden />
        {dict.trailer}
      </button>

      <Modal open={open} onClose={() => setOpen(false)} className="max-w-3xl overflow-hidden p-0">
        <div className="flex items-center justify-between border-b border-surface-border px-4 py-3">
          <span className="text-sm font-semibold text-ink">{dict.trailer}</span>
          <button
            type="button"
            onClick={() => setOpen(false)}
            aria-label={dict.closeLabel}
            className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-muted transition-colors hover:bg-surface-hover hover:text-ink"
          >
            <X className="h-4 w-4" aria-hidden />
          </button>
        </div>
        <div className="relative aspect-video w-full bg-black">
          {ytId ? (
            <iframe
              src={`https://www.youtube.com/embed/${ytId}?autoplay=1&rel=0&modestbranding=1`}
              title={dict.trailer}
              allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
              allowFullScreen
              className="absolute inset-0 h-full w-full border-0"
            />
          ) : (
            // eslint-disable-next-line jsx-a11y/media-has-caption
            <video src={url} controls autoPlay className="absolute inset-0 h-full w-full" />
          )}
        </div>
      </Modal>
    </>
  );
}
