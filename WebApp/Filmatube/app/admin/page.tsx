import { AdminPlaceholder } from "@/components/admin/AdminPlaceholder";
import { getDict } from "@/lib/i18n/server";

/** Dashboard skeleton is fleshed out on Day 27. */
export default async function AdminDashboardPage() {
  const dict = await getDict();
  return <AdminPlaceholder title={dict.admin.dashboard} comingSoon={dict.common.comingSoon} />;
}
