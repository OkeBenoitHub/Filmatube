import Link from "next/link";
import { FollowButton } from "@/components/social/FollowButton";
import type { FollowUserCard } from "@/lib/social";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Circular avatar with an initial fallback. */
export function UserAvatar({ name, url, size = 44 }: { name: string; url: string; size?: number }) {
  return (
    <div
      className="flex shrink-0 items-center justify-center overflow-hidden rounded-full border border-surface-border bg-surface-hover text-sm font-semibold text-ink-muted"
      style={{ width: size, height: size }}
    >
      {url ? (
        // eslint-disable-next-line @next/next/no-img-element
        <img src={url} alt="" className="h-full w-full object-cover" />
      ) : (
        (name.trim()[0] ?? "?").toUpperCase()
      )}
    </div>
  );
}

/** A vertical list of user cards, each linking to their public profile with a follow toggle. */
export function UserList({ users, dict }: { users: FollowUserCard[]; dict: Dictionary["catalog"] }) {
  if (users.length === 0) {
    return <p className="px-4 py-8 text-ink-muted md:px-6">{dict.noUsers}</p>;
  }
  return (
    <ul className="divide-y divide-surface-border">
      {users.map((u) => (
        <li key={u.uid} className="flex items-center gap-3 px-4 py-3 md:px-6">
          <Link href={`/u/${u.uid}`} className="flex min-w-0 flex-1 items-center gap-3">
            <UserAvatar name={u.displayName} url={u.avatarUrl} />
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-ink">{u.displayName}</p>
              {u.bio && <p className="truncate text-xs text-ink-muted">{u.bio}</p>}
            </div>
          </Link>
          <FollowButton targetUid={u.uid} dict={dict} size="sm" />
        </li>
      ))}
    </ul>
  );
}
