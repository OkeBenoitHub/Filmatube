import Link from "next/link";
import { FollowButton } from "@/components/social/FollowButton";
import { TasteMatchBadge } from "@/components/social/TasteMatchBadge";
import { UserAvatar } from "@/components/social/UserList";
import type { SuggestedUser } from "@/lib/social";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Grid of suggested users to follow, each with a taste-match badge. */
export function SuggestionList({ users, dict }: { users: SuggestedUser[]; dict: Dictionary["catalog"] }) {
  if (users.length === 0) {
    return <p className="py-12 text-center text-ink-muted">{dict.discoverEmpty}</p>;
  }
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {users.map((u) => (
        <div key={u.uid} className="flex items-center gap-3 rounded-xl border border-surface-border p-3">
          <Link href={`/u/${u.uid}`} className="flex min-w-0 flex-1 items-center gap-3">
            <UserAvatar name={u.displayName} url={u.avatarUrl} size={44} />
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-ink">{u.displayName}</p>
              <div className="mt-0.5">
                <TasteMatchBadge percent={u.tasteMatch} dict={dict} size="sm" />
              </div>
            </div>
          </Link>
          <FollowButton targetUid={u.uid} dict={dict} size="sm" />
        </div>
      ))}
    </div>
  );
}
