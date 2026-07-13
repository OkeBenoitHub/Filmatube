import type { Metadata } from "next";
import { LegalLayout } from "@/components/legal/LegalLayout";
import { getDict } from "@/lib/i18n/server";

export const metadata: Metadata = {
  title: "Terms of Service — Filmatube",
  description: "The rules for using Filmatube fairly and safely.",
};

export default async function TermsPage() {
  const dict = await getDict();
  return <LegalLayout doc={dict.legal.terms} dict={dict} />;
}
