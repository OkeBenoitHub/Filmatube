import { AccountHeader } from "@/components/account/AccountHeader";
import { ProfilesManager } from "@/components/account/ProfilesManager";
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
      <ProfilesManager uid={user.uid} defaultName={defaultName} dict={dict.account} />
    </div>
  );
}
