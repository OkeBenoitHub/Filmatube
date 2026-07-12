"use client";

import { useTransition } from "react";
import Link from "next/link";
import { Check, X, Trash2 } from "lucide-react";
import { setReportStatus, removeReportedContent } from "@/app/admin/reports/actions";
import type { ReportRow } from "@/lib/admin/reports";
import type { Dictionary } from "@/lib/i18n/dictionaries";

export function ReportsTable({ reports, dict }: { reports: ReportRow[]; dict: Dictionary["admin"] }) {
  const [pending, startTransition] = useTransition();

  if (reports.length === 0) {
    return <p className="py-12 text-center text-ink-muted">{dict.reportsEmpty}</p>;
  }

  const statusLabel = (s: string) =>
    s === "resolved" ? dict.reportStatusResolved : s === "dismissed" ? dict.reportStatusDismissed : dict.reportStatusPending;

  return (
    <div className="space-y-3">
      {reports.map((r) => (
        <div key={r.id} className="rounded-xl border border-surface-border p-4">
          <div className="flex flex-wrap items-center gap-2 text-xs text-ink-muted">
            <span className="rounded bg-surface-hover px-2 py-0.5 font-medium uppercase">{r.type}</span>
            <span
              className={
                r.status === "pending"
                  ? "rounded bg-amber-500/15 px-2 py-0.5 font-medium text-amber-400"
                  : "rounded bg-surface-hover px-2 py-0.5"
              }
            >
              {statusLabel(r.status)}
            </span>
            {r.movieId && (
              <Link href={`/movie/${r.movieId}`} className="text-brand-400 hover:underline">
                /movie/{r.movieId}
              </Link>
            )}
            <Link href={`/u/${r.reportedUserId}`} className="hover:underline">
              @{r.reportedUserId.slice(0, 8)}
            </Link>
          </div>

          {r.targetText && <p className="mt-2 rounded-lg bg-surface-hover px-3 py-2 text-sm text-ink">{r.targetText}</p>}

          {r.status === "pending" && (
            <div className="mt-3 flex flex-wrap gap-2">
              <button
                type="button"
                disabled={pending}
                onClick={() => startTransition(() => setReportStatus(r.id, "resolved"))}
                className="inline-flex items-center gap-1.5 rounded-lg border border-surface-border px-3 py-1.5 text-sm text-ink hover:bg-surface-hover disabled:opacity-50"
              >
                <Check className="h-4 w-4" aria-hidden />
                {dict.reportResolve}
              </button>
              <button
                type="button"
                disabled={pending}
                onClick={() => startTransition(() => setReportStatus(r.id, "dismissed"))}
                className="inline-flex items-center gap-1.5 rounded-lg border border-surface-border px-3 py-1.5 text-sm text-ink-muted hover:bg-surface-hover disabled:opacity-50"
              >
                <X className="h-4 w-4" aria-hidden />
                {dict.reportDismiss}
              </button>
              <button
                type="button"
                disabled={pending}
                onClick={() => startTransition(() => removeReportedContent(r.id, r.type, r.movieId, r.targetId))}
                className="inline-flex items-center gap-1.5 rounded-lg border border-red-500/40 px-3 py-1.5 text-sm text-red-400 hover:bg-red-500/10 disabled:opacity-50"
              >
                <Trash2 className="h-4 w-4" aria-hidden />
                {dict.reportDelete}
              </button>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
