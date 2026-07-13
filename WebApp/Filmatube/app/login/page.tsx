import { Suspense } from "react";
import { LoginForm } from "@/components/auth/LoginForm";
import { LandingFooter } from "@/components/landing/LandingFooter";
import { LandingHeader } from "@/components/landing/LandingHeader";
import { getDict } from "@/lib/i18n/server";

export default async function LoginPage() {
  const dict = await getDict();
  return (
    <div className="flex min-h-screen flex-col bg-surface">
      <LandingHeader dict={dict} linkPrefix="/" hideSignIn />
      <Suspense>
        <LoginForm dict={dict.auth} />
      </Suspense>
      <LandingFooter t={dict.landing} />
    </div>
  );
}
