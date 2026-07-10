import Link from "next/link";
import { notFound } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { UserList } from "@/components/social/UserList";
import { getDict } from "@/lib/i18n/server";
import { getUserProfile } from "@/lib/user";
import { getFollowerIds, getFollowUsers } from "@/lib/social";

export default async function FollowersPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const [dict, profile, followerIds] = await Promise.all([getDict(), getUserProfile(id), getFollowerIds(id)]);
  if (!profile) notFound();
  const c = dict.catalog;
  const users = await getFollowUsers(followerIds);

  return (
    <div className="mx-auto max-w-3xl py-6">
      <div className="flex items-center gap-3 px-4 pb-4 md:px-6">
        <Link href={`/u/${id}`} className="text-ink-muted hover:text-ink" aria-label={c.goBack}>
          <ArrowLeft className="h-5 w-5" aria-hidden />
        </Link>
        <h1 className="text-lg font-semibold text-ink">{c.followers}</h1>
      </div>
      <UserList users={users} dict={c} />
    </div>
  );
}
