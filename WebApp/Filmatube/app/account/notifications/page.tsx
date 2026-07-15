import { Bell } from "lucide-react";
import { AccountHeader } from "@/components/account/AccountHeader";
import { NotificationPreferences } from "@/components/social/NotificationPreferences";
import { PageHero } from "@/components/ui/PageHero";
import { getDict } from "@/lib/i18n/server";

export default async function AccountNotificationsPage() {
  const dict = await getDict();
  const c = dict.catalog;
  return (
    <div className="min-h-screen">
      <AccountHeader signOutLabel={dict.account.signOut} />
      <main className="mx-auto max-w-4xl px-4 py-10 md:px-6">
        <PageHero icon={Bell} eyebrow={dict.account.eyebrow} title={c.notifPrefsTitle} subtitle={c.notifPrefsDesc} />
        <div className="mx-auto mt-8 max-w-2xl">
          <NotificationPreferences dict={c} />
        </div>
      </main>
    </div>
  );
}
