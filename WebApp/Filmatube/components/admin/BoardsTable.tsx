"use client";

import { useTransition } from "react";
import Link from "next/link";
import { BadgeCheck, Lock, Star, Trash2 } from "lucide-react";
import { deleteBoard, setBoardFeatured, setBoardOfficial } from "@/app/admin/boards/actions";
import type { Board } from "@/lib/boards";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

/** Admin board list: toggle official/featured, or delete a board outright. */
export function BoardsTable({ boards, dict }: { boards: Board[]; dict: Dictionary["admin"] }) {
  const [pending, startTransition] = useTransition();

  if (boards.length === 0) {
    return <p className="py-16 text-center text-ink-muted">{dict.boardsEmpty}</p>;
  }

  return (
    <ul className="space-y-2">
      {boards.map((b) => (
        <li key={b.id} className="flex items-center gap-3 rounded-xl border border-surface-border p-3">
          <div className="min-w-0 flex-1">
            <Link href={`/boards/${b.id}`} className="flex items-center gap-1.5 truncate font-semibold text-ink hover:underline">
              {b.title}
              {b.isOfficial && <BadgeCheck className="h-4 w-4 shrink-0 text-brand-400" aria-hidden />}
              {!b.isPublic && <Lock className="h-3.5 w-3.5 shrink-0 text-ink-muted" aria-hidden />}
            </Link>
            <p className="truncate text-xs text-ink-muted">
              {b.type} · {b.memberCount} · {b.id}
            </p>
          </div>

          <div className="flex shrink-0 gap-1.5">
            <button
              type="button"
              disabled={pending}
              onClick={() => startTransition(() => setBoardOfficial(b.id, !b.isOfficial))}
              aria-pressed={b.isOfficial}
              className={cn(
                "inline-flex h-8 items-center gap-1.5 rounded-lg border px-2.5 text-xs font-medium transition-colors disabled:opacity-60",
                b.isOfficial
                  ? "border-brand-500 bg-brand-500/15 text-ink"
                  : "border-surface-border text-ink-muted hover:bg-surface-hover hover:text-ink",
              )}
            >
              <BadgeCheck className="h-3.5 w-3.5" aria-hidden />
              {dict.boardOfficial}
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={() => startTransition(() => setBoardFeatured(b.id, !b.isFeatured))}
              aria-pressed={b.isFeatured}
              className={cn(
                "inline-flex h-8 items-center gap-1.5 rounded-lg border px-2.5 text-xs font-medium transition-colors disabled:opacity-60",
                b.isFeatured
                  ? "border-brand-500 bg-brand-500/15 text-ink"
                  : "border-surface-border text-ink-muted hover:bg-surface-hover hover:text-ink",
              )}
            >
              <Star className="h-3.5 w-3.5" aria-hidden />
              {dict.boardFeatured}
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={() => {
                if (confirm(dict.boardDeleteConfirm)) startTransition(() => deleteBoard(b.id));
              }}
              aria-label={dict.boardDelete}
              className="inline-flex h-8 items-center rounded-lg border border-surface-border px-2.5 text-ink-muted transition-colors hover:bg-surface-hover hover:text-red-400 disabled:opacity-60"
            >
              <Trash2 className="h-3.5 w-3.5" aria-hidden />
            </button>
          </div>
        </li>
      ))}
    </ul>
  );
}
