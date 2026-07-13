"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { signInWithEmail, signInWithGoogle } from "@/lib/auth/actions";
import { errorCode, mapAuthError } from "@/lib/auth/errors";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { AuthShell } from "./AuthShell";
import { AuthDivider, ErrorBanner } from "./AuthBits";
import { GoogleButton } from "./GoogleButton";

export function LoginForm({ dict }: { dict: Dictionary["auth"] }) {
  const router = useRouter();
  const nextParam = useSearchParams().get("next");
  // Only allow internal paths — blocks open redirects (//evil.com, /\evil.com, https://…).
  const next =
    nextParam && nextParam.startsWith("/") && !nextParam.startsWith("//") && !nextParam.includes("\\")
      ? nextParam
      : "/home";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const succeed = () => {
    router.replace(next);
    router.refresh();
  };

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!email) return setError(dict.errors.emailRequired);
    if (!password) return setError(dict.errors.passwordRequired);
    setLoading(true);
    try {
      await signInWithEmail(email.trim(), password);
      succeed();
    } catch (err) {
      setError(mapAuthError(errorCode(err), dict.errors));
      setLoading(false);
    }
  }

  async function google() {
    setError(null);
    setLoading(true);
    try {
      await signInWithGoogle();
      succeed();
    } catch (err) {
      setError(mapAuthError(errorCode(err), dict.errors));
      setLoading(false);
    }
  }

  return (
    <AuthShell title={dict.loginTitle} subtitle={dict.loginSubtitle}>
      <form onSubmit={submit} className="space-y-3">
        <Input type="email" placeholder={dict.email} value={email} autoComplete="email" onChange={(e) => setEmail(e.target.value)} />
        <Input type="password" placeholder={dict.password} value={password} autoComplete="current-password" onChange={(e) => setPassword(e.target.value)} />
        <div className="text-right">
          <Link href="/forgot-password" className="text-xs text-ink-muted hover:text-ink">
            {dict.forgotPassword}
          </Link>
        </div>
        {error && <ErrorBanner>{error}</ErrorBanner>}
        <Button type="submit" loading={loading} className="w-full">
          {dict.signIn}
        </Button>
      </form>

      <AuthDivider>{dict.orDivider}</AuthDivider>
      <GoogleButton label={dict.continueGoogle} onClick={google} disabled={loading} />

      <p className="mt-6 text-center text-sm text-ink-muted">
        {dict.noAccount}{" "}
        <Link href="/register" className="font-medium text-brand-400 hover:underline">
          {dict.signUp}
        </Link>
      </p>
    </AuthShell>
  );
}
