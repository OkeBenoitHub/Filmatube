import { ActivityFeed } from "@/components/social/ActivityFeed";
import { getDict } from "@/lib/i18n/server";

export default async function ActivityPage() {
  const dict = await getDict();
  return <ActivityFeed dict={dict.catalog} />;
}
