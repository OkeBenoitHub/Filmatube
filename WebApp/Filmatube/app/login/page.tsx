import { Suspense } from "react";
import { LoginForm } from "@/components/auth/LoginForm";
import { getDict } from "@/lib/i18n/server";

export default async function LoginPage() {
  const dict = await getDict();
  return (
    <Suspense>
      <LoginForm dict={dict.auth} />
    </Suspense>
  );
}
