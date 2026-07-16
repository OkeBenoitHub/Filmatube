import Link from "next/link";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { Wordmark } from "@/components/Wordmark";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/**
 * Sticky landing/marketing header — logo, section nav, language switcher, sign-in + app CTA.
 * `linkPrefix` is "" on the landing page (same-page anchors) and "/" elsewhere (jump to landing).
 */
export function LandingHeader({
  dict,
  linkPrefix = "",
  hideSignIn = false,
}: {
  dict: Dictionary;
  linkPrefix?: string;
  hideSignIn?: boolean;
}) {
  const t = dict.landing;
  const navLinks = [
    { href: `${linkPrefix}#features`, label: t.navFeatures },
    { href: `${linkPrefix}#how`, label: t.navHow },
    { href: `${linkPrefix}#faq`, label: t.navFaq },
    { href: `${linkPrefix}#download`, label: t.navDownload },
  ];

  return (
    <header className="sticky top-0 z-40 border-b border-surface-border/60 bg-surface/80 backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-6">
        <Wordmark />
        <nav className="hidden items-center gap-7 md:flex">
          {navLinks.map((link) => (
            <a key={link.href} href={link.href} className="text-sm font-medium text-ink-muted transition-colors hover:text-ink">
              {link.label}
            </a>
          ))}
        </nav>
        <div className="flex items-center gap-3">
          <LanguageSwitcher />
          {!hideSignIn && (
            <Link
              href="/login"
              className="hidden h-9 items-center whitespace-nowrap rounded-lg px-3 text-sm font-medium text-ink-muted transition-colors hover:text-ink sm:inline-flex"
            >
              {dict.common.signIn}
            </Link>
          )}
          <Link
            href="/home"
            className="inline-flex h-9 items-center whitespace-nowrap rounded-lg bg-brand-500 px-4 text-sm font-semibold text-white transition-colors hover:bg-brand-600"
          >
            {t.openApp}
          </Link>
        </div>
      </div>
    </header>
  );
}
