import type { Metadata } from "next";
import Link from "next/link";
import { ArrowLeft, Bug, Sparkles, Wrench } from "lucide-react";
import { LandingFooter } from "@/components/landing/LandingFooter";
import { LandingHeader } from "@/components/landing/LandingHeader";
import { APK } from "@/lib/apk";
import { getDict } from "@/lib/i18n/server";
import type { Dictionary } from "@/lib/i18n/dictionaries";

export const metadata: Metadata = {
  title: "Changelog — Filmatube",
  description: "What's new in each release of the Filmatube Android app.",
};

type ChangeType = "new" | "improved" | "fixed";

const TYPE_CONFIG: Record<ChangeType, { icon: typeof Sparkles; labelKey: keyof Dictionary["changelog"] }> = {
  new: { icon: Sparkles, labelKey: "typeNew" },
  improved: { icon: Wrench, labelKey: "typeImproved" },
  fixed: { icon: Bug, labelKey: "typeFixed" },
};

/** Each type gets its own hue so the log scans quickly — green stays the brand/primary. */
const TYPE_STYLE: Record<ChangeType, string> = {
  new: "border-brand-700/50 bg-brand-700/20 text-brand-300",
  improved: "border-sky-500/30 bg-sky-500/10 text-sky-300",
  fixed: "border-gold/30 bg-gold/10 text-gold",
};

export default async function ChangelogPage() {
  const dict = await getDict();
  const c = dict.changelog;

  const releases: { version: string; date: string; tag?: string; changes: { type: ChangeType; text: string }[] }[] = [
    {
      version: APK.version,
      date: c.v1Date,
      tag: c.v1Tag,
      changes: [
        { type: "new", text: c.c1 },
        { type: "new", text: c.c2 },
        { type: "new", text: c.c3 },
        { type: "new", text: c.c4 },
        { type: "new", text: c.c5 },
        { type: "new", text: c.c6 },
        { type: "new", text: c.c7 },
        { type: "new", text: c.c8 },
        { type: "new", text: c.c9 },
        { type: "new", text: c.c10 },
      ],
    },
  ];

  return (
    <div className="flex min-h-screen flex-col bg-surface">
      <LandingHeader dict={dict} linkPrefix="/" />

      <main className="mx-auto w-full max-w-3xl flex-1 px-6 pb-20 pt-12 md:pt-16">
        <Link
          href="/"
          className="mb-8 inline-flex items-center gap-1.5 text-[13px] text-ink-faint transition-colors hover:text-brand-300"
        >
          <ArrowLeft className="h-4 w-4" aria-hidden />
          {c.back}
        </Link>

        <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-gold">{c.badge}</p>
        <h1 className="mb-2 text-3xl font-black leading-[1.05] tracking-tight text-ink md:text-5xl">{c.heading}</h1>
        <p className="text-base text-ink-muted">{c.subtitle}</p>

        <div className="my-10 h-px bg-surface-border" />

        {/* Legend */}
        <div className="mb-8 flex flex-wrap gap-3">
          {(Object.entries(TYPE_CONFIG) as [ChangeType, (typeof TYPE_CONFIG)[ChangeType]][]).map(
            ([type, { icon: Icon, labelKey }]) => (
              <span
                key={type}
                className={`inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-[11px] font-bold ${TYPE_STYLE[type]}`}
              >
                <Icon className="h-3 w-3" aria-hidden />
                {c[labelKey]}
              </span>
            ),
          )}
        </div>

        {/* Releases */}
        <div className="flex flex-col gap-6">
          {releases.map((release) => (
            <article
              key={release.version}
              className="overflow-hidden rounded-[20px] border border-surface-border bg-surface-card/40"
            >
              <header className="flex flex-col gap-2 border-b border-surface-border/70 bg-surface-card/60 px-5 py-5 sm:flex-row sm:items-center sm:justify-between md:px-7">
                <div className="flex items-center gap-3">
                  <h2 className="text-xl font-extrabold tracking-tight text-ink md:text-2xl">v{release.version}</h2>
                  {release.tag && (
                    <span className="rounded-lg border border-brand-700/50 bg-brand-700/25 px-2.5 py-1 text-[11px] font-bold text-brand-300">
                      {release.tag}
                    </span>
                  )}
                </div>
                <p className="text-[13px] text-ink-faint">{release.date}</p>
              </header>

              <div className="px-5 py-2 md:px-7">
                {release.changes.map((change) => {
                  const { icon: Icon, labelKey } = TYPE_CONFIG[change.type];
                  return (
                    <div
                      key={change.text}
                      className="flex items-start gap-3 border-b border-surface-border/40 py-3 last:border-b-0"
                    >
                      <span
                        className={`mt-0.5 inline-flex shrink-0 items-center gap-1 rounded-md border px-2 py-0.5 text-[10px] font-bold ${TYPE_STYLE[change.type]}`}
                      >
                        <Icon className="h-3 w-3" aria-hidden />
                        {c[labelKey]}
                      </span>
                      <p className="text-sm leading-relaxed text-ink-muted">{change.text}</p>
                    </div>
                  );
                })}
              </div>
            </article>
          ))}
        </div>

        <p className="mt-10 text-center text-[13px] text-ink-faint">{c.footerNote}</p>
      </main>

      <LandingFooter t={dict.landing} />
    </div>
  );
}
