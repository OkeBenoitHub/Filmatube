import Link from "next/link";
import { BadgeCheck, Lock, MessagesSquare, Users } from "lucide-react";
import type { Board } from "@/lib/boards";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** A board tile: cover (or gradient fallback), title, member count and official/private badges. */
export function BoardCard({ board, dict }: { board: Board; dict: Dictionary["catalog"] }) {
  const members = board.memberCount === 1 ? dict.memberCountOne : dict.memberCount;

  return (
    <Link href={`/boards/${board.id}`} className="group block">
      <div className="relative aspect-video overflow-hidden rounded-xl border border-surface-border bg-gradient-to-br from-brand-700/40 to-surface-hover">
        {board.coverUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={board.coverUrl}
            alt=""
            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
          />
        ) : (
          <span className="flex h-full items-center justify-center">
            <MessagesSquare className="h-8 w-8 text-brand-300/70" aria-hidden />
          </span>
        )}
        <div className="absolute right-1.5 top-1.5 flex gap-1">
          {board.isOfficial && (
            <span
              className="rounded bg-black/60 p-1 text-brand-300"
              title={dict.boardOfficial}
              aria-label={dict.boardOfficial}
            >
              <BadgeCheck className="h-3.5 w-3.5" aria-hidden />
            </span>
          )}
          {!board.isPublic && (
            <span className="rounded bg-black/60 p-1 text-white">
              <Lock className="h-3.5 w-3.5" aria-hidden />
            </span>
          )}
        </div>
      </div>
      <p className="mt-1.5 truncate text-sm font-semibold text-ink group-hover:text-brand-300">{board.title}</p>
      <p className="mt-0.5 flex items-center gap-1 text-xs text-ink-muted">
        <Users className="h-3 w-3" aria-hidden />
        {board.memberCount} {members}
        {board.movieTitle && <span className="truncate"> · {board.movieTitle}</span>}
      </p>
    </Link>
  );
}
