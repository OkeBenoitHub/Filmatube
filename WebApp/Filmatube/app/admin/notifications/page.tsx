import { BroadcastComposer } from "@/components/admin/BroadcastComposer";
import { getDict } from "@/lib/i18n/server";

export default async function AdminNotificationsPage() {
  const dict = await getDict();
  const genreLabels = dict.genres as Record<string, string>;
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <h1 className="text-2xl font-bold text-ink">{dict.admin.broadcastHeading}</h1>
      <BroadcastComposer dict={dict.admin} genreLabels={genreLabels} />
    </div>
  );
}
