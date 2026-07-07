import { redirect } from "next/navigation";
import { AppHeader } from "@/components/catalog/AppHeader";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict } from "@/lib/i18n/server";

/** Signed-in catalog shell: gates on the session cookie and mounts the app header. */
export default async function CatalogLayout({ children }: { children: React.ReactNode }) {
  const user = await getCurrentUser();
  if (!user) redirect("/login?next=/home");
  const dict = await getDict();

  return (
    <div className="min-h-screen bg-surface">
      <AppHeader dict={dict.catalog} />
      <main className="pb-16">{children}</main>
    </div>
  );
}
