import type { ReactNode } from "react";

/**
 * Admin/CMS shell. The auth gate (isAdmin) + sidebar (Dashboard, Movies, Users,
 * Requests, Theater, Notifications, Analytics) are built on Day 26.
 */
export default function AdminLayout({ children }: Readonly<{ children: ReactNode }>) {
  return <section>{children}</section>;
}
