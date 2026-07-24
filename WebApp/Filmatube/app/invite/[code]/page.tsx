import type { Metadata } from "next";
import Link from "next/link";
import { Gift } from "lucide-react";
import { Wordmark } from "@/components/Wordmark";
import { SetRefCookie } from "@/components/referral/SetRefCookie";
import { getCurrentUser } from "@/lib/auth/session";
import { getUserProfile } from "@/lib/user";
import { getDict } from "@/lib/i18n/server";

export const metadata: Metadata = { title: "You're invited — Filmatube" };

/**
 * Public invite landing (code = the referrer's uid). Drops the referral cookie, then points the
 * visitor at sign-up — the session route attributes the referral on first sign-up. Already-signed-
 * in visitors just get a link into the app; you can't refer someone who already has an account.
 */
export default async function InvitePage({ params }: { params: Promise<{ code: string }> }) {
  const { code } = await params;
  const [dict, referrer, viewer] = await Promise.all([
    getDict(),
    getUserProfile(code),
    getCurrentUser(),
  ]);
  const c = dict.referral;
  const name = referrer?.displayName?.trim() || dict.catalog.aFriend;

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-8 bg-surface px-4 py-16">
      <SetRefCookie code={code} />
      <Wordmark href="/" />

      <div className="w-full max-w-md rounded-2xl border border-surface-border bg-surface-card p-8 text-center shadow-2xl shadow-black/40">
        <div className="mx-auto mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-700 shadow-lg shadow-brand-900/40">
          <Gift className="h-7 w-7 text-white" aria-hidden />
        </div>
        <h1 className="text-2xl font-bold text-ink">{c.inviteHeading.replace("{name}", name)}</h1>
        <p className="mt-2 text-sm text-ink-muted">{c.inviteSubtitle}</p>

        {viewer ? (
          <Link
            href="/home"
            className="mt-6 inline-flex h-11 w-full items-center justify-center rounded-lg bg-brand-600 px-4 font-semibold text-white hover:bg-brand-500"
          >
            {c.inviteOpenApp}
          </Link>
        ) : (
          <>
            <Link
              href="/register"
              className="mt-6 inline-flex h-11 w-full items-center justify-center rounded-lg bg-brand-600 px-4 font-semibold text-white hover:bg-brand-500"
            >
              {c.inviteCta}
            </Link>
            <Link href="/login" className="mt-3 inline-block text-sm text-brand-400 hover:text-brand-300">
              {c.inviteSignIn}
            </Link>
          </>
        )}
      </div>
    </main>
  );
}
