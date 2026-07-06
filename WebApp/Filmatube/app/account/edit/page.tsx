import { AccountHeader } from "@/components/account/AccountHeader";
import { EditProfileForm } from "@/components/account/EditProfileForm";
import { requireUser } from "@/lib/auth/guards";
import { getDict } from "@/lib/i18n/server";
import { getUserProfile } from "@/lib/user";

export default async function EditAccountPage() {
  const user = await requireUser();
  const dict = await getDict();
  const profile = await getUserProfile(user.uid);

  return (
    <div className="min-h-screen">
      <AccountHeader signOutLabel={dict.account.signOut} />
      <EditProfileForm
        uid={user.uid}
        initialName={profile?.displayName ?? ""}
        initialBio={profile?.bio ?? ""}
        initialAvatarUrl={profile?.avatarUrl ?? ""}
        dict={dict.account}
      />
    </div>
  );
}
