import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import { LandingFooter } from "@/components/landing/LandingFooter";
import { Wordmark } from "@/components/Wordmark";
import { getDict } from "@/lib/i18n/server";

export default async function TermsPage() {
  const dict = await getDict();
  const g = dict.legal;
  return (
    <div className="flex min-h-screen flex-col bg-surface">
      <header className="border-b border-surface-border/60">
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center justify-between px-6">
          <Wordmark />
          <Link href="/" className="inline-flex items-center gap-1.5 text-sm text-ink-muted hover:text-ink">
            <ArrowLeft className="h-4 w-4" aria-hidden />
            {g.backHome}
          </Link>
        </div>
      </header>
      <main className="mx-auto w-full max-w-3xl flex-1 px-6 py-16">
        <h1 className="text-3xl font-bold text-ink">{g.termsTitle}</h1>
        <p className="mt-6 leading-relaxed text-ink-muted">{g.termsIntro}</p>
        <p className="mt-6 text-sm text-ink-faint">{g.termsContact}</p>
      </main>
      <LandingFooter t={dict.landing} />
    </div>
  );
}
