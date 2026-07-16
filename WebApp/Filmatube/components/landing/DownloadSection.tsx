import Link from "next/link";
import {
  CalendarDays,
  CheckCircle2,
  CircleHelp,
  Download,
  Film,
  FolderOpen,
  Globe,
  HardDrive,
  ShieldCheck,
  type LucideIcon,
} from "lucide-react";
import { APK, apkFileName } from "@/lib/apk";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

/* ── Simulated Android screens (decorative) ─────────────────────────────── */

function StatusBar() {
  // Device chrome — the clock/dots are decorative glyphs, never translated.
  return (
    <div className="flex items-center justify-between bg-black/40 px-2 pb-1 pt-1.5">
      <span className="text-[6px] text-white/50">9:41</span>
      <span className="flex gap-0.5">
        {[0, 1, 2].map((i) => (
          <span key={i} className="h-[3px] w-[3px] rounded-full bg-white/50" />
        ))}
      </span>
    </div>
  );
}

function SettingsScreen({ d }: { d: Dictionary["landing"]["download"] }) {
  return (
    <div className="flex h-full flex-col bg-[#1a1a1a]">
      <StatusBar />
      <div className="border-b border-white/10 bg-white/5 px-2 py-1.5">
        <p className="text-[8px] font-semibold text-white">{d.mockSecurity}</p>
      </div>
      {[d.mockScreenLock, d.mockFingerprint].map((item) => (
        <div key={item} className="border-b border-white/5 px-2 py-1.5">
          <p className="text-[7px] text-white/40">{item}</p>
        </div>
      ))}
      <div className="flex items-center justify-between border-l-2 border-brand-400 bg-brand-500/15 px-2 py-1.5">
        <div className="min-w-0 pr-1">
          <p className="text-[7.5px] font-semibold text-white">{d.mockUnknownApps}</p>
          <p className="text-[6px] text-white/40">{d.mockAllowSource}</p>
        </div>
        <span className="relative h-[11px] w-5 shrink-0 rounded-full bg-brand-400">
          <span className="absolute right-[2px] top-1/2 h-2 w-2 -translate-y-1/2 rounded-full bg-white" />
        </span>
      </div>
      {[d.mockDeviceAdmin, d.mockSimLock].map((item) => (
        <div key={item} className="border-b border-white/5 px-2 py-1.5">
          <p className="text-[7px] text-white/40">{item}</p>
        </div>
      ))}
    </div>
  );
}

function DownloadsScreen({ d }: { d: Dictionary["landing"]["download"] }) {
  return (
    <div className="flex h-full flex-col bg-[#1a1a1a]">
      <StatusBar />
      <div className="border-b border-white/10 bg-white/5 px-2 py-1.5">
        <p className="text-[8px] font-semibold text-white">{d.mockDownloads}</p>
      </div>
      <div className="flex items-center gap-1.5 border-l-2 border-brand-400 bg-brand-500/12 px-2 py-1.5">
        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-brand-500">
          <Film className="h-3 w-3 text-white" aria-hidden />
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-[7.5px] font-semibold text-white">{apkFileName}</p>
          <p className="text-[6px] text-white/40">
            {APK.size} · {d.mockComplete}
          </p>
          <div className="mt-1 h-[2px] rounded bg-brand-400" />
        </div>
        <CheckCircle2 className="h-3 w-3 shrink-0 text-brand-300" aria-hidden />
      </div>
      {[d.mockOtherFile1, d.mockOtherFile2].map((f) => (
        <div key={f} className="flex items-center gap-1.5 border-b border-white/5 px-2 py-1.5">
          <span className="h-[18px] w-[18px] shrink-0 rounded bg-white/10" />
          <p className="text-[7px] text-white/40">{f}</p>
        </div>
      ))}
    </div>
  );
}

function InstallerScreen({ d }: { d: Dictionary["landing"]["download"] }) {
  return (
    <div className="flex h-full flex-col items-center justify-center bg-[#1c1c1c] px-3">
      <span className="mb-1.5 flex h-11 w-11 items-center justify-center rounded-xl bg-brand-500 shadow-lg shadow-brand-500/40">
        <Film className="h-5 w-5 text-white" aria-hidden />
      </span>
      <p className="text-[9px] font-bold text-white">Filmatube</p>
      <p className="mb-2 text-center text-[6.5px] text-white/40">{d.mockInstallPrompt}</p>
      <div className="mb-2 w-full rounded-lg bg-white/5 px-1.5 py-1">
        <p className="mb-0.5 text-[6px] uppercase tracking-wide text-white/40">{d.mockPermissions}</p>
        {[d.mockPermInternet, d.mockPermStorage, d.mockPermNotifications].map((p) => (
          <div key={p} className="mb-[2px] flex items-center gap-1">
            <span className="h-[3px] w-[3px] shrink-0 rounded-full bg-brand-400" />
            <p className="text-[6.5px] text-white/55">{p}</p>
          </div>
        ))}
      </div>
      <div className="flex w-full gap-1.5">
        <span className="flex-1 rounded-md border border-white/20 py-1 text-center text-[7px] text-white/50">
          {d.mockCancel}
        </span>
        <span className="flex-1 rounded-md bg-brand-500 py-1 text-center text-[7px] font-bold text-white">
          {d.mockInstall}
        </span>
      </div>
    </div>
  );
}

