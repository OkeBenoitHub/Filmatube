"use client";

import { useTransition } from "react";
import Link from "next/link";
import { AlertTriangle, Ban, ShieldCheck, Undo2 } from "lucide-react";
import { setUserBanned } from "@/app/admin/users/actions";
import { UserAvatar } from "@/components/social/UserList";
import type { AdminUserRow } from "@/lib/admin/users";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Admin user list with ban/unban. Ban state shown here is the custom claim, not the doc flag. */
export function UsersTable({ users, dict }: { users: AdminUserRow[]; dict: Dictionary["admin"] }) {
  const [pending, startTransition] = useTransition();

  if (users.length === 0) {
    return <p className="py-16 text-center text-ink-muted">{dict.usersEmpty}</p>;
  }

  return (
    <ul className="space-y-2">
      {users.map((u) => (
        <li key={u.uid} className="flex items-center gap-3 rounded-xl border border-surface-border p-3">
          <UserAvatar name={u.displayName} url={u.avatarUrl} size={36} />
          <div className="min-w-0 flex-1">
            <Link href={`/u/${u.uid}`} className="flex items-center gap-1.5 truncate font-semibold text-ink hover:underline">
              {u.displayName || u.uid.slice(0, 8)}
              {u.isAdmin && <ShieldCheck className="h-4 w-4 shrink-0 text-brand-400" aria-label={dict.userAdmin} />}
            </Link>
            <p className="flex items-center gap-2 truncate text-xs text-ink-muted">
              {u.uid}
              {u.isBanned && <span className="font-semibold text-red-400">· {dict.userBanned}</span>}
              {u.flagMismatch && (
                <span className="inline-flex items-center gap-1 text-amber-400" title={dict.userFlagMismatchHint}>
                  <AlertTriangle className="h-3 w-3" aria-hidden />
                  {dict.userFlagMismatch}
                </span>
              )}
            </p>
          </div>

          <button
            type="button"
            disabled={pending}
            onClick={() => {
              if (!u.isBanned && !confirm(dict.userBanConfirm)) return;
              startTransition(() => setUserBanned(u.uid, !u.isBanned));
            }}
            className={
              u.isBanned
                ? "inline-flex h-8 shrink-0 items-center gap-1.5 whitespace-nowrap rounded-lg border border-surface-border px-2.5 text-xs font-medium text-ink-muted transition-colors hover:bg-surface-hover hover:text-ink disabled:opacity-60"
                : "inline-flex h-8 shrink-0 items-center gap-1.5 whitespace-nowrap rounded-lg border border-red-500/40 px-2.5 text-xs font-medium text-red-400 transition-colors hover:bg-red-500/10 disabled:opacity-60"
            }
          >
            {u.isBanned ? <Undo2 className="h-3.5 w-3.5" aria-hidden /> : <Ban className="h-3.5 w-3.5" aria-hidden />}
            {u.isBanned ? dict.userUnban : dict.userBan}
          </button>
        </li>
      ))}
    </ul>
  );
}
