import Link from "next/link";
import { Globe } from "lucide-react";
import { notFound } from "next/navigation";
import { FollowButton } from "@/components/social/FollowButton";
import { TasteMatchBadge } from "@/components/social/TasteMatchBadge";
import { UserAvatar } from "@/components/social/UserList";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict } from "@/lib/i18n/server";
import { getUserProfile } from "@/lib/user";
import { getPublicCollections } from "@/lib/collections";
import { getFollowerIds, getFollowingIds, tasteMatch } from "@/lib/social";

export default async function PublicProfilePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [me, dict, profile, followerIds, followingIds, collections] = await Promise.all([
    getCurrentUser(),
    getDict(),
    getUserProfile(id),
    getFollowerIds(id),
    getFollowingIds(id),
    getPublicCollections(id),
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

          {!isSelf && (
            <div className="flex justify-center sm:justify-start">
              <TasteMatchBadge percent={taste} dict={c} />
            </div>
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

      {collections.length > 0 && (
        <section className="mt-10 space-y-3">
          <h2 className="text-lg font-semibold text-ink">{c.collections}</h2>
          <div className="flex gap-3 overflow-x-auto pb-2">
            {collections.map((col) => (
              <Link key={col.id} href={`/collections/${col.id}`} className="w-40 shrink-0">
                <div className="relative aspect-video overflow-hidden rounded-lg border border-surface-border bg-surface-hover">
                  {col.coverUrl && (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img src={col.coverUrl} alt="" className="h-full w-full object-cover" />
                  )}
                  <span className="absolute right-1.5 top-1.5 rounded bg-black/60 p-1 text-white">
                    <Globe className="h-3 w-3" aria-hidden />
                  </span>
                </div>
                <p className="mt-1.5 truncate text-sm text-ink">{col.title}</p>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
