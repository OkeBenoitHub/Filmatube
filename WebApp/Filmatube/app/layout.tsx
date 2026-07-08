import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AuthProvider } from "@/components/providers/AuthProvider";
import { LocaleProvider } from "@/components/providers/LocaleProvider";
import { MiniPlayerProvider } from "@/components/player/MiniPlayerProvider";
import { ToastProvider } from "@/components/ui/Toast";
import { getDict, getLocale } from "@/lib/i18n/server";
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
  const [locale, dict] = await Promise.all([getLocale(), getDict()]);

  // `dark` class enables Tailwind's class-based dark mode app-wide (forced dark).
  return (
    <html lang={locale} className="dark">
      <body>
        <LocaleProvider initialLocale={locale}>
          <AuthProvider>
            <MiniPlayerProvider dict={dict.player}>
              <ToastProvider>{children}</ToastProvider>
            </MiniPlayerProvider>
          </AuthProvider>
        </LocaleProvider>
      </body>
    </html>
  );
}
