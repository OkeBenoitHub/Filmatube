import type { Metadata } from "next";
import type { ReactNode } from "react";
import Link from "next/link";
import {
  ArrowLeft,
  CheckCircle2,
  CircleHelp,
  Download,
  FolderOpen,
  Heart,
  PlayCircle,
  ShieldCheck,
  Film,
  type LucideIcon,
} from "lucide-react";
import { LandingFooter } from "@/components/landing/LandingFooter";
import { LandingHeader } from "@/components/landing/LandingHeader";
import { APK, apkFileName } from "@/lib/apk";
import { getDict } from "@/lib/i18n/server";
import type { Dictionary } from "@/lib/i18n/dictionaries";

export const metadata: Metadata = {
  title: "Install the Android app — Filmatube",
  description: "Step-by-step guide to installing the Filmatube Android APK safely.",
};

type InstallDict = Dictionary["install"];

/* ── Phone mockups (decorative) ─────────────────────────────────────────── */

function PhoneFrame({ children }: { children: ReactNode }) {
  return (
    <div className="relative h-80 w-[168px] shrink-0 overflow-hidden rounded-[28px] border-2 border-surface-border bg-black shadow-2xl shadow-black/50">
      <div aria-hidden className="absolute left-1/2 top-0 z-10 h-4 w-13 -translate-x-1/2 rounded-b-xl bg-[#0a0a0a]" />
      {children}
    </div>
  );
}

function StatusBar() {
  return (
    <div className="flex justify-between px-2 pb-1 pt-2">
      <span className="text-[7px] text-white/50">9:41</span>
      <span className="text-[7px] text-white/50">5G ▮▮▮</span>
    </div>
  );
}

/** Step 1 — browser download complete */
function DownloadScreen({ d }: { d: InstallDict }) {
  return (
    <div className="flex h-full flex-col bg-[#1a1a1a]">
      <StatusBar />
      <div className="mx-1.5 mb-1.5 flex items-center gap-1 rounded-lg bg-white/5 px-1.5 py-1">
        <span className="h-1.5 w-1.5 rounded-full bg-brand-300" />
        <span className="text-[6.5px] text-white/50">filmatube.app</span>
      </div>
      <div className="mx-1.5 flex items-center gap-1.5 rounded-lg border border-brand-700/50 bg-brand-500/12 p-1.5">
        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-md bg-brand-500">
          <Film className="h-3 w-3 text-white" aria-hidden />
        </span>
        <div className="min-w-0 flex-1">
          <p className="truncate text-[7.5px] font-bold text-white">{apkFileName}</p>
          <p className="text-[6px] text-white/50">{d.mockDownloadComplete}</p>
        </div>
        <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-brand-300" aria-hidden />
      </div>
      <div className="flex-1" />
      <div className="m-1.5 flex items-center gap-1 rounded-md bg-white/5 p-1">
        <Download className="h-2.5 w-2.5 text-brand-400" aria-hidden />
        <span className="text-[6.5px] text-white/60">{d.mockTapOpen}</span>
      </div>
    </div>
  );
}

/** Step 2 — Settings: allow install from this source */
function UnknownSourcesScreen({ d }: { d: InstallDict }) {
  return (
    <div className="flex h-full flex-col bg-[#1a1a1a]">
      <div className="flex items-center gap-1 border-b border-white/10 bg-white/5 px-1.5 pb-1 pt-5">
        <ArrowLeft className="h-2.5 w-2.5 text-white/60" aria-hidden />
        <span className="text-[8px] font-semibold text-white">{d.mockUnknownApps}</span>
      </div>
      <div className="flex items-center gap-1.5 px-1.5 py-1.5">
        <span className="flex h-[22px] w-[22px] items-center justify-center rounded-md bg-[#4285F4] text-[10px] font-extrabold text-white">
          C
        </span>
        <div>
          <p className="text-[8px] font-semibold text-white">Chrome</p>
          <p className="text-[6px] text-white/40">{d.mockBrowser}</p>
        </div>
      </div>
      <div className="mx-1.5 h-px bg-white/10" />
      <div className="flex items-center justify-between border-l-2 border-brand-400 bg-brand-500/12 px-1.5 py-2">
        <div className="pr-1">
          <p className="text-[7.5px] font-semibold text-white">{d.mockAllowSource}</p>
          <p className="text-[6px] text-white/45">{d.mockAllowSourceDesc}</p>
        </div>
        <span className="relative h-3.5 w-[26px] shrink-0 rounded-full bg-brand-400">
          <span className="absolute right-[2px] top-1/2 h-2.5 w-2.5 -translate-y-1/2 rounded-full bg-white" />
        </span>
      </div>
    </div>
  );
}

