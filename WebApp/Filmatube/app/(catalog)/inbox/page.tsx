import { RecommendationInbox } from "@/components/social/RecommendationInbox";
import { getDict } from "@/lib/i18n/server";

export default async function InboxPage() {
  const dict = await getDict();
  return <RecommendationInbox dict={dict.catalog} />;
}
