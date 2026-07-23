"use client";

import { cn } from "@/lib/utils";

/** Selectable pill used by the taste picker. */
export function Chip({
  label,
  selected,
  onClick,
}: {
  label: string;
  selected: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={selected}
      className={cn(
        // Sized to mirror the Android Material 3 FilterChip: 32px tall, 8px corners (not a
        // full pill), compact label — the web pill read noticeably chunkier next to it.
        "inline-flex h-8 items-center whitespace-nowrap rounded-lg border px-3 text-[13px] transition-colors",
        selected
          ? "border-brand-500 bg-brand-500/15 text-ink"
          : "border-surface-border text-ink-muted hover:text-ink",
      )}
    >
      {label}
    </button>
  );
}
