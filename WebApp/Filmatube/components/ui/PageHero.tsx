import type { ReactNode } from "react";
import type { LucideIcon } from "lucide-react";

/**
 * Spotitube-style page hero in Filmatube green: large gradient icon tile beside an
 * uppercase eyebrow, a huge 900-weight title, an optional subtitle and an actions slot.
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
      <div className="flex h-36 w-36 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-900 shadow-2xl shadow-brand-900/50 sm:h-48 sm:w-48">
        <Icon className="h-16 w-16 text-white sm:h-20 sm:w-20" aria-hidden />
      </div>
      <div className="min-w-0 text-center sm:text-left">
        <p className="text-xs font-bold uppercase tracking-widest text-ink-muted">{eyebrow}</p>
        <h1 className="mt-1 text-4xl font-black leading-none tracking-tight text-ink md:text-6xl">{title}</h1>
        {subtitle && <p className="mt-2 text-sm text-ink-muted">{subtitle}</p>}
        {children && <div className="mt-4 flex flex-wrap justify-center gap-2 sm:justify-start">{children}</div>}
      </div>
    </div>
  );
}
