import Link from "next/link";
import {
  ArrowRight,
  Captions,
  Download,
  Film,
  Gauge,
  Globe,
  Heart,
  Languages,
  MessagesSquare,
  MonitorPlay,
  PictureInPicture2,
  Play,
  PlayCircle,
  Plus,
  Rss,
  RotateCcw,
  Send,
  Sparkles,
  Theater,
  UserPlus,
  UsersRound,
  type LucideIcon,
} from "lucide-react";
import { LandingFooter } from "@/components/landing/LandingFooter";
import { LandingHeader } from "@/components/landing/LandingHeader";
import { getDict } from "@/lib/i18n/server";
import { cn } from "@/lib/utils";

export default async function LandingPage() {
  const dict = await getDict();
  const t = dict.landing;

  const features: { icon: LucideIcon; title: string; desc: string }[] = [
    { icon: MonitorPlay, ...t.features.watch },
    { icon: UsersRound, ...t.features.social },
    { icon: Theater, ...t.features.theater },
    { icon: MessagesSquare, ...t.features.boards },
    { icon: Download, ...t.features.downloads },
    { icon: Sparkles, ...t.features.discover },
  ];

  const steps = [t.how.step1, t.how.step2, t.how.step3];

  const socialPoints: { icon: LucideIcon; title: string; desc: string }[] = [
    { icon: UserPlus, ...t.social.follow },
    { icon: Rss, ...t.social.feed },
    { icon: Heart, ...t.social.reactions },
    { icon: Send, ...t.social.recommend },
  ];

  const playerPoints: { icon: LucideIcon; label: string }[] = [
    { icon: Captions, label: t.player.subtitles },
    { icon: Languages, label: t.player.audio },
    { icon: Gauge, label: t.player.speed },
    { icon: RotateCcw, label: t.player.resume },
    { icon: PictureInPicture2, label: t.player.pip },
    { icon: Download, label: t.player.offline },
  ];

  const stats = [t.stats.movies, t.stats.members, t.stats.reviews, t.stats.countries];

  const faqs = [t.faq.q1, t.faq.q2, t.faq.q3, t.faq.q4, t.faq.q5, t.faq.q6];

  return (
    <div className="flex min-h-screen flex-col bg-surface">
      <LandingHeader dict={dict} />

      <main className="flex-1">
        {/* ── Hero ─────────────────────────────────────────────── */}
        <section className="relative overflow-hidden">
          <div
            aria-hidden
            className="pointer-events-none absolute left-1/2 top-[-220px] h-[520px] w-[820px] -translate-x-1/2 rounded-full bg-brand-500/10 blur-3xl"
          />
          <div
            aria-hidden
            className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_-10%,rgba(93,208,138,0.08),transparent_60%)]"
          />
          <div className="relative mx-auto flex w-full max-w-4xl flex-col items-center px-6 pb-16 pt-20 text-center md:pt-28">
            <span className="mb-6 inline-flex items-center gap-1.5 rounded-full border border-brand-700/50 bg-brand-700/25 px-3.5 py-1.5 text-xs font-medium text-brand-300">
              <Sparkles className="h-3.5 w-3.5" aria-hidden />
              {t.badge}
            </span>

            <h1 className="text-4xl font-extrabold tracking-tight text-ink md:text-6xl">{t.title}</h1>
            <p className="mt-5 max-w-xl text-base leading-relaxed text-ink-muted md:text-lg">{t.tagline}</p>

            <div className="mt-8 flex flex-col items-center gap-4">
              <div className="flex flex-col items-center gap-3 sm:flex-row">
                <Link
                  href="/register"
                  className="inline-flex h-12 items-center gap-2 rounded-lg bg-brand-500 px-7 text-sm font-semibold text-white transition-colors hover:bg-brand-600"
                >
                  {t.ctaPrimary}
                  <ArrowRight className="h-4 w-4" aria-hidden />
                </Link>
                <Link
                  href="/home"
                  className="inline-flex h-12 items-center gap-2 rounded-lg border border-surface-border px-6 text-sm font-medium text-ink transition-colors hover:bg-surface-hover"
                >
                  <Play className="h-4 w-4 fill-current" aria-hidden />
                  {t.ctaSecondary}
                </Link>
              </div>
              <p className="text-xs text-ink-faint">{t.heroTrust}</p>
            </div>

            {/* Poster strip */}
            <div className="mt-16 flex w-full justify-center gap-3 md:gap-4">
              {[0, 1, 2, 3, 4, 5].map((i) => (
                <div
                  key={i}
                  className={cn(
                    "aspect-[2/3] w-24 shrink-0 items-center justify-center rounded-xl border border-surface-border bg-gradient-to-b from-surface-hover to-surface-card md:w-28",
                    i === 0 || i === 5 ? "hidden md:flex" : "flex",
                    (i === 2 || i === 3) && "-translate-y-3 border-brand-700/60 shadow-lg shadow-brand-900/30",
                  )}
                >
                  <Film className="h-6 w-6 text-ink-faint/50" aria-hidden />
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Features ─────────────────────────────────────────── */}
        <section id="features" className="mx-auto w-full max-w-6xl scroll-mt-20 px-6 py-20">
          <SectionHeading kicker={t.featuresKicker} title={t.featuresTitle} subtitle={t.featuresSubtitle} />
          <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {features.map((feature) => (
              <div
                key={feature.title}
                className="group rounded-2xl border border-surface-border bg-surface-card p-6 transition-colors hover:border-brand-700/60"
              >
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-700/25 transition-colors group-hover:bg-brand-700/40">
                  <feature.icon className="h-5 w-5 text-brand-300" aria-hidden />
                </div>
                <h3 className="mt-4 font-semibold text-ink">{feature.title}</h3>
                <p className="mt-1.5 text-sm leading-relaxed text-ink-muted">{feature.desc}</p>
              </div>
            ))}
          </div>
        </section>

        {/* ── How it works ─────────────────────────────────────── */}
        <section id="how" className="scroll-mt-20 border-y border-surface-border/60 bg-surface-card/30">
          <div className="mx-auto w-full max-w-6xl px-6 py-20">
            <SectionHeading kicker={t.howKicker} title={t.howTitle} />
            <div className="mt-12 grid gap-6 md:grid-cols-3">
              {steps.map((step, i) => (
                <div key={step.title} className="relative rounded-2xl border border-surface-border bg-surface p-6">
                  <div className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-500 text-sm font-bold text-white">
                    {i + 1}
                  </div>
                  <h3 className="mt-4 text-lg font-semibold text-ink">{step.title}</h3>
                  <p className="mt-2 text-sm leading-relaxed text-ink-muted">{step.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Social spotlight ─────────────────────────────────── */}
        <section className="mx-auto w-full max-w-6xl px-6 py-20">
          <div className="grid items-center gap-12 lg:grid-cols-2">
            <div>
              <p className="text-xs font-semibold uppercase tracking-widest text-gold">{t.socialKicker}</p>
              <h2 className="mt-2 text-2xl font-bold tracking-tight text-ink md:text-3xl">{t.socialTitle}</h2>
              <p className="mt-4 max-w-md text-sm leading-relaxed text-ink-muted md:text-base">{t.socialDesc}</p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              {socialPoints.map((p) => (
                <div key={p.title} className="rounded-2xl border border-surface-border bg-surface-card p-5">
                  <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-700/25">
                    <p.icon className="h-5 w-5 text-brand-300" aria-hidden />
                  </div>
                  <h3 className="mt-3 text-sm font-semibold text-ink">{p.title}</h3>
                  <p className="mt-1 text-sm leading-relaxed text-ink-muted">{p.desc}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Player spotlight ─────────────────────────────────── */}
        <section className="border-y border-surface-border/60 bg-surface-card/30">
          <div className="mx-auto grid w-full max-w-6xl items-center gap-12 px-6 py-20 lg:grid-cols-2">
            {/* Mock player */}
            <div className="order-2 lg:order-1">
              <div className="relative aspect-video overflow-hidden rounded-2xl border border-surface-border bg-gradient-to-br from-surface-hover to-surface-card shadow-2xl shadow-brand-900/20">
                <div aria-hidden className="absolute inset-0 bg-[radial-gradient(circle_at_50%_40%,rgba(93,208,138,0.12),transparent_70%)]" />
                <div className="absolute inset-0 flex items-center justify-center">
                  <div className="flex h-16 w-16 items-center justify-center rounded-full bg-brand-500/90 shadow-lg">
                    <Play className="h-7 w-7 fill-white text-white" aria-hidden />
                  </div>
                </div>
                <div className="absolute inset-x-4 bottom-4">
                  <div className="h-1 rounded-full bg-white/20">
                    <div className="h-1 w-2/5 rounded-full bg-brand-400" />
                  </div>
                </div>
              </div>
            </div>
            {/* Points */}
            <div className="order-1 lg:order-2">
              <p className="text-xs font-semibold uppercase tracking-widest text-gold">{t.playerKicker}</p>
              <h2 className="mt-2 text-2xl font-bold tracking-tight text-ink md:text-3xl">{t.playerTitle}</h2>
              <p className="mt-4 max-w-md text-sm leading-relaxed text-ink-muted md:text-base">{t.playerDesc}</p>
              <ul className="mt-6 grid gap-3 sm:grid-cols-2">
                {playerPoints.map((p) => (
                  <li key={p.label} className="flex items-center gap-2.5 text-sm text-ink">
                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-brand-700/25">
                      <p.icon className="h-4 w-4 text-brand-300" aria-hidden />
                    </span>
                    {p.label}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </section>

        {/* ── Stats ────────────────────────────────────────────── */}
        <section className="mx-auto w-full max-w-6xl px-6 py-20">
          <SectionHeading kicker={t.statsKicker} title={t.statsTitle} />
          <div className="mt-12 grid grid-cols-2 gap-4 md:grid-cols-4">
            {stats.map((s) => (
              <div key={s.label} className="rounded-2xl border border-surface-border bg-surface-card p-6 text-center">
                <p className="text-3xl font-extrabold text-brand-400 md:text-4xl">{s.value}</p>
                <p className="mt-1.5 text-sm text-ink-muted">{s.label}</p>
              </div>
            ))}
          </div>
        </section>

        {/* ── Download CTA ─────────────────────────────────────── */}
        <section id="download" className="scroll-mt-20 px-6 pb-20">
          <div className="relative mx-auto w-full max-w-5xl overflow-hidden rounded-3xl border border-brand-700/40 bg-gradient-to-br from-brand-900/40 via-surface-card to-surface px-6 py-14 text-center md:py-20">
            <div aria-hidden className="pointer-events-none absolute left-1/2 top-[-140px] h-[360px] w-[560px] -translate-x-1/2 rounded-full bg-brand-500/15 blur-3xl" />
            <div className="relative mx-auto max-w-xl">
              <p className="text-xs font-semibold uppercase tracking-widest text-gold">{t.downloadKicker}</p>
              <h2 className="mt-2 text-3xl font-bold tracking-tight text-ink md:text-4xl">{t.downloadTitle}</h2>
              <p className="mt-4 text-sm leading-relaxed text-ink-muted md:text-base">{t.downloadDesc}</p>
              <div className="mt-8 flex flex-col items-center justify-center gap-3 sm:flex-row">
                <Link
                  href="/home"
                  className="inline-flex h-12 items-center gap-2 rounded-lg bg-brand-500 px-7 text-sm font-semibold text-white transition-colors hover:bg-brand-600"
                >
                  <Globe className="h-4 w-4" aria-hidden />
                  {t.downloadWeb}
                </Link>
                <span className="inline-flex h-12 items-center gap-2 rounded-lg border border-surface-border px-6 text-sm font-medium text-ink-muted">
                  <PlayCircle className="h-4 w-4" aria-hidden />
                  {t.downloadPlay}
                </span>
              </div>
            </div>
          </div>
        </section>

        {/* ── FAQ ──────────────────────────────────────────────── */}
        <section id="faq" className="mx-auto w-full max-w-3xl scroll-mt-20 px-6 pb-24">
          <SectionHeading kicker={t.faqKicker} title={t.faqTitle} />
          <div className="mt-10 divide-y divide-surface-border rounded-2xl border border-surface-border bg-surface-card">
            {faqs.map((item) => (
              <details key={item.q} className="group px-5 py-4">
                <summary className="flex cursor-pointer list-none items-center justify-between gap-4 text-sm font-semibold text-ink">
                  {item.q}
                  <Plus className="h-4 w-4 shrink-0 text-brand-400 transition-transform group-open:rotate-45" aria-hidden />
                </summary>
                <p className="mt-3 text-sm leading-relaxed text-ink-muted">{item.a}</p>
              </details>
            ))}
          </div>
        </section>
      </main>

      <LandingFooter t={t} />
    </div>
  );
}

function SectionHeading({ kicker, title, subtitle }: { kicker: string; title: string; subtitle?: string }) {
  return (
    <div className="mx-auto max-w-2xl text-center">
      <p className="text-xs font-semibold uppercase tracking-widest text-gold">{kicker}</p>
      <h2 className="mt-2 text-2xl font-bold tracking-tight text-ink md:text-3xl">{title}</h2>
      {subtitle && <p className="mt-3 text-sm leading-relaxed text-ink-muted md:text-base">{subtitle}</p>}
    </div>
  );
}
