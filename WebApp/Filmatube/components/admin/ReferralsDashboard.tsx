"use client";

import { useTransition } from "react";
import { AlertTriangle } from "lucide-react";
import { Card } from "@/components/ui/Card";
import type { ReferralAnalytics } from "@/lib/admin/referrals";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { revokeReferral } from "@/app/admin/referrals/actions";

type Dict = Dictionary["adminReferrals"];

export function ReferralsDashboard({ analytics, dict }: { analytics: ReferralAnalytics; dict: Dict }) {
  const [pending, start] = useTransition();

  const stats: { value: number; label: string; alert?: boolean }[] = [
    { value: analytics.total, label: dict.statTotal },
    { value: analytics.last7, label: dict.statLast7 },
    { value: analytics.last30, label: dict.statLast30 },
    { value: analytics.referrerCount, label: dict.statReferrers },
    { value: analytics.suspiciousCount, label: dict.statFlagged, alert: analytics.suspiciousCount > 0 },
  ];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-ink">{dict.title}</h1>
        <p className="mt-1 text-sm text-ink-muted">{dict.subtitle}</p>
      </div>

      {/* Summary tiles — single headline numbers, so tiles not charts. */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
        {stats.map((s) => (
          <Card key={s.label} className="p-4 text-center">
            <p className={`text-3xl font-black tabular-nums ${s.alert ? "text-amber-400" : "text-ink"}`}>{s.value}</p>
            <p className="mt-1 text-xs text-ink-muted">{s.label}</p>
          </Card>
        ))}
      </div>

      {/* Top referrers */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold text-ink">{dict.topReferrers}</h2>
        {analytics.topReferrers.length === 0 ? (
          <p className="text-sm text-ink-muted">{dict.empty}</p>
        ) : (
          <ul className="divide-y divide-surface-border overflow-hidden rounded-2xl border border-surface-border">
            {analytics.topReferrers.map((r) => (
              <li key={r.uid} className="flex items-center gap-3 bg-surface-card px-4 py-3">
                <span className="min-w-0 flex-1 truncate font-medium text-ink">{r.name}</span>
                {r.suspicious > 0 && (
                  <span className="inline-flex items-center gap-1 rounded-full bg-amber-500/15 px-2 py-0.5 text-xs font-medium text-amber-400">
                    <AlertTriangle className="h-3 w-3" aria-hidden />
                    {dict.flaggedN.replace("{n}", String(r.suspicious))}
                  </span>
                )}
                <span className="tabular-nums text-sm font-semibold text-brand-300">{r.count}</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* Recent referrals + review */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold text-ink">{dict.recent}</h2>
        {analytics.recent.length === 0 ? (
          <p className="text-sm text-ink-muted">{dict.empty}</p>
        ) : (
          <div className="overflow-x-auto rounded-2xl border border-surface-border">
            <table className="w-full text-sm">
              <thead className="bg-surface-card text-left text-xs uppercase tracking-wide text-ink-faint">
                <tr>
                  <th className="px-4 py-2.5 font-medium">{dict.colReferrer}</th>
                  <th className="px-4 py-2.5 font-medium">{dict.colReferred}</th>
                  <th className="px-4 py-2.5 font-medium">{dict.colWhen}</th>
                  <th className="px-4 py-2.5 font-medium">{dict.colStatus}</th>
                  <th className="px-4 py-2.5" />
                </tr>
              </thead>
              <tbody className="divide-y divide-surface-border">
                {analytics.recent.map((r) => (
                  <tr key={r.referredId} className={r.suspicious ? "bg-amber-500/[0.06]" : ""}>
                    <td className="max-w-[12rem] truncate px-4 py-2.5 text-ink">{r.referrerName}</td>
                    <td className="max-w-[12rem] truncate px-4 py-2.5 text-ink">{r.referredName}</td>
                    <td className="whitespace-nowrap px-4 py-2.5 text-ink-muted">
                      {r.createdAtMs ? new Date(r.createdAtMs).toLocaleDateString() : "—"}
                    </td>
                    <td className="px-4 py-2.5">
                      {r.suspicious ? (
                        <span className="inline-flex items-center gap-1 text-amber-400">
                          <AlertTriangle className="h-3.5 w-3.5" aria-hidden />
                          {r.reason}
                        </span>
                      ) : (
                        <span className="text-ink-muted">{dict.statusOk}</span>
                      )}
                    </td>
                    <td className="px-4 py-2.5 text-right">
                      <button
                        type="button"
                        disabled={pending}
                        onClick={() => {
                          if (confirm(dict.revokeConfirm)) start(() => revokeReferral(r.referredId));
                        }}
                        className="rounded-lg border border-surface-border px-2.5 py-1 text-xs font-medium text-red-400 hover:bg-red-500/10 disabled:opacity-50"
                      >
                        {dict.revoke}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
