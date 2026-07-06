import { AdminPlaceholder } from "@/components/admin/AdminPlaceholder";
import { getDict } from "@/lib/i18n/server";

export default async function AdminAnalyticsPage() {
  const dict = await getDict();
  return <AdminPlaceholder title={dict.admin.analytics} comingSoon={dict.common.comingSoon} />;
}
