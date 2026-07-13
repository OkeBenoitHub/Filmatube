"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { registerWithEmail, signInWithGoogle } from "@/lib/auth/actions";
import { errorCode, mapAuthError } from "@/lib/auth/errors";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { AuthShell } from "./AuthShell";
import { AuthDivider, ErrorBanner } from "./AuthBits";
import { GoogleButton } from "./GoogleButton";

export function RegisterForm({ dict }: { dict: Dictionary["auth"] }) {
  const router = useRouter();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const succeed = () => {
    // New accounts go through taste onboarding first.
    router.replace("/account/taste");
    router.refresh();
  };

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!name) return setError(dict.errors.nameRequired);
    if (!email) return setError(dict.errors.emailRequired);
    if (password.length < 6) return setError(dict.errors.passwordShort);
    if (password !== confirm) return setError(dict.errors.passwordMismatch);
    setLoading(true);
    try {
      await registerWithEmail(name.trim(), email.trim(), password);
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
      const { isNewUser } = await signInWithGoogle();
      if (isNewUser) {
        succeed(); // new user → taste onboarding
      } else {
        // Existing account signing in via the register page → straight to the app.
        router.replace("/home");
        router.refresh();
      }
    } catch (err) {
      setError(mapAuthError(errorCode(err), dict.errors));
      setLoading(false);
    }
  }

  return (
    <AuthShell title={dict.registerTitle} subtitle={dict.registerSubtitle}>
      <form onSubmit={submit} className="space-y-3">
        <Input placeholder={dict.name} value={name} autoComplete="name" onChange={(e) => setName(e.target.value)} />
        <Input type="email" placeholder={dict.email} value={email} autoComplete="email" onChange={(e) => setEmail(e.target.value)} />
        <Input type="password" placeholder={dict.password} value={password} autoComplete="new-password" onChange={(e) => setPassword(e.target.value)} />
        <Input type="password" placeholder={dict.confirmPassword} value={confirm} autoComplete="new-password" onChange={(e) => setConfirm(e.target.value)} />
        {error && <ErrorBanner>{error}</ErrorBanner>}
        <Button type="submit" loading={loading} className="w-full">
          {dict.createAccount}
        </Button>
      </form>

      <AuthDivider>{dict.orDivider}</AuthDivider>
      <GoogleButton label={dict.continueGoogle} onClick={google} disabled={loading} />

      <p className="mt-6 text-center text-sm text-ink-muted">
        {dict.haveAccount}{" "}
        <Link href="/login" className="font-medium text-brand-400 hover:underline">
          {dict.signIn}
        </Link>
      </p>
    </AuthShell>
  );
}
