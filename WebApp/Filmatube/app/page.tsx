import Link from "next/link";
import {
  Download,
  Film,
  MessagesSquare,
  Play,
  Sparkles,
  Theater,
  UsersRound,
  type LucideIcon,
} from "lucide-react";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { Wordmark } from "@/components/Wordmark";
import { getDict } from "@/lib/i18n/server";
import { cn } from "@/lib/utils";

export default async function LandingPage() {
  const dict = await getDict();
  const t = dict.landing;

  const features: { icon: LucideIcon; title: string; desc: string }[] = [
    { icon: Download, ...t.features.watch },
    { icon: UsersRound, ...t.features.social },
    { icon: Theater, ...t.features.theater },
    { icon: MessagesSquare, ...t.features.boards },
  ];

  return (
    <div className="flex min-h-screen flex-col">
      {/* ── Header ─────────────────────────────────────────────── */}
      <header className="sticky top-0 z-40 border-b border-surface-border/60 bg-surface/80 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-6">
          <Wordmark />
          <div className="flex items-center gap-3">
            <LanguageSwitcher />
            <Link
              href="#"
              className="hidden h-9 items-center rounded-lg border border-surface-border px-4 text-sm font-medium text-ink transition-colors hover:bg-surface-hover sm:inline-flex"
            >
              {dict.common.signIn}
            </Link>
          </div>
        </div>
      </header>

      <main className="flex-1">
        {/* ── Hero ─────────────────────────────────────────────── */}
        <section className="relative overflow-hidden">
          <div
            aria-hidden
            className="pointer-events-none absolute left-1/2 top-[-220px] h-[520px] w-[820px] -translate-x-1/2 rounded-full bg-brand-500/10 blur-3xl"
          />
          <div className="relative mx-auto flex w-full max-w-4xl flex-col items-center px-6 pb-16 pt-20 text-center md:pt-28">
            <span className="mb-6 inline-flex items-center gap-1.5 rounded-full border border-brand-700/50 bg-brand-700/25 px-3.5 py-1.5 text-xs font-medium text-brand-300">
              <Sparkles className="h-3.5 w-3.5" aria-hidden />
              {t.badge}
            </span>

            <h1 className="text-4xl font-extrabold tracking-tight text-ink md:text-6xl">
              {t.title}
            </h1>
            <p className="mt-5 max-w-xl text-base leading-relaxed text-ink-muted md:text-lg">
              {t.tagline}
            </p>

            <div className="mt-8 flex flex-col items-center gap-3">
              <div className="flex flex-col items-center gap-3 sm:flex-row">
                <Link
                  href="#features"
                  className="inline-flex h-12 items-center rounded-lg bg-brand-500 px-7 text-sm font-semibold text-white transition-colors hover:bg-brand-600"
                >
                  {t.ctaPrimary}
                </Link>
                <span className="inline-flex h-12 items-center gap-2 rounded-lg border border-surface-border px-5 text-sm font-medium text-ink-muted">
                  <Play className="h-4 w-4 fill-current" aria-hidden />
                  {t.storeBadge}
                </span>
              </div>
              <p className="text-xs text-ink-faint">{t.ctaHint}</p>
            </div>

            {/* Poster strip */}
            <div className="mt-16 flex w-full justify-center gap-3 md:gap-4">
              {[0, 1, 2, 3, 4, 5].map((i) => (
                <div
                  key={i}
                  className={cn(
                    "aspect-[2/3] w-24 shrink-0 items-center justify-center rounded-xl border border-surface-border bg-gradient-to-b from-surface-hover to-surface-card md:w-28",
                    i === 0 || i === 5 ? "hidden md:flex" : "flex",
                    (i === 2 || i === 3) &&
                      "-translate-y-3 border-brand-700/60 shadow-lg shadow-brand-900/30",
                  )}
                >
                  <Film className="h-6 w-6 text-ink-faint/50" aria-hidden />
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Features ─────────────────────────────────────────── */}
        <section id="features" className="mx-auto w-full max-w-6xl px-6 py-20">
          <p className="text-center text-xs font-semibold uppercase tracking-widest text-gold">
            {t.featuresKicker}
          </p>
          <h2 className="mt-2 text-center text-2xl font-bold tracking-tight text-ink md:text-3xl">
            {t.featuresTitle}
          </h2>

          <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {features.map((feature) => (
              <div
                key={feature.title}
                className="rounded-2xl border border-surface-border bg-surface-card p-6 transition-colors hover:border-brand-700/60"
              >
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-700/25">
                  <feature.icon className="h-5 w-5 text-brand-300" aria-hidden />
                </div>
                <h3 className="mt-4 font-semibold text-ink">{feature.title}</h3>
                <p className="mt-1.5 text-sm leading-relaxed text-ink-muted">{feature.desc}</p>
              </div>
            ))}
          </div>
        </section>
      </main>

      {/* ── Footer ─────────────────────────────────────────────── */}
      <footer className="border-t border-surface-border/60">
        <div className="mx-auto flex w-full max-w-6xl flex-col items-center gap-3 px-6 py-10 text-center sm:flex-row sm:justify-between sm:text-left">
          <div className="flex flex-col gap-1">
            <Wordmark />
            <p className="text-xs text-ink-faint">{t.footerTagline}</p>
          </div>
          <p className="text-xs text-ink-faint">
            © {new Date().getFullYear()} Filmatube. {t.footerRights}
          </p>
        </div>
      </footer>
    </div>
  );
}
