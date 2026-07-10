import Link from "next/link";
import { notFound } from "next/navigation";
import { FollowButton } from "@/components/social/FollowButton";
import { UserAvatar } from "@/components/social/UserList";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict } from "@/lib/i18n/server";
import { getUserProfile } from "@/lib/user";
import { getFollowerIds, getFollowingIds, tasteMatch } from "@/lib/social";

export default async function PublicProfilePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [me, dict, profile, followerIds, followingIds] = await Promise.all([
    getCurrentUser(),
    getDict(),
    getUserProfile(id),
    getFollowerIds(id),
    getFollowingIds(id),
  ]);
  if (!profile) notFound();
  const c = dict.catalog;
  const isSelf = me?.uid === id;

  let taste = 0;
  if (me && !isSelf) {
    const mine = await getUserProfile(me.uid);
    taste = tasteMatch(mine?.genrePreferences ?? [], profile.genrePreferences);
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 md:px-6">
      <div className="flex flex-col items-center gap-4 text-center sm:flex-row sm:items-start sm:text-left">
        <UserAvatar name={profile.displayName} url={profile.avatarUrl} size={96} />
        <div className="flex-1 space-y-3">
          <div>
            <h1 className="text-2xl font-bold text-ink">{profile.displayName}</h1>
            {profile.bio && <p className="mt-1 text-sm text-ink-muted">{profile.bio}</p>}
          </div>

          <div className="flex items-center justify-center gap-6 sm:justify-start">
            <Link href={`/u/${id}/followers`} className="text-sm text-ink hover:underline">
              <span className="font-bold">{followerIds.length}</span>{" "}
              <span className="text-ink-muted">{c.followers}</span>
            </Link>
            <Link href={`/u/${id}/following`} className="text-sm text-ink hover:underline">
              <span className="font-bold">{followingIds.length}</span>{" "}
              <span className="text-ink-muted">{c.followingWord}</span>
            </Link>
          </div>

          {!isSelf && taste > 0 && (
            <p className="text-sm font-semibold text-brand-400">
              {taste}% {c.matchSuffix}
            </p>
          )}

          <div className="flex justify-center sm:justify-start">
            <FollowButton targetUid={id} dict={c} />
          </div>
        </div>
      </div>

      {profile.genrePreferences.length > 0 && (
        <div className="mt-8 flex flex-wrap gap-1.5">
          {profile.genrePreferences.map((g) => (
            <span key={g} className="rounded-full bg-surface-hover px-2.5 py-1 text-xs text-ink-muted">
              {(dict.genres as Record<string, string>)[g] ?? g}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
