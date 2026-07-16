import Link from "next/link";
import { ReportsTable } from "@/components/admin/ReportsTable";
import { getDict } from "@/lib/i18n/server";
import { getReports, REPORT_TYPES } from "@/lib/admin/reports";
import { cn } from "@/lib/utils";

export default async function AdminReportsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const [dict, allReports, sp] = await Promise.all([getDict(), getReports(), searchParams]);
  const a = dict.admin;

  const scope = typeof sp.scope === "string" ? sp.scope : "";
  const reports =
    scope === "boards"
      ? allReports.filter((r) => r.type === REPORT_TYPES.BOARD_MESSAGE)
      : scope === "pending"
        ? allReports.filter((r) => r.status === "pending")
        : allReports;

  const pendingBoards = allReports.filter(
    (r) => r.type === REPORT_TYPES.BOARD_MESSAGE && r.status === "pending",
  ).length;

  const scopes = [
    { value: "", label: a.reportScopeAll, count: 0 },
    { value: "pending", label: a.reportScopePending, count: 0 },
    { value: "boards", label: a.reportScopeBoards, count: pendingBoards },
  ];

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-ink">{a.reportsTitle}</h1>
        <div className="mt-3 flex flex-wrap gap-1.5">
          {scopes.map((s) => (
            <Link
              key={s.value || "all"}
              href={s.value ? `/admin/reports?scope=${s.value}` : "/admin/reports"}
              aria-current={scope === s.value ? "page" : undefined}
              className={cn(
                "inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-xs font-semibold transition-colors",
                scope === s.value
                  ? "border-brand-500 bg-brand-500 text-white"
                  : "border-surface-border text-ink-muted hover:bg-surface-hover hover:text-ink",
              )}
            >
              {s.label}
              {s.count > 0 && (
                <span className="rounded-full bg-amber-500/20 px-1.5 text-[10px] font-bold text-amber-400">
                  {s.count}
                </span>
              )}
            </Link>
          ))}
        </div>
      </div>

      <ReportsTable reports={reports} dict={a} />
    </div>
  );
}
