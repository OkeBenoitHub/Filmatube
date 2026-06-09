import type { Metadata } from "next";
import type { ReactNode } from "react";
import { ToastProvider } from "@/components/ui/Toast";
import "./globals.css";

export const metadata: Metadata = {
  title: "Filmatube",
  description: "Watch movies, follow people who share your taste, and discuss in real time.",
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  // `dark` class enables Tailwind's class-based dark mode app-wide.
  return (
    <html lang="en" className="dark">
      <body>
        <ToastProvider>{children}</ToastProvider>
      </body>
    </html>
  );
}