/** Step 3 — installer dialog */
function InstallerScreen({ d }: { d: InstallDict }) {
  return (
    <div className="flex h-full flex-col items-center justify-center bg-[#1c1c1c] px-3.5">
      <span className="mb-1.5 flex h-11 w-11 items-center justify-center rounded-xl bg-brand-500 shadow-lg shadow-brand-500/40">
        <Film className="h-5 w-5 text-white" aria-hidden />
      </span>
      <p className="text-[9.5px] font-bold text-white">Filmatube</p>
      <p className="mb-3 text-center text-[6.5px] text-white/45">{d.mockInstallPrompt}</p>
      <div className="flex w-full gap-1.5">
        <span className="flex-1 rounded-md border border-white/20 py-1 text-center text-[7.5px] text-white/50">
          {d.mockCancel}
        </span>
        <span className="flex-1 rounded-md bg-brand-500 py-1 text-center text-[7.5px] font-bold text-white">
          {d.mockInstall}
        </span>
      </div>
      <div className="mt-3 flex items-center gap-1">
        <ShieldCheck className="h-2 w-2 text-white/30" aria-hidden />
        <span className="text-[5.5px] text-white/30">{d.mockPlayProtect}</span>
      </div>
    </div>
  );
}

/** Step 4 — app open */
function AppOpenScreen() {
  return (
    <div className="flex h-full flex-col bg-[#0a0a0a]">
      <div className="flex items-center gap-1 px-1.5 pb-1 pt-5">
        <Film className="h-3 w-3 text-brand-400" aria-hidden />
        <span className="text-[9px] font-extrabold text-brand-400">Filmatube</span>
      </div>
      {[0, 1, 2].map((i) => (
        <div key={i} className="flex items-center gap-1.5 px-1.5 py-1">
          <span className={`h-5 w-5 shrink-0 rounded ${i % 2 ? "bg-white/5" : "bg-white/10"}`} />
          <div className="flex-1">
            <span className="mb-1 block h-1 w-3/5 rounded bg-white/15" />
            <span className="block h-[3px] w-2/5 rounded bg-white/10" />
          </div>
          <Heart className={`h-2.5 w-2.5 ${i === 0 ? "text-brand-400" : "text-white/15"}`} aria-hidden />
        </div>
      ))}
      <div className="flex-1" />
      <div className="flex items-center gap-1.5 border-t border-white/10 bg-white/5 px-1.5 py-2">
        <span className="h-[18px] w-[18px] shrink-0 rounded bg-brand-500" />
        <div className="flex-1">
          <span className="mb-1 block h-[3.5px] w-1/2 rounded bg-white/20" />
          <span className="block h-[2.5px] w-1/3 rounded bg-white/10" />
        </div>
        <PlayCircle className="h-4 w-4 text-brand-400" aria-hidden />
      </div>
    </div>
  );
}

/* ── Page ───────────────────────────────────────────────────────────────── */

