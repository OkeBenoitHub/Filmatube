"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Home, Search, Compass, Bookmark, Users, Mail, MessagesSquare, UserRound } from "lucide-react";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { NotificationBell } from "@/components/social/NotificationBell";
import { Wordmark } from "@/components/Wordmark";
import { useAuth } from "@/components/providers/AuthProvider";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

/** Top navigation for the signed-in catalog: Home / Browse / Search + account + sign out. */
export function AppHeader({ dict }: { dict: Dictionary["catalog"] }) {
  const pathname = usePathname();
  const router = useRouter();
  const { signOut } = useAuth();

  const links = [
    { href: "/home", label: dict.home, icon: Home },
    { href: "/browse", label: dict.browse, icon: Compass },
    { href: "/search", label: dict.search, icon: Search },
    { href: "/library", label: dict.library, icon: Bookmark },
    { href: "/activity", label: dict.activity, icon: Users },
    { href: "/boards", label: dict.boards, icon: MessagesSquare },
  ];

  return (
    <header className="sticky top-0 z-40 border-b border-surface-border/60 bg-surface/80 backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center gap-2 px-3 sm:gap-4 sm:px-4 md:px-6">
        <Wordmark href="/home" />

        {/* Primary nav — scrolls horizontally on small screens so it never overflows the bar. */}
        <nav className="no-scrollbar flex min-w-0 flex-1 items-center gap-0.5 overflow-x-auto sm:gap-1">
          {links.map(({ href, label, icon: Icon }) => {
            const active = pathname === href || pathname.startsWith(`${href}/`);
            return (
              <Link
                key={href}
                href={href}
                // Full prefetch: dynamic routes otherwise only prefetch their loading shell.
                // Combined with staleTimes, the payload is already local when clicked.
                prefetch={true}
                className={cn(
                  "flex shrink-0 items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-sm font-medium transition-colors md:px-3",
                  active ? "bg-surface-hover text-ink" : "text-ink-muted hover:text-ink",
                )}
              >
                <Icon className="h-4 w-4 shrink-0" aria-hidden />
                <span className="hidden lg:inline">{label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="flex shrink-0 items-center gap-1.5 sm:gap-2">
          <LanguageSwitcher />
          <NotificationBell dict={dict} />
          <Link
            href="/inbox"
            aria-label={dict.inbox}
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-surface-border text-ink-muted transition-colors hover:bg-surface-hover hover:text-ink"
          >
            <Mail className="h-4 w-4" aria-hidden />
          </Link>
          <Link
            href="/account"
            aria-label={dict.account}
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-surface-border text-ink-muted transition-colors hover:bg-surface-hover hover:text-ink"
          >
            <UserRound className="h-4 w-4" aria-hidden />
          </Link>
          <button
            type="button"
            onClick={async () => {
              await signOut();
              router.replace("/");
              router.refresh();
            }}
            className="hidden h-9 shrink-0 whitespace-nowrap rounded-lg border border-surface-border px-4 text-sm font-medium text-ink transition-colors hover:bg-surface-hover lg:block"
          >
            {dict.signOut}
          </button>
        </div>
      </div>
    </header>
  );
}
