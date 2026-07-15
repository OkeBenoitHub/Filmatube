import { Users } from "lucide-react";
import { AccountHeader } from "@/components/account/AccountHeader";
import { ProfilesManager } from "@/components/account/ProfilesManager";
import { PageHero } from "@/components/ui/PageHero";
import { requireUser } from "@/lib/auth/guards";
import { getDict } from "@/lib/i18n/server";
import { getUserProfile } from "@/lib/user";

export default async function ProfilesPage() {
  const user = await requireUser();
  const dict = await getDict();
  const profile = await getUserProfile(user.uid);
  const defaultName = profile?.displayName?.trim() || "Me";

  return (
    <div className="min-h-screen">
      <AccountHeader signOutLabel={dict.account.signOut} />
      <main className="mx-auto max-w-4xl px-4 py-10 md:px-6">
        <PageHero
          icon={Users}
          eyebrow={dict.account.eyebrow}
          title={dict.account.profiles}
          subtitle={dict.account.profilesSubtitle}
        />
        <div className="mt-6">
          <ProfilesManager uid={user.uid} defaultName={defaultName} dict={dict.account} />
        </div>
      </main>
    </div>
  );
}
