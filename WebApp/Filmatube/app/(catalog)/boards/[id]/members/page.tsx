import { notFound } from "next/navigation";
import { Users } from "lucide-react";
import { MemberRow } from "@/components/boards/MemberRow";
import { PageHero } from "@/components/ui/PageHero";
import { getCurrentUser } from "@/lib/auth/session";
import { getBoard, getBoardMembers } from "@/lib/boards";
import { getDict } from "@/lib/i18n/server";

export default async function BoardMembersPage({ params }: { params: Promise<{ id: string }> }) {
  const [{ id }, user, dict] = await Promise.all([params, getCurrentUser(), getDict()]);
  const c = dict.catalog;

  const board = await getBoard(id, user?.uid);
  if (!board) notFound();
  const members = await getBoardMembers(id);
  const canModerate = !!user && (board.ownerId === user.uid || user.admin === true);

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 md:px-6">
      <PageHero icon={Users} eyebrow={board.title} title={c.membersTitle} subtitle={c.membersSubtitle} />
      <ul className="mt-12 space-y-2">
        {members.map((m) => (
          <MemberRow key={m.uid} boardId={id} member={m} canModerate={canModerate} dict={c} />
        ))}
      </ul>
    </div>
  );
}
