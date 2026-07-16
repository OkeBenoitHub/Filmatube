"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import {
  BarChart3,
  Bell,
  Film,
  Flag,
  Inbox,
  LayoutDashboard,
  MessagesSquare,
  Theater,
  Users,
  type LucideIcon,
} from "lucide-react";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { Wordmark } from "@/components/Wordmark";
import { useAuth } from "@/components/providers/AuthProvider";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

export function AdminShell({
  dict,
  signOutLabel,
  children,
}: {
  dict: Dictionary["admin"];
  signOutLabel: string;
  children: ReactNode;
}) {
  const pathname = usePathname();
  const router = useRouter();
  const { signOut } = useAuth();

  const items: { href: string; label: string; icon: LucideIcon }[] = [
    { href: "/admin", label: dict.dashboard, icon: LayoutDashboard },
    { href: "/admin/movies", label: dict.movies, icon: Film },
    { href: "/admin/users", label: dict.users, icon: Users },
    { href: "/admin/requests", label: dict.requests, icon: Inbox },
    { href: "/admin/boards", label: dict.boards, icon: MessagesSquare },
    { href: "/admin/reports", label: dict.reports, icon: Flag },
    { href: "/admin/theater", label: dict.theater, icon: Theater },
    { href: "/admin/notifications", label: dict.notifications, icon: Bell },
    { href: "/admin/analytics", label: dict.analytics, icon: BarChart3 },
  ];

  const isActive = (href: string) =>
    href === "/admin" ? pathname === "/admin" : pathname.startsWith(href);

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-60 shrink-0 flex-col border-r border-surface-border bg-surface-card px-3 py-4 md:flex">
        <div className="px-2">
          <Wordmark />
        </div>
        <nav className="mt-6 flex flex-col gap-1">
          {items.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors",
                isActive(item.href)
                  ? "bg-brand-700/25 font-medium text-brand-300"
                  : "text-ink-muted hover:bg-surface-hover hover:text-ink",
              )}
            >
              <item.icon className="h-4 w-4" aria-hidden />
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-end gap-3 border-b border-surface-border px-6">
          <LanguageSwitcher />
          <button
            type="button"
            onClick={async () => {
              await signOut();
              router.replace("/");
              router.refresh();
            }}
            className="h-9 rounded-lg border border-surface-border px-3 text-sm font-medium text-ink hover:bg-surface-hover"
          >
            {signOutLabel}
          </button>
        </header>
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
