import { AdminPlaceholder } from "@/components/admin/AdminPlaceholder";
import { getDict } from "@/lib/i18n/server";

export default async function AdminTheaterPage() {
  const dict = await getDict();
  return <AdminPlaceholder title={dict.admin.theater} comingSoon={dict.common.comingSoon} />;
}
