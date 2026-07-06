import { RegisterForm } from "@/components/auth/RegisterForm";
import { getDict } from "@/lib/i18n/server";

export default async function RegisterPage() {
  const dict = await getDict();
  return <RegisterForm dict={dict.auth} />;
}
