import type { Metadata } from "next";
import { LegalLayout } from "@/components/legal/LegalLayout";
import { getDict } from "@/lib/i18n/server";

export const metadata: Metadata = {
  title: "Privacy Policy — Filmatube",
  description: "How Filmatube collects, uses, and protects your personal data.",
};

export default async function PrivacyPage() {
  const dict = await getDict();
  return <LegalLayout doc={dict.legal.privacy} dict={dict} />;
}
