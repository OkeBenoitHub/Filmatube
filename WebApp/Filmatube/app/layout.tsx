import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./globals.css";

export const metadata: Metadata = {
  title: "Filmatube",
  description: "Watch movies, follow people who share your taste, and discuss in real time.",
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  // `dark` class is set now so Tailwind's class-based dark mode (Day 9) applies app-wide.
  return (
    <html lang="en" className="dark">
      <body>{children}</body>
    </html>
  );
}
