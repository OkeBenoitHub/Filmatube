import { Bell } from "lucide-react";
import { NotificationCenter } from "@/components/social/NotificationCenter";
import { PageHero } from "@/components/ui/PageHero";
import { getDict } from "@/lib/i18n/server";

export default async function NotificationsPage() {
  const dict = await getDict();
  const c = dict.catalog;
  return (
    <div className="mx-auto max-w-6xl px-4 py-8 md:px-6">
      <PageHero icon={Bell} eyebrow={c.notifEyebrow} title={c.notificationsTitle} subtitle={c.notifSubtitle} />
      <div className="mt-10">
        <NotificationCenter dict={c} />
      </div>
    </div>
  );
}
