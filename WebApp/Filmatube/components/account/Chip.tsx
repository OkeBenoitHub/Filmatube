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
        "rounded-full border px-3 py-1.5 text-sm transition-colors",
        selected
          ? "border-brand-500 bg-brand-500/15 text-ink"
          : "border-surface-border text-ink-muted hover:text-ink",
      )}
    >
      {label}
    </button>
  );
}
