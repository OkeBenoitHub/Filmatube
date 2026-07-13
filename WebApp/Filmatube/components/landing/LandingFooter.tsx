import Link from "next/link";
import { Github, Instagram, Twitter } from "lucide-react";
import { Wordmark } from "@/components/Wordmark";
import type { Dictionary } from "@/lib/i18n/dictionaries";

type FooterLink = { label: string; href: string; external?: boolean };

/** Rich multi-column landing footer — brand, four link columns, social row + bottom bar. */
export function LandingFooter({ t }: { t: Dictionary["landing"] }) {
  const l = t.footerLinks;

  const columns: { title: string; links: FooterLink[] }[] = [
    {
      title: t.footerProduct,
      links: [
        { label: l.features, href: "/#features" },
        { label: t.navHow, href: "/#how" },
        { label: l.player, href: "/login" },
        { label: l.download, href: "/#download" },
      ],
    },
    {
      title: t.footerExplore,
      links: [
        { label: l.home, href: "/home" },
        { label: l.search, href: "/search" },
        { label: l.requests, href: "/requests" },
        { label: l.signIn, href: "/login" },
      ],
    },
    {
      title: t.footerCompany,
      links: [
        { label: l.about, href: "/#features" },
        { label: l.privacy, href: "/privacy" },
        { label: l.terms, href: "/terms" },
        { label: l.contact, href: "mailto:hello@filmatube.app", external: true },
      ],
    },
    {
      title: t.footerSupport,
      links: [
        { label: l.faq, href: "/#faq" },
        { label: l.help, href: "/#faq" },
        { label: l.report, href: "mailto:support@filmatube.app", external: true },
        { label: l.status, href: "/#download" },
      ],
    },
  ];

  const socials: { label: string; href: string; icon: typeof Github }[] = [
    { label: "GitHub", href: "https://github.com", icon: Github },
    { label: "Instagram", href: "https://instagram.com", icon: Instagram },
    { label: "X", href: "https://x.com", icon: Twitter },
  ];

  return (
    <footer className="border-t border-surface-border/60 bg-surface-card/40">
      <div className="mx-auto grid w-full max-w-6xl grid-cols-2 gap-8 px-6 pb-10 pt-14 md:grid-cols-[2fr_1fr_1fr_1fr_1fr]">
        {/* Brand */}
        <div className="col-span-2 md:col-span-1">
          <Wordmark />
          <p className="mt-4 max-w-xs text-sm leading-relaxed text-ink-faint">{t.footerTagline}</p>
        </div>

        {/* Link columns */}
        {columns.map((col) => (
          <nav key={col.title} aria-label={col.title}>
            <h3 className="text-[11px] font-bold uppercase tracking-widest text-ink-muted">{col.title}</h3>
            <ul className="mt-4 space-y-2.5">
              {col.links.map((link) => (
                <li key={link.label}>
                  {link.external ? (
                    <a href={link.href} className="text-sm text-ink-faint transition-colors hover:text-brand-400">
                      {link.label}
                    </a>
                  ) : (
                    <Link href={link.href} className="text-sm text-ink-faint transition-colors hover:text-brand-400">
                      {link.label}
                    </Link>
                  )}
                </li>
              ))}
            </ul>
          </nav>
        ))}
      </div>

      {/* Bottom bar */}
      <div className="border-t border-surface-border/50">
        <div className="mx-auto flex w-full max-w-6xl flex-col items-start justify-between gap-4 px-6 py-5 sm:flex-row sm:items-center">
          <p className="text-xs text-ink-faint">
            © {new Date().getFullYear()} Filmatube. {t.footerRights} · {t.footerMade}
          </p>
          <div className="flex items-center gap-2">
            {socials.map(({ label, href, icon: Icon }) => (
              <a
                key={label}
                href={href}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={label}
                className="flex h-9 w-9 items-center justify-center rounded-lg border border-surface-border bg-surface-hover/40 text-ink-faint transition-colors hover:border-brand-700/60 hover:bg-brand-700/20 hover:text-brand-300"
              >
                <Icon className="h-4 w-4" aria-hidden />
              </a>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
