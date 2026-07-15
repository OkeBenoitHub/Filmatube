import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";
import { AuthProvider } from "@/components/providers/AuthProvider";
import { LocaleProvider } from "@/components/providers/LocaleProvider";
import { MiniPlayerProvider } from "@/components/player/MiniPlayerProvider";
import { ServiceWorkerRegister } from "@/components/providers/ServiceWorkerRegister";
import { ToastProvider } from "@/components/ui/Toast";
import { getDict, getLocale } from "@/lib/i18n/server";
import "./globals.css";

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL ?? "https://filmatube.app"),
  title: {
    default: "Filmatube — Watch. Share. Discuss.",
    template: "%s · Filmatube",
  },
  description:
    "Stream movies, follow people who share your taste, and talk about it all in real time.",
  manifest: "/manifest.webmanifest",
  applicationName: "Filmatube",
  keywords: ["movies", "streaming", "social", "reviews", "watch together", "film"],
  openGraph: {
    type: "website",
    siteName: "Filmatube",
    title: "Filmatube — Watch. Share. Discuss.",
    description:
      "Stream movies, follow people who share your taste, and talk about it all in real time.",
    images: [{ url: "/og.png", width: 1200, height: 630, alt: "Filmatube" }],
  },
  twitter: {
    card: "summary_large_image",
    title: "Filmatube — Watch. Share. Discuss.",
    description:
      "Stream movies, follow people who share your taste, and talk about it all in real time.",
    images: ["/og.png"],
  },
};

export const viewport: Viewport = {
  themeColor: "#16a34a",
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
        <ServiceWorkerRegister />
      </body>
    </html>
  );
}
