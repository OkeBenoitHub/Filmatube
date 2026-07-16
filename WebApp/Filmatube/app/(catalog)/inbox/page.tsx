import { Mail } from "lucide-react";
import { RecommendationInbox } from "@/components/social/RecommendationInbox";
import { PageHero } from "@/components/ui/PageHero";
import { getDict } from "@/lib/i18n/server";

export default async function InboxPage() {
  const dict = await getDict();
  const c = dict.catalog;
  return (
    <div className="mx-auto max-w-6xl px-4 py-8 md:px-6">
      <PageHero icon={Mail} eyebrow={c.inboxEyebrow} title={c.inbox} subtitle={c.inboxSubtitle} />
      <div className="mt-10">
        <RecommendationInbox dict={c} />
      </div>
    </div>
  );
}
