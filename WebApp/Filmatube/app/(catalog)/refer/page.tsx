import { headers } from "next/headers";
import { Gift, Lock, Ticket, UserPlus, type LucideIcon } from "lucide-react";
import { Avatar } from "@/components/account/Avatar";
import { InviteCard } from "@/components/referral/InviteCard";
import { requireUser } from "@/lib/auth/guards";
import { getDict } from "@/lib/i18n/server";
import { getReferralsBy } from "@/lib/referrals";
import { inviteUrl } from "@/lib/referral-shared";
import { getUserProfile } from "@/lib/user";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Invite-a-friend dashboard: the user's link (copy/share) and who has joined through it. */
export default async function ReferPage() {
  const user = await requireUser();
  const dict = await getDict();
  const c = dict.referral;

  const h = await headers();
  const origin = `${h.get("x-forwarded-proto") ?? "https"}://${h.get("host")}`;
  const url = inviteUrl(origin, user.uid);

  const referrals = await getReferralsBy(user.uid);
  const friends = (
    await Promise.all(
      referrals.slice(0, 50).map(async (r) => {
        const p = await getUserProfile(r.referredId);
        return p ? { id: r.referredId, name: p.displayName || c.aFriendName, avatarUrl: p.avatarUrl } : null;
      }),
    )
  ).filter((f): f is { id: string; name: string; avatarUrl: string } => f !== null);

  return (
    <div className="mx-auto max-w-6xl space-y-8 px-4 py-10 md:px-6">
      <div className="flex items-center gap-4">
        <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-700 shadow-lg shadow-brand-900/40">
          <Gift className="h-7 w-7 text-white" aria-hidden />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-ink">{c.dashTitle}</h1>
          <p className="mt-1 text-sm text-ink-muted">{c.dashSubtitle}</p>
        </div>
      </div>

      <InviteCard url={url} dict={c} />

      {/* Rewards — unlocked the moment a first friend joins (the function also persists a durable
          "recruiter" badge + earlyAccess flag for the achievement engine). */}
      <section className="space-y-3">
        <h2 className="text-lg font-semibold text-ink">{c.rewardsTitle}</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          <RewardCard icon={UserPlus} title={c.recruiter} desc={c.recruiterDesc} unlocked={friends.length > 0} dict={c} />
          <RewardCard icon={Ticket} title={c.earlyAccess} desc={c.earlyAccessDesc} unlocked={friends.length > 0} dict={c} />
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold text-ink">
          {c.referredHeading.replace("{n}", String(friends.length))}
        </h2>
        {friends.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-surface-border p-8 text-center text-ink-muted">
            {c.referredEmpty}
          </p>
        ) : (
          <ul className="divide-y divide-surface-border overflow-hidden rounded-2xl border border-surface-border">
            {friends.map((f) => (
              <li key={f.id} className="flex items-center gap-3 bg-surface-card px-4 py-3">
                <Avatar url={f.avatarUrl} name={f.name} size={40} />
                <span className="min-w-0 flex-1 truncate font-medium text-ink">{f.name}</span>
                <span className="rounded-full bg-brand-700/25 px-2.5 py-0.5 text-xs font-medium text-brand-300">
                  {c.joinedLabel}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

function RewardCard({
  icon: Icon,
  title,
  desc,
  unlocked,
  dict,
}: {
  icon: LucideIcon;
  title: string;
  desc: string;
  unlocked: boolean;
  dict: Dictionary["referral"];
}) {
  return (
    <div className={`flex items-center gap-3 rounded-2xl border p-4 ${unlocked ? "border-brand-700/50 bg-brand-700/10" : "border-surface-border bg-surface-card"}`}>
      <span className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl ${unlocked ? "bg-brand-600 text-white" : "bg-surface-hover text-ink-faint"}`}>
        {unlocked ? <Icon className="h-5 w-5" aria-hidden /> : <Lock className="h-5 w-5" aria-hidden />}
      </span>
      <div className="min-w-0 flex-1">
        <p className="font-semibold text-ink">{title}</p>
        <p className="truncate text-sm text-ink-muted">{desc}</p>
      </div>
      <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${unlocked ? "bg-brand-700/25 text-brand-300" : "bg-surface-hover text-ink-faint"}`}>
        {unlocked ? dict.rewardUnlocked : dict.rewardLocked}
      </span>
    </div>
  );
}
