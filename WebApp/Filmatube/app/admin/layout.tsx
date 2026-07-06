import type { ReactNode } from "react";
import { AdminShell } from "@/components/admin/AdminShell";
import { requireAdmin } from "@/lib/auth/guards";
import { getDict } from "@/lib/i18n/server";

/**
 * Admin/CMS shell. Gated by the `admin` custom claim (requireAdmin redirects
 * non-admins). Middleware already ensures a session cookie is present.
 */
export default async function AdminLayout({ children }: Readonly<{ children: ReactNode }>) {
  await requireAdmin();
  const dict = await getDict();
  return (
    <AdminShell dict={dict.admin} signOutLabel={dict.account.signOut}>
      {children}
    </AdminShell>
  );
}