/* ── Install step card ──────────────────────────────────────────────────── */

function StepCard({
  step,
  d,
}: {
  step: { num: string; icon: LucideIcon; title: string; desc: string; accent: string; Screen: typeof SettingsScreen };
  d: Dictionary["landing"]["download"];
}) {
  const { icon: Icon, Screen } = step;
  return (
    <div className="flex min-w-0 flex-1 flex-col items-center gap-5">
      {/* Mini phone frame */}
      <div className="relative">
        <div
          aria-hidden
          className={cn("absolute -bottom-2 left-1/2 h-5 w-20 -translate-x-1/2 rounded-full blur-xl", step.accent)}
        />
        <div className="relative z-10 h-[230px] w-[130px] overflow-hidden rounded-[22px] border border-surface-border bg-black shadow-2xl shadow-black/60">
          <div
            aria-hidden
            className="absolute left-1/2 top-0 z-10 h-2.5 w-9 -translate-x-1/2 rounded-b-md bg-[#0a0a0a]"
          />
          <Screen d={d} />
        </div>
      </div>

      {/* Step info */}
      <div className="max-w-[240px] text-center">
        <span className="mb-3 inline-flex items-center gap-1.5 rounded-full border border-brand-700/50 bg-brand-700/25 px-3 py-1">
          <Icon className="h-4 w-4 text-brand-300" aria-hidden />
          <span className="text-[11px] font-bold text-brand-300">
            {d.stepLabel} {step.num}
          </span>
        </span>
        <h3 className="mb-1.5 text-[15px] font-bold leading-snug text-ink">{step.title}</h3>
        <p className="text-[12.5px] leading-relaxed text-ink-muted">{step.desc}</p>
      </div>
    </div>
  );
}

/* ── Section ────────────────────────────────────────────────────────────── */

/**
 * Landing download section — APK CTA + metadata, an app card, and a three-step visual
 * install guide. Mirrors the Spotitube layout in Filmatube green; no Play Store tile,
 * since distribution is web-first + sideloaded APK.
 */
