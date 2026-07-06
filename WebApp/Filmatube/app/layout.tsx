import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthProvider } from "@/components/providers/AuthProvider";
import { LocaleProvider } from "@/components/providers/LocaleProvider";
import { ToastProvider } from "@/components/ui/Toast";
import { getLocale } from "@/lib/i18n/server";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Filmatube — Watch. Share. Discuss.",
    template: "%s · Filmatube",
  },
  description:
    "Stream movies, follow people who share your taste, and talk about it all in real time.",
};

export default async function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  const locale = await getLocale();

  // `dark` class enables Tailwind's class-based dark mode app-wide (forced dark).
  return (
    <html lang={locale} className="dark">
      <body>
        <LocaleProvider initialLocale={locale}>
          <AuthProvider>
            <ToastProvider>{children}</ToastProvider>
          </AuthProvider>
        </LocaleProvider>
      </body>
    </html>
  );
}
