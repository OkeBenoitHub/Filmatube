import { AdminPlaceholder } from "@/components/admin/AdminPlaceholder";
import { getDict } from "@/lib/i18n/server";

export default async function AdminUsersPage() {
  const dict = await getDict();
  return <AdminPlaceholder title={dict.admin.users} comingSoon={dict.common.comingSoon} />;
}
