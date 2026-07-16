"use client";

import { useRouter } from "next/navigation";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { Wordmark } from "@/components/Wordmark";
import { useAuth } from "@/components/providers/AuthProvider";

/** Header for authenticated account pages: wordmark, language switcher, sign out. */
export function AccountHeader({ signOutLabel }: { signOutLabel: string }) {
  const { signOut } = useAuth();
  const router = useRouter();

  return (
    <header className="sticky top-0 z-40 border-b border-surface-border/60 bg-surface/80 backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-4xl items-center justify-between px-6">
        <Wordmark />
        <div className="flex items-center gap-3">
          <LanguageSwitcher />
          <button
            type="button"
            onClick={async () => {
              await signOut();
              router.replace("/");
              router.refresh();
            }}
            className="h-9 rounded-lg border border-surface-border px-4 text-sm font-medium text-ink transition-colors hover:bg-surface-hover"
          >
            {signOutLabel}
          </button>
        </div>
      </div>
    </header>
  );
}
