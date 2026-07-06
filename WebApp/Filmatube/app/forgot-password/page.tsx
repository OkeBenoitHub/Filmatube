import { ForgotForm } from "@/components/auth/ForgotForm";
import { getDict } from "@/lib/i18n/server";

export default async function ForgotPasswordPage() {
  const dict = await getDict();
  return <ForgotForm dict={dict.auth} />;
}
