import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { NotificationPreferences } from "@/components/social/NotificationPreferences";
import { getDict } from "@/lib/i18n/server";

export default async function AccountNotificationsPage() {
  const dict = await getDict();
  const c = dict.catalog;
  return (
    <div className="mx-auto max-w-2xl px-4 py-8 md:px-6">
      <div className="mb-6 flex items-center gap-3">
        <Link href="/account" className="text-ink-muted hover:text-ink" aria-label={c.goBack}>
          <ArrowLeft className="h-5 w-5" aria-hidden />
        </Link>
        <h1 className="text-2xl font-bold text-ink">{c.notifPrefsTitle}</h1>
      </div>
      <NotificationPreferences dict={c} />
    </div>
  );
}
