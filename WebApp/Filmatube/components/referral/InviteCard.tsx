"use client";

import { useState } from "react";
import { Check, Copy, Share2 } from "lucide-react";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** The share panel on the invite dashboard: the link, plus Copy and (where supported) Share. */
export function InviteCard({ url, dict }: { url: string; dict: Dictionary["referral"] }) {
  const [copied, setCopied] = useState(false);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      /* clipboard blocked — the field is selectable as a fallback */
    }
  };

  const share = async () => {
    if (typeof navigator !== "undefined" && navigator.share) {
      try {
        await navigator.share({ title: dict.shareTitle, text: dict.shareText, url });
      } catch {
        /* dismissed */
      }
    } else {
      void copy();
    }
  };

  return (
    <div className="rounded-2xl border border-surface-border bg-surface-card p-5">
      <p className="text-sm font-medium text-ink-muted">{dict.yourLink}</p>
      <div className="mt-2 flex flex-col gap-2 sm:flex-row">
        <input
          readOnly
          value={url}
          onFocus={(e) => e.currentTarget.select()}
          className="h-11 flex-1 rounded-lg border border-surface-border bg-surface px-3 text-sm text-ink"
        />
        <div className="flex gap-2">
          <button
            type="button"
            onClick={copy}
            className="inline-flex h-11 items-center gap-2 rounded-lg border border-surface-border px-4 text-sm font-semibold text-ink hover:bg-surface-hover"
          >
            {copied ? <Check className="h-4 w-4 text-brand-400" aria-hidden /> : <Copy className="h-4 w-4" aria-hidden />}
            {copied ? dict.copied : dict.copy}
          </button>
          <button
            type="button"
            onClick={share}
            className="inline-flex h-11 items-center gap-2 rounded-lg bg-brand-600 px-4 text-sm font-semibold text-white hover:bg-brand-500"
          >
            <Share2 className="h-4 w-4" aria-hidden />
            {dict.share}
          </button>
        </div>
      </div>
    </div>
  );
}
