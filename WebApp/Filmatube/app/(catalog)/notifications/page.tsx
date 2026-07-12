import { NotificationCenter } from "@/components/social/NotificationCenter";
import { getDict } from "@/lib/i18n/server";

export default async function NotificationsPage() {
  const dict = await getDict();
  return <NotificationCenter dict={dict.catalog} />;
}
