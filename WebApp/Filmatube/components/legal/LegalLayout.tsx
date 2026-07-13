import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { LandingFooter } from "@/components/landing/LandingFooter";
import { Wordmark } from "@/components/Wordmark";
import type { Dictionary } from "@/lib/i18n/dictionaries";

interface LegalSection {
  heading: string;
  body: string[];
}

interface LegalDoc {
  title: string;
  subtitle: string;
  updated: string;
  sections: LegalSection[];
}

/** Shared legal-page shell: header, title block, numbered sections, and the landing footer. */
export function LegalLayout({ doc, dict }: { doc: LegalDoc; dict: Dictionary }) {
  const g = dict.legal;
  return (
    <div className="flex min-h-screen flex-col bg-surface">
      <header className="sticky top-0 z-40 border-b border-surface-border/60 bg-surface/80 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-6">
          <Wordmark />
          <Link href="/" className="inline-flex items-center gap-1.5 text-sm text-ink-muted transition-colors hover:text-ink">
            <ArrowLeft className="h-4 w-4" aria-hidden />
            {g.backHome}
          </Link>
        </div>
      </header>

      <main className="mx-auto w-full max-w-3xl flex-1 px-6 pb-20 pt-14 md:pt-20">
        <h1 className="text-4xl font-extrabold tracking-tight text-ink md:text-5xl">{doc.title}</h1>
        <p className="mt-3 text-ink-muted">{doc.subtitle}</p>
        <p className="mt-1 text-xs text-ink-faint">
          {g.updatedLabel}: {doc.updated}
        </p>

        <div className="my-10 h-px bg-surface-border" />

        <div className="space-y-10">
          {doc.sections.map((section, i) => (
            <section key={section.heading}>
              <h2 className="flex items-baseline gap-3 text-xs font-bold uppercase tracking-widest text-brand-400">
                <span className="tabular-nums text-brand-700">{String(i + 1).padStart(2, "0")}</span>
                {section.heading}
              </h2>
              <div className="mt-3 space-y-3">
                {section.body.map((para, j) => (
                  <p key={j} className="text-sm leading-7 text-ink-muted">
                    {para}
                  </p>
                ))}
              </div>
            </section>
          ))}
        </div>
      </main>

      <LandingFooter t={dict.landing} />
    </div>
  );
}
