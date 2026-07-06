import { AdminPlaceholder } from "@/components/admin/AdminPlaceholder";
import { getDict } from "@/lib/i18n/server";

export default async function AdminRequestsPage() {
  const dict = await getDict();
  return <AdminPlaceholder title={dict.admin.requests} comingSoon={dict.common.comingSoon} />;
}
