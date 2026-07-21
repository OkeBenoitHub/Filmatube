import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";

/**
 * Page hero in Filmatube green: a gradient icon tile beside an uppercase eyebrow, a heavy
 * title, an optional subtitle and an actions slot.
 *
 * Sized so the header introduces the page without owning it — an earlier pass ran to a 192px
 * tile and a 60px title, which pushed the actual content of every page below the fold.
 */
export function PageHero({
  icon: Icon,
  eyebrow,
  title,
  subtitle,
  children,
}: {
  icon: LucideIcon;
  eyebrow: string;
  title: string;
  subtitle?: string;
  children?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-end">
      <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-900 shadow-xl shadow-brand-900/40 sm:h-28 sm:w-28">
        <Icon className="h-10 w-10 text-white sm:h-12 sm:w-12" aria-hidden />
      </div>
      <div className="min-w-0 text-center sm:text-left">
        <p className="text-xs font-bold uppercase tracking-widest text-ink-muted">{eyebrow}</p>
        <h1 className="mt-1 text-3xl font-black leading-tight tracking-tight text-ink md:text-4xl">{title}</h1>
        {subtitle && <p className="mt-2 text-sm text-ink-muted">{subtitle}</p>}
        {children && <div className="mt-4 flex flex-wrap justify-center gap-2 sm:justify-start">{children}</div>}
      </div>
    </div>
  );
}