export default async function InstallPage() {
  const dict = await getDict();
  const d = dict.install;
  const hasApk = APK.url !== "";

  const steps: { icon: LucideIcon; title: string; body: string[]; Screen: (p: { d: InstallDict }) => ReactNode }[] = [
    { icon: Download, title: d.step1Title, body: [d.step1Body], Screen: DownloadScreen },
    { icon: ShieldCheck, title: d.step2Title, body: [d.step2Intro, d.step2Li1, d.step2Li2], Screen: UnknownSourcesScreen },
    { icon: FolderOpen, title: d.step3Title, body: [d.step3Body], Screen: InstallerScreen },
    { icon: PlayCircle, title: d.step4Title, body: [d.step4Body], Screen: AppOpenScreen },
  ];

  const troubleshooting = [d.tb1, d.tb2, d.tb3, d.tb4, d.tb5];

  return (
    <div className="flex min-h-screen flex-col bg-surface">
      <LandingHeader dict={dict} linkPrefix="/" />

      <main className="mx-auto w-full max-w-4xl flex-1 px-6 pb-20 pt-12 md:pt-16">
        <Link
          href="/"
          className="mb-8 inline-flex items-center gap-1.5 text-[13px] text-ink-faint transition-colors hover:text-brand-300"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden />
          {d.back}
        </Link>

        <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-gold">{d.badge}</p>
        <h1 className="mb-3 text-3xl font-black leading-[1.05] tracking-tight text-ink md:text-5xl">{d.heading}</h1>
        <p className="mb-8 max-w-xl text-base text-ink-muted">{d.subtitle}</p>

        {/* Safety note */}
        <div className="mb-10 flex gap-3 rounded-2xl border border-brand-700/40 bg-brand-900/20 p-5">
          <ShieldCheck className="h-5 w-5 shrink-0 text-brand-400" aria-hidden />
          <div>
            <h2 className="mb-1 text-[15px] font-bold text-ink">{d.safeTitle}</h2>
            <p className="text-sm leading-relaxed text-ink-muted">{d.safeBody}</p>
          </div>
        </div>

        {/* Steps */}
        <div className="flex flex-col gap-4">
          {steps.map((step, i) => (
            <div
              key={step.title}
              className="flex flex-col gap-6 rounded-[18px] border border-surface-border bg-surface-card/60 p-5 sm:flex-row sm:items-center md:p-7"
            >
              <div className="order-2 min-w-0 flex-1 sm:order-1">
                <div className="mb-3 flex items-center gap-3">
                  <div className="relative shrink-0">
                    <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-brand-700/25 text-brand-300">
                      <step.icon className="h-5 w-5" aria-hidden />
                    </span>
                    <span className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-brand-500 text-[11px] font-extrabold text-white">
                      {i + 1}
                    </span>
                  </div>
                  <h2 className="text-lg font-extrabold tracking-tight text-ink md:text-xl">{step.title}</h2>
                </div>
                {step.body.map((line) => (
                  <p key={line} className="mb-2 text-[14.5px] leading-relaxed text-ink-muted last:mb-0">
                    {line}
                  </p>
                ))}
                {i === 0 &&
                  (hasApk ? (
                    <a
                      href={APK.url}
                      download
                      className="mt-4 inline-flex h-10 items-center gap-2 rounded-xl bg-brand-500 px-5 text-sm font-bold text-white transition-colors hover:bg-brand-600"
                    >
                      <Download className="h-4 w-4" aria-hidden />
                      {d.downloadApk}
                    </a>
                  ) : (
                    <span className="mt-4 inline-flex h-10 items-center gap-2 rounded-xl border border-surface-border px-5 text-sm font-bold text-ink-faint">
                      <Download className="h-4 w-4" aria-hidden />
                      {d.apkSoon}
                    </span>
                  ))}
              </div>

              <div className="order-1 flex justify-center sm:order-2">
                <PhoneFrame>
                  <step.Screen d={d} />
                </PhoneFrame>
              </div>
            </div>
          ))}
        </div>

        {/* Troubleshooting */}
        <section className="mt-14">
          <div className="mb-5 flex items-center gap-2">
            <CircleHelp className="h-5 w-5 text-brand-400" aria-hidden />
            <h2 className="text-xl font-black tracking-tight text-ink md:text-2xl">{d.troubleshooting}</h2>
          </div>
          <div className="flex flex-col gap-3">
            {troubleshooting.map((item) => (
              <div key={item.q} className="rounded-xl border border-surface-border bg-surface-card/50 p-5">
                <h3 className="mb-1.5 flex items-center gap-2 text-[15px] font-bold text-ink">
                  <CheckCircle2 className="h-4 w-4 shrink-0 text-brand-400" aria-hidden />
                  {item.q}
                </h3>
                <p className="pl-6 text-sm leading-relaxed text-ink-muted">{item.a}</p>
              </div>
            ))}
          </div>
        </section>

        <p className="mt-12 text-center text-[13px] text-ink-faint">
          {d.footerNote} v{APK.version} · {APK.minAndroid}
        </p>
      </main>

      <LandingFooter t={dict.landing} />
    </div>
  );
}
