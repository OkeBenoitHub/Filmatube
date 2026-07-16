"use client";

import { useTransition } from "react";
import Link from "next/link";
import { MicOff, UserMinus, Volume2 } from "lucide-react";
import { removeBoardMember, setBoardMemberMuted } from "@/app/boards/actions";
import { UserAvatar } from "@/components/social/UserList";
import type { BoardMember } from "@/lib/boards";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** One member row. Mute/remove show only for the board owner (and admins). */
export function MemberRow({
  boardId,
  member,
  canModerate,
  dict,
}: {
  boardId: string;
  member: BoardMember;
  canModerate: boolean;
  dict: Dictionary["catalog"];
}) {
  const [pending, startTransition] = useTransition();
  const isOwner = member.role === "owner";

  return (
    <li className="flex items-center gap-3 rounded-xl border border-surface-border p-3">
      <Link href={`/u/${member.uid}`}>
        <UserAvatar name={member.name} url={member.avatar} size={36} />
      </Link>
      <div className="min-w-0 flex-1">
        <Link href={`/u/${member.uid}`} className="block truncate text-sm font-semibold text-ink hover:underline">
          {member.name}
        </Link>
        <p className="text-xs text-ink-muted">
          {isOwner ? dict.roleOwner : dict.roleMember}
          {member.muted && ` · ${dict.mutedLabel}`}
        </p>
      </div>

      {canModerate && !isOwner && (
        <div className="flex shrink-0 gap-1.5">
          <button
            type="button"
            disabled={pending}
            onClick={() => startTransition(() => setBoardMemberMuted(boardId, member.uid, !member.muted))}
            className="inline-flex h-8 items-center gap-1.5 rounded-lg border border-surface-border px-2.5 text-xs font-medium text-ink-muted transition-colors hover:bg-surface-hover hover:text-ink disabled:opacity-60"
          >
            {member.muted ? <Volume2 className="h-3.5 w-3.5" aria-hidden /> : <MicOff className="h-3.5 w-3.5" aria-hidden />}
            {member.muted ? dict.unmuteAction : dict.muteAction}
          </button>
          <button
            type="button"
            disabled={pending}
            onClick={() => startTransition(() => removeBoardMember(boardId, member.uid))}
            className="inline-flex h-8 items-center gap-1.5 rounded-lg border border-surface-border px-2.5 text-xs font-medium text-ink-muted transition-colors hover:bg-surface-hover hover:text-red-400 disabled:opacity-60"
          >
            <UserMinus className="h-3.5 w-3.5" aria-hidden />
            {dict.removeMemberAction}
          </button>
        </div>
      )}
    </li>
  );
}