export function DownloadSection({ t }: { t: Dictionary["landing"] }) {
  const d = t.download;
  const hasApk = APK.url !== "";

  const pills: { icon: LucideIcon; label: string; sub: string }[] = [
    { icon: ShieldCheck, label: `v${APK.version}`, sub: d.version },
    { icon: HardDrive, label: APK.size, sub: d.size },
    { icon: CalendarDays, label: APK.updated, sub: d.updated },
  ];

  const steps = [
    {
      num: "1",
      icon: ShieldCheck,
      title: d.step1Title,
      desc: d.step1Desc,
      accent: "bg-brand-400/40",
      Screen: SettingsScreen,
    },
    {
      num: "2",
      icon: FolderOpen,
      title: d.step2Title,
      desc: d.step2Desc,
      accent: "bg-brand-500/40",
      Screen: DownloadsScreen,
    },
    {
      num: "3",
      icon: CheckCircle2,
      title: d.step3Title,
      desc: d.step3Desc,
      accent: "bg-brand-300/40",
      Screen: InstallerScreen,
    },
  ];

  return (
    <section id="download" className="relative scroll-mt-20 overflow-hidden">
      {/* ── Download CTA ── */}
      <div className="relative px-6 pb-16 pt-20 md:pb-20 md:pt-24">
        <div
          aria-hidden
          className="absolute left-1/2 top-0 h-px w-3/5 -translate-x-1/2 bg-gradient-to-r from-transparent via-brand-500/25 to-transparent"
        />
        <div className="mx-auto flex max-w-5xl flex-col items-center gap-10 md:flex-row md:gap-12">
          {/* Left — text */}
          <div className="relative z-10 flex-1">
            <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-gold">{d.badge}</p>
            <h2 className="mb-3 text-3xl font-black leading-[1.05] tracking-tight text-ink md:text-5xl">{d.title}</h2>
            <p className="mb-7 max-w-md text-[15px] leading-relaxed text-ink-muted md:text-base">
              {d.subtitle} · {APK.minAndroid} · {d.freeAlways}
            </p>

            {/* Metadata pills */}
            <div className="mb-7 flex flex-wrap gap-3">
              {pills.map(({ icon: Icon, label, sub }) => (
                <div
                  key={sub}
                  className="flex items-center gap-2 rounded-xl border border-surface-border bg-surface-card/60 px-3 py-2"
                >
                  <Icon className="h-3.5 w-3.5 shrink-0 text-brand-400" aria-hidden />
                  <div>
                    <p className="text-xs font-bold leading-tight text-ink">{label}</p>
                    <p className="text-[10px] text-ink-faint">{sub}</p>
                  </div>
                </div>
              ))}
            </div>

            {/* CTAs */}
            <div className="flex flex-wrap items-center gap-3">
              {hasApk ? (
                <a
                  href={APK.url}
                  download
                  className="inline-flex h-12 animate-pulse items-center gap-2 rounded-2xl bg-brand-500 px-7 text-[15px] font-bold text-white shadow-[0_0_28px_rgba(46,158,91,0.45)] transition-colors hover:animate-none hover:bg-brand-600"
                >
                  <Download className="h-4 w-4" aria-hidden />
                  {d.apkButton}
                </a>
              ) : (
                <span
                  aria-disabled
                  className="inline-flex h-12 cursor-default items-center gap-2 rounded-2xl border border-surface-border bg-surface-card/60 px-7 text-[15px] font-bold text-ink-faint"
                >
                  <Download className="h-4 w-4" aria-hidden />
                  {d.apkSoon}
                </span>
              )}

              <Link
                href="/install"
                className="inline-flex h-12 items-center gap-2 rounded-2xl border border-surface-border px-6 text-sm font-semibold text-ink transition-colors hover:border-brand-700/60 hover:text-brand-300"
              >
                <CircleHelp className="h-4 w-4" aria-hidden />
                {d.howToInstall}
              </Link>

              <Link
                href="/home"
                className="inline-flex h-12 items-center gap-2 rounded-2xl border border-surface-border px-6 text-sm font-semibold text-ink transition-colors hover:border-brand-700/60 hover:text-brand-300"
              >
                <Globe className="h-4 w-4" aria-hidden />
                {d.watchWeb}
              </Link>
            </div>

            {/* Changelog + help links */}
            <div className="relative z-10 mt-6 flex flex-wrap gap-6">
              <Link
                href="/changelog"
                className="border-b border-surface-border pb-0.5 text-[13px] text-ink-faint transition-colors hover:border-brand-700 hover:text-brand-300"
              >
                {d.viewChangelog} →
              </Link>
              <Link
                href="/install"
                className="border-b border-surface-border pb-0.5 text-[13px] text-ink-faint transition-colors hover:border-brand-700 hover:text-brand-300"
              >
                {d.needHelp} →
              </Link>
            </div>
          </div>

          {/* Right — app card */}
          <div className="w-full shrink-0 md:w-80">
            <div className="relative overflow-hidden rounded-[28px] border border-brand-700/40 bg-brand-900/20 p-7 shadow-2xl shadow-brand-900/20">
              <div
                aria-hidden
                className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_0%,rgba(93,208,138,0.12),transparent_70%)]"
              />
              <div className="relative">
                <span className="mb-5 flex h-[72px] w-[72px] items-center justify-center rounded-[20px] bg-brand-500 shadow-xl shadow-brand-500/40">
                  <Film className="h-9 w-9 text-white" aria-hidden />
                </span>

                <p className="text-[22px] font-extrabold tracking-tight text-ink">Filmatube</p>
                <p className="mb-5 text-[13px] text-ink-muted">{d.appSubtitle}</p>

                <div className="mb-5 flex items-center gap-1">
                  {[0, 1, 2, 3, 4].map((i) => (
                    <span key={i} className="text-sm" aria-hidden>
                      ⭐
                    </span>
                  ))}
                  <span className="ml-1 text-xs text-ink-faint">{d.newRelease}</span>
                </div>

                <ul className="space-y-2">
                  {[d.feat1, d.feat2, d.feat3, d.feat4, d.feat5].map((f) => (
                    <li key={f} className="flex items-center gap-2">
                      <CheckCircle2 className="h-4 w-4 shrink-0 text-brand-300" aria-hidden />
                      <span className="text-[12.5px] text-ink-muted">{f}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ── Install guide ── */}
      <div className="relative border-t border-surface-border/60 bg-surface-card/30 px-6 py-16 md:py-20">
        <div className="mx-auto mb-12 max-w-2xl text-center md:mb-16">
          <p className="text-xs font-semibold uppercase tracking-widest text-gold">{d.guideBadge}</p>
          <h2 className="mt-2 text-2xl font-black tracking-tight text-ink md:text-4xl">{d.guideTitle}</h2>
          <p className="mt-3 text-sm leading-relaxed text-ink-muted md:text-base">{d.guideSubtitle}</p>
        </div>

        <div className="relative mx-auto max-w-4xl">
          {/* Dashed connector (desktop) */}
          <div
            aria-hidden
            className="absolute left-[18%] right-[18%] top-[115px] hidden border-t-2 border-dashed border-surface-border md:block"
          />
          <div className="relative flex flex-col gap-12 md:flex-row md:gap-6">
            {steps.map((step) => (
              <StepCard key={step.num} step={step} d={d} />
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
