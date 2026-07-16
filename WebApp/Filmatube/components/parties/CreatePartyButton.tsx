"use client";

import { useState, useTransition } from "react";
import { MonitorPlay } from "lucide-react";
import { createParty } from "@/app/parties/actions";
import { Modal } from "@/components/ui/Modal";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

/** Start options, in minutes from now — mirrors the Android PartyStart enum. */
const OPTIONS = [0, 30, 60, 120];

/**
 * "Watch party" on the movie page: pick when it starts, then the action creates the room and
 * redirects to its lobby in the same round-trip.
 */
export function CreatePartyButton({ movieId, dict }: { movieId: string; dict: Dictionary["catalog"] }) {
  const [open, setOpen] = useState(false);
  const [start, setStart] = useState(0);
  const [error, setError] = useState(false);
  const [pending, startTransition] = useTransition();

  const labels: Record<number, string> = {
    0: dict.partyStartNow,
    30: dict.partyStart30m,
    60: dict.partyStart1h,
    120: dict.partyStart2h,
  };

  const create = () => {
    setError(false);
    startTransition(async () => {
      try {
        await createParty(movieId, start);
      } catch (e) {
        // redirect() throws NEXT_REDIRECT — that's success, not failure.
        if (typeof e === "object" && e !== null && "digest" in e) throw e;
        setError(true);
      }
    });
  };

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="inline-flex h-11 items-center gap-2 whitespace-nowrap rounded-lg border border-surface-border px-6 text-sm font-semibold text-ink transition-colors hover:bg-surface-hover"
      >
        <MonitorPlay className="h-4 w-4" aria-hidden />
        {dict.partyCreateAction}
      </button>

      <Modal open={open} onClose={() => setOpen(false)}>
        <h2 className="text-lg font-bold text-ink">{dict.partyCreateTitle}</h2>
        <p className="mt-1 text-sm text-ink-muted">{dict.partyCreateSubtitle}</p>

        <p className="mt-5 text-xs font-semibold uppercase tracking-wide text-ink-muted">{dict.partyWhen}</p>
        <div className="mt-2 flex flex-wrap gap-2">
          {OPTIONS.map((o) => (
            <button
              key={o}
              type="button"
              onClick={() => setStart(o)}
              aria-pressed={start === o}
              className={cn(
                "rounded-full border px-4 py-1.5 text-sm font-semibold transition-colors",
                start === o
                  ? "border-brand-500 bg-brand-500 text-white"
                  : "border-surface-border text-ink-muted hover:bg-surface-hover hover:text-ink",
              )}
            >
              {labels[o]}
            </button>
          ))}
        </div>

        {error && <p className="mt-4 text-sm text-red-400">{dict.partyCreateError}</p>}

        <div className="mt-6 flex gap-2">
          <button
            type="button"
            onClick={create}
            disabled={pending}
            className="h-10 flex-1 whitespace-nowrap rounded-lg bg-brand-500 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
          >
            {dict.partyCreateButton}
          </button>
          <button
            type="button"
            onClick={() => setOpen(false)}
            className="h-10 whitespace-nowrap rounded-lg border border-surface-border px-4 text-sm font-medium text-ink hover:bg-surface-hover"
          >
            {dict.closeLabel}
          </button>
        </div>
      </Modal>
    </>
  );
}
