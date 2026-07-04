"use client";

import { locales } from "@/lib/i18n/config";
import { useI18n } from "@/components/providers/LocaleProvider";
import { cn } from "@/lib/utils";

/** Segmented EN/FR toggle — persists via cookie and refreshes server-rendered copy. */
export function LanguageSwitcher({ className }: { className?: string }) {
  const { locale, setLocale, dict } = useI18n();

  return (
    <div
      role="group"
      aria-label={dict.common.language}
      className={cn(
        "flex items-center gap-0.5 rounded-full border border-surface-border bg-surface-card p-0.5",
        className,
      )}
    >
      {locales.map((l) => (
        <button
          key={l}
          type="button"
          onClick={() => setLocale(l)}
          aria-pressed={l === locale}
          className={cn(
            "rounded-full px-2.5 py-1 text-xs font-semibold uppercase transition-colors",
            l === locale
              ? "bg-brand-500 text-white"
              : "text-ink-muted hover:text-ink",
          )}
        >
          {l}
        </button>
      ))}
    </div>
  );
}
