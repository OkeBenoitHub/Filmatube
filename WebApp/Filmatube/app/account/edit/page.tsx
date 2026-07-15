import { Pencil } from "lucide-react";
import { AccountHeader } from "@/components/account/AccountHeader";
import { EditProfileForm } from "@/components/account/EditProfileForm";
import { PageHero } from "@/components/ui/PageHero";
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
      <main className="mx-auto max-w-4xl px-4 py-10 md:px-6">
        <PageHero
          icon={Pencil}
          eyebrow={dict.account.eyebrow}
          title={dict.account.edit}
          subtitle={dict.account.editSubtitle}
        />
        <div className="mt-6">
          <EditProfileForm
            uid={user.uid}
            initialName={profile?.displayName ?? ""}
            initialBio={profile?.bio ?? ""}
            initialAvatarUrl={profile?.avatarUrl ?? ""}
            dict={dict.account}
          />
        </div>
      </main>
    </div>
  );
}
