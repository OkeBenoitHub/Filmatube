"use client";

import { useState } from "react";
import { Share2, Check, Smartphone } from "lucide-react";
import type { Dictionary } from "@/lib/i18n/dictionaries";

const ANDROID_PACKAGE = "com.filmatube.app";
const PLAY_STORE = `https://play.google.com/store/apps/details?id=${ANDROID_PACKAGE}`;

/** Android deep link that opens the app at the movie, falling back to the Play Store. */
function appIntentUrl(movieId: string): string {
  const fallback = encodeURIComponent(PLAY_STORE);
  return `intent://movie/${movieId}#Intent;scheme=filmatube;package=${ANDROID_PACKAGE};S.browser_fallback_url=${fallback};end`;
}

/** Share a movie link (Web Share API → clipboard fallback) + "Open in app" deep link. */
export function ShareBar({
  movieId,
  title,
  dict,
}: {
  movieId: string;
  title: string;
  dict: Dictionary["catalog"];
}) {
  const [copied, setCopied] = useState(false);

  const share = async () => {
    const url = `${window.location.origin}/movie/${movieId}`;
    if (navigator.share) {
      try {
        await navigator.share({ title, url });
        return;
      } catch {
        /* cancelled — fall through to copy */
      }
    }
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      /* ignore */
    }
  };

  return (
    <>
      <button
        type="button"
        onClick={share}
        className="inline-flex h-11 items-center gap-2 rounded-lg border border-surface-border px-6 text-sm font-semibold text-ink transition-colors hover:bg-surface-hover"
      >
        {copied ? <Check className="h-4 w-4 text-brand-400" aria-hidden /> : <Share2 className="h-4 w-4" aria-hidden />}
        {copied ? dict.copied : dict.share}
      </button>
      <a
        href={appIntentUrl(movieId)}
        className="inline-flex h-11 items-center gap-2 rounded-lg border border-surface-border px-6 text-sm font-semibold text-ink transition-colors hover:bg-surface-hover"
      >
        <Smartphone className="h-4 w-4" aria-hidden />
        {dict.openInApp}
      </a>
    </>
  );
}
