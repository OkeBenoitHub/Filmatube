import type { LucideIcon } from "lucide-react";

/**
 * Branded preview for a section that isn't built yet — radial green glow, tonal icon
 * medallion, headline + subtitle and a "Coming soon" pill. Mirrors the Android
 * FeaturePreviewScreen so both platforms share one design language.
 */
export function FeaturePreview({
  icon: Icon,
  title,
  subtitle,
  badge,
}: {
  icon: LucideIcon;
  title: string;
  subtitle: string;
  badge: string;
}) {
  return (
    <div className="relative flex min-h-[80vh] items-center justify-center overflow-hidden px-6">
      <div
        aria-hidden
        className="pointer-events-none absolute h-[420px] w-[420px] rounded-full bg-brand-500/10 blur-3xl"
      />
      <div className="relative flex max-w-md flex-col items-center gap-4 text-center">
        <div className="flex h-20 w-20 items-center justify-center rounded-full border border-surface-border bg-gradient-to-b from-surface-hover to-surface-card">
          <Icon className="h-8 w-8 text-brand-400" aria-hidden />
        </div>
        <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
        <p className="text-sm leading-relaxed text-ink-muted">{subtitle}</p>
        <span className="rounded-full border border-brand-700/50 bg-brand-700/25 px-3.5 py-1.5 text-xs font-medium text-brand-300">
          {badge}
        </span>
      </div>
    </div>
  );
}
