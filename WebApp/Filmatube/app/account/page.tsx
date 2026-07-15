import Link from "next/link";
import { Bell, ChevronRight, Clapperboard, Pencil, Users, type LucideIcon } from "lucide-react";
import { AccountHeader } from "@/components/account/AccountHeader";
import { Avatar } from "@/components/account/Avatar";
import { requireUser } from "@/lib/auth/guards";
import { getDict } from "@/lib/i18n/server";
import { getUserProfile } from "@/lib/user";

export default async function AccountPage() {
  const user = await requireUser();
  const dict = await getDict();
  const a = dict.account;
  const c = dict.catalog;
  const profile = await getUserProfile(user.uid);

  const links: { href: string; icon: LucideIcon; title: string; desc: string }[] = [
    { href: "/account/edit", icon: Pencil, title: a.edit, desc: a.editSubtitle },
    { href: "/account/profiles", icon: Users, title: a.manageProfiles, desc: a.profilesSubtitle },
    { href: "/account/notifications", icon: Bell, title: c.notifPrefsTitle, desc: c.notifPrefsDesc },
    { href: "/requests", icon: Clapperboard, title: c.requestMovie, desc: c.requestsSubtitle },
  ];

  return (
    <div className="min-h-screen">
      <AccountHeader signOutLabel={a.signOut} />
      <main className="mx-auto max-w-4xl px-4 py-10 md:px-6">
        {/* ── Hero (Spotitube pattern with the user's avatar as the tile) ── */}
        <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-end">
          <div className="shrink-0 rounded-full shadow-2xl shadow-brand-900/50 ring-4 ring-brand-700/40">
            <Avatar url={profile?.avatarUrl} name={profile?.displayName ?? ""} size={160} />
          </div>
          <div className="min-w-0 text-center sm:text-left">
            <p className="text-xs font-bold uppercase tracking-widest text-ink-muted">{a.eyebrow}</p>
            <h1 className="mt-1 truncate text-4xl font-black leading-none tracking-tight text-ink md:text-6xl">
              {profile?.displayName || a.title}
            </h1>
            <p className="mt-2 text-sm text-ink-muted">{profile?.bio || a.subtitle}</p>
            {profile?.isAdmin && (
              <span className="mt-3 inline-block rounded-md bg-gold/20 px-2 py-0.5 text-xs font-medium text-gold">
                {a.admin}
              </span>
            )}
          </div>
        </div>

        {/* ── Stats ── */}
        <div className="mt-10 grid grid-cols-3 gap-4">
          <Stat value={profile?.followersCount ?? 0} label={a.followers} />
          <Stat value={profile?.followingCount ?? 0} label={a.following} />
          <Stat value={0} label={a.watched} />
        </div>

        {/* ── Quick links ── */}
        <div className="mt-8 grid gap-4 sm:grid-cols-2">
          {links.map(({ href, icon: Icon, title, desc }) => (
            <Link
              key={href}
              href={href}
              className="group flex items-center gap-4 rounded-2xl border border-surface-border bg-surface-card p-5 transition-colors hover:border-brand-700/60"
            >
              <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand-700/25 transition-colors group-hover:bg-brand-700/40">
                <Icon className="h-5 w-5 text-brand-300" aria-hidden />
              </span>
              <span className="min-w-0 flex-1">
                <span className="block font-semibold text-ink">{title}</span>
                <span className="block truncate text-sm text-ink-muted">{desc}</span>
              </span>
              <ChevronRight className="h-4 w-4 shrink-0 text-ink-faint transition-transform group-hover:translate-x-0.5" aria-hidden />
            </Link>
          ))}
        </div>
      </main>
    </div>
  );
}

function Stat({ value, label }: { value: number; label: string }) {
  return (
    <div className="rounded-2xl border border-surface-border bg-surface-card p-5 text-center">
      <span className="block text-2xl font-extrabold text-brand-400">{value}</span>
      <span className="mt-0.5 block text-xs text-ink-muted">{label}</span>
    </div>
  );
}
