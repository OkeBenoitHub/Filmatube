"use client";

import { useState } from "react";
import Link from "next/link";
import { MailCheck } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { sendReset } from "@/lib/auth/actions";
import { errorCode, mapAuthError } from "@/lib/auth/errors";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { AuthShell } from "./AuthShell";
import { ErrorBanner } from "./AuthBits";

export function ForgotForm({ dict }: { dict: Dictionary["auth"] }) {
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!email.trim()) return setError(dict.errors.emailRequired);
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) return setError(dict.errors.emailInvalid);
    setLoading(true);
    try {
      await sendReset(email.trim());
      setSent(true);
    } catch (err) {
      // Privacy: unknown email still shows "sent".
      if (errorCode(err) === "auth/user-not-found") {
        setSent(true);
      } else {
        setError(mapAuthError(errorCode(err), dict.errors));
      }
      setLoading(false);
    }
  }

  if (sent) {
    return (
      <AuthShell title={dict.forgotSentTitle} subtitle={dict.forgotSentMessage}>
        <div className="flex flex-col items-center gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-brand-700/25">
            <MailCheck className="h-7 w-7 text-brand-300" aria-hidden />
          </div>
          <Link
            href="/login"
            className="text-sm font-medium text-brand-400 hover:underline"
          >
            {dict.backToLogin}
          </Link>
        </div>
      </AuthShell>
    );
  }

  return (
    <AuthShell title={dict.forgotTitle} subtitle={dict.forgotSubtitle}>
      <form onSubmit={submit} className="space-y-3">
        <Input type="email" placeholder={dict.email} value={email} autoComplete="email" onChange={(e) => setEmail(e.target.value)} />
        {error && <ErrorBanner>{error}</ErrorBanner>}
        <Button type="submit" loading={loading} className="w-full">
          {dict.forgotSend}
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-ink-muted">
        <Link href="/login" className="font-medium text-brand-400 hover:underline">
          {dict.backToLogin}
        </Link>
      </p>
    </AuthShell>
  );
}
