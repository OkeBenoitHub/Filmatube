import { RegisterForm } from "@/components/auth/RegisterForm";
import { LandingFooter } from "@/components/landing/LandingFooter";
import { LandingHeader } from "@/components/landing/LandingHeader";
import { redirectIfSignedIn } from "@/lib/auth/guards";
import { getDict } from "@/lib/i18n/server";

export default async function RegisterPage() {
  await redirectIfSignedIn();
  const dict = await getDict();
  return (
    <div className="flex min-h-screen flex-col bg-surface">
      <LandingHeader dict={dict} linkPrefix="/" />
      <RegisterForm dict={dict.auth} />
      <LandingFooter t={dict.landing} />
    </div>
  );
}
