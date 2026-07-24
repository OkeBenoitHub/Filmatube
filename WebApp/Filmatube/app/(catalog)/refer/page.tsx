import { headers } from "next/headers";
import { Gift } from "lucide-react";
import { Avatar } from "@/components/account/Avatar";
import { InviteCard } from "@/components/referral/InviteCard";
import { requireUser } from "@/lib/auth/guards";
import { getDict } from "@/lib/i18n/server";
import { getReferralsBy } from "@/lib/referrals";
import { inviteUrl } from "@/lib/referral-shared";
import { getUserProfile } from "@/lib/user";

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
