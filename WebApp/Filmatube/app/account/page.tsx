import Link from "next/link";
import { AccountHeader } from "@/components/account/AccountHeader";
import { Avatar } from "@/components/account/Avatar";
import { requireUser } from "@/lib/auth/guards";
import { getDict } from "@/lib/i18n/server";
import { getUserProfile } from "@/lib/user";

export default async function AccountPage() {
  const user = await requireUser();
  const dict = await getDict();
  const a = dict.account;
  const profile = await getUserProfile(user.uid);

  return (
    <div className="min-h-screen">
      <AccountHeader signOutLabel={a.signOut} />
      <main className="mx-auto flex max-w-lg flex-col items-center gap-4 px-6 py-10">
        <Avatar url={profile?.avatarUrl} name={profile?.displayName ?? ""} size={96} />

        <div className="flex flex-col items-center gap-1 text-center">
          <h1 className="text-2xl font-bold text-ink">{profile?.displayName}</h1>
          {profile?.isAdmin && (
            <span className="rounded-md bg-gold/20 px-2 py-0.5 text-xs font-medium text-gold">{a.admin}</span>
          )}
          {profile?.bio && <p className="mt-1 text-sm text-ink-muted">{profile.bio}</p>}
        </div>

        <div className="flex gap-3">
          <Link
            href="/account/edit"
            className="inline-flex h-9 items-center rounded-lg border border-surface-border px-4 text-sm font-medium text-ink hover:bg-surface-hover"
          >
            {a.edit}
          </Link>
          <Link
            href="/account/profiles"
            className="inline-flex h-9 items-center rounded-lg border border-surface-border px-4 text-sm font-medium text-ink hover:bg-surface-hover"
          >
            {a.manageProfiles}
          </Link>
          <Link
            href="/account/notifications"
            className="inline-flex h-9 items-center rounded-lg border border-surface-border px-4 text-sm font-medium text-ink hover:bg-surface-hover"
          >
            {dict.catalog.notificationsTitle}
          </Link>
          <Link
            href="/requests"
            className="inline-flex h-9 items-center rounded-lg border border-surface-border px-4 text-sm font-medium text-ink hover:bg-surface-hover"
          >
            {dict.catalog.requestMovie}
          </Link>
        </div>

        <div className="mt-4 flex w-full justify-around">
          <Stat value={profile?.followersCount ?? 0} label={a.followers} />
          <Stat value={profile?.followingCount ?? 0} label={a.following} />
          <Stat value={0} label={a.watched} />
        </div>
      </main>
    </div>
  );
}

function Stat({ value, label }: { value: number; label: string }) {
  return (
    <div className="flex flex-col items-center">
      <span className="text-xl font-bold text-ink">{value}</span>
      <span className="text-xs text-ink-muted">{label}</span>
    </div>
  );
}
