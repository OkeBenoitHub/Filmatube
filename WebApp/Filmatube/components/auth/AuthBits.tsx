import type { ReactNode } from "react";

export function AuthDivider({ children }: { children: ReactNode }) {
  return (
    <div className="my-5 flex items-center gap-3">
      <div className="h-px flex-1 bg-surface-border" />
      <span className="text-xs text-ink-faint">{children}</span>
      <div className="h-px flex-1 bg-surface-border" />
    </div>
  );
}

export function ErrorBanner({ children }: { children: ReactNode }) {
  return (
    <div className="rounded-lg bg-red-500/15 px-3 py-2 text-sm text-red-300">{children}</div>
  );
}
