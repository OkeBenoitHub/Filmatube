import { notFound } from "next/navigation";
import { BadgeCheck, Lock, MessagesSquare, Users } from "lucide-react";
import { getCurrentUser } from "@/lib/auth/session";
import { getBoard, isBoardMember } from "@/lib/boards";
import { getDict } from "@/lib/i18n/server";

export default async function BoardPage({ params }: { params: Promise<{ id: string }> }) {
  const [{ id }, user, dict] = await Promise.all([params, getCurrentUser(), getDict()]);
  const c = dict.catalog;

  const board = await getBoard(id, user?.uid);
  if (!board) notFound();
  const isMember = user ? await isBoardMember(id, user.uid) : false;
  const members = board.memberCount === 1 ? c.memberCountOne : c.memberCount;

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 md:px-6">
      <header className="flex flex-col gap-5 sm:flex-row sm:items-end">
        <div className="relative aspect-video w-full shrink-0 overflow-hidden rounded-xl border border-surface-border bg-gradient-to-br from-brand-700/40 to-surface-hover sm:w-56">
          {board.coverUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={board.coverUrl} alt="" className="h-full w-full object-cover" />
          ) : (
            <span className="flex h-full items-center justify-center">
              <MessagesSquare className="h-8 w-8 text-brand-300/70" aria-hidden />
            </span>
          )}
        </div>

        <div className="min-w-0 flex-1">
          <p className="text-xs font-bold uppercase tracking-widest text-ink-muted">{c.boardsEyebrow}</p>
          <h1 className="mt-1 flex items-center gap-2 text-3xl font-black tracking-tight text-ink md:text-4xl">
            <span className="min-w-0 break-words">{board.title}</span>
            {board.isOfficial && (
              <BadgeCheck className="h-6 w-6 shrink-0 text-brand-400" aria-label={c.boardOfficial} />
            )}
            {!board.isPublic && <Lock className="h-5 w-5 shrink-0 text-ink-muted" aria-hidden />}
          </h1>
          {board.description && <p className="mt-2 text-sm text-ink-muted">{board.description}</p>}
          <p className="mt-2 flex items-center gap-1.5 text-xs text-ink-muted">
            <Users className="h-3.5 w-3.5" aria-hidden />
            {board.memberCount} {members}
            {board.movieTitle && <span className="truncate"> · {board.movieTitle}</span>}
          </p>
        </div>
      </header>

      {/* Day 135 mounts the real-time chat here. */}
      <div className="mt-10 rounded-xl border border-surface-border bg-surface-hover/40 p-10 text-center text-sm text-ink-muted">
        {isMember ? c.boardsMine : c.boardsSubtitle}
      </div>
    </div>
  );
}
