"use client";

import { useState } from "react";
import { Check, Download, Share2 } from "lucide-react";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/**
 * The shareable stats card: a preview of the generated PNG plus Share / Download. Web Share is
 * used when available (it can attach the actual image file); otherwise the link is copied.
 */
export function StatsCard({ uid, dict }: { uid: string; dict: Dictionary["catalog"] }) {
  const [copied, setCopied] = useState(false);
  const path = `/api/stats-card/${uid}`;

  const share = async () => {
    const url = `${window.location.origin}${path}`;
    try {
      // Share the image itself where the browser supports files; fall back to the URL.
      const file = await fetch(path)
        .then((r) => r.blob())
        .then((b) => new File([b], "filmatube-stats.png", { type: "image/png" }))
        .catch(() => null);

      if (file && navigator.canShare?.({ files: [file] })) {
        await navigator.share({ files: [file], title: dict.statsCardTitle });
        return;
      }
      if (navigator.share) {
        await navigator.share({ title: dict.statsCardTitle, url });
        return;
      }
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* dismissed or blocked — nothing to recover */
    }
  };

  return (
    <section className="space-y-3">
      <h2 className="text-lg font-semibold text-ink">{dict.statsCardTitle}</h2>
      <div className="overflow-hidden rounded-2xl border border-surface-border">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={path} alt={dict.statsCardTitle} className="block w-full" />
      </div>
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={share}
          className="inline-flex h-10 items-center gap-2 rounded-lg bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-500"
        >
          {copied ? <Check className="h-4 w-4" aria-hidden /> : <Share2 className="h-4 w-4" aria-hidden />}
          {copied ? dict.copied : dict.share}
        </button>
        <a
          href={path}
          download="filmatube-stats.png"
          className="inline-flex h-10 items-center gap-2 rounded-lg border border-surface-border px-4 text-sm font-semibold text-ink hover:bg-surface-hover"
        >
          <Download className="h-4 w-4" aria-hidden />
          {dict.statsCardDownload}
        </a>
      </div>
    </section>
  );
}
