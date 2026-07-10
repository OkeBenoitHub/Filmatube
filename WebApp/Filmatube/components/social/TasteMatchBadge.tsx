import { Sparkles } from "lucide-react";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Colored pill showing genre-overlap % with the current user. Green = strong, amber = mild. */
export function TasteMatchBadge({
  percent,
  dict,
  size = "md",
}: {
  percent: number;
  dict: Dictionary["catalog"];
  size?: "sm" | "md";
}) {
  if (percent <= 0) return null;
  const strong = percent >= 50;
  const pad = size === "sm" ? "px-2 py-0.5 text-xs" : "px-3 py-1 text-sm";
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full font-semibold ${pad} ${
        strong ? "bg-brand-500/15 text-brand-400" : "bg-amber-500/15 text-amber-400"
      }`}
    >
      <Sparkles className={size === "sm" ? "h-3 w-3" : "h-3.5 w-3.5"} aria-hidden />
      {percent}% {dict.matchSuffix}
    </span>
  );
}
