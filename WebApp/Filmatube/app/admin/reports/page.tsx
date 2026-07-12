import { ReportsTable } from "@/components/admin/ReportsTable";
import { getDict } from "@/lib/i18n/server";
import { getReports } from "@/lib/admin/reports";

export default async function AdminReportsPage() {
  const [dict, reports] = await Promise.all([getDict(), getReports()]);
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-bold text-ink">{dict.admin.reportsTitle}</h1>
      <ReportsTable reports={reports} dict={dict.admin} />
    </div>
  );
}
