import { BADGE_EMOJI, BADGE_IDS } from "@/lib/badges";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/**
 * The achievements grid: every badge, earned ones in full colour and locked ones dimmed with a
 * lock overlay. Data is read-only (awarded by Cloud Functions); this just renders state.
 */
export function Badges({ earned, dict }: { earned: Set<string>; dict: Dictionary["catalog"] }) {
  return (
    <section className="space-y-3">
      <div className="flex items-baseline justify-between gap-3">
        <h2 className="text-lg font-semibold text-ink">{dict.badgesTitle}</h2>
        <span className="text-sm text-ink-muted">
          {dict.badgesEarned.replace("{n}", String(earned.size)).replace("{total}", String(BADGE_IDS.length))}
        </span>
      </div>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {BADGE_IDS.map((id) => {
          const has = earned.has(id);
          const meta = dict.badgeMeta[id];
          return (
            <div
              key={id}
              className={`flex items-center gap-3 rounded-2xl border p-3 ${
                has ? "border-brand-700/50 bg-brand-700/10" : "border-surface-border bg-surface-card"
              }`}
            >
              <span
                className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl text-xl ${
                  has ? "bg-brand-600/20" : "grayscale"
                }`}
                aria-hidden
              >
                {has ? BADGE_EMOJI[id] : "🔒"}
              </span>
              <div className="min-w-0">
                <p className={`truncate text-sm font-semibold ${has ? "text-ink" : "text-ink-muted"}`}>{meta.name}</p>
                <p className="truncate text-xs text-ink-faint">{meta.desc}</p>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
