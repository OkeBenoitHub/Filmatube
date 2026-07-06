import type { ReactNode } from "react";
import { Wordmark } from "@/components/Wordmark";

/** Centered auth card: logo + title/subtitle + form content. */
export function AuthShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <div className="flex min-h-screen items-center justify-center px-6 py-12">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center gap-3 text-center">
          <Wordmark />
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-ink">{title}</h1>
            <p className="mt-1 text-sm text-ink-muted">{subtitle}</p>
          </div>
        </div>
        {children}
      </div>
    </div>
  );
}
