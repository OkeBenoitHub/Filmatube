import { ForgotForm } from "@/components/auth/ForgotForm";
import { LandingFooter } from "@/components/landing/LandingFooter";
import { LandingHeader } from "@/components/landing/LandingHeader";
import { getDict } from "@/lib/i18n/server";

export default async function ForgotPasswordPage() {
  const dict = await getDict();
  return (
    <div className="flex min-h-screen flex-col bg-surface">
      <LandingHeader dict={dict} linkPrefix="/" />
      <ForgotForm dict={dict.auth} />
      <LandingFooter t={dict.landing} />
    </div>
  );
}
