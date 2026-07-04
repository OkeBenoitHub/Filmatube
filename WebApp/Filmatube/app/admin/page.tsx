import { LayoutDashboard } from "lucide-react";
import { FeaturePreview } from "@/components/FeaturePreview";
import { getDict } from "@/lib/i18n/server";

/** Admin auth gate + dashboard shell (sidebar, stat cards) are built on Days 26–27. */
export default async function AdminPage() {
  const dict = await getDict();
  return (
    <FeaturePreview
      icon={LayoutDashboard}
      title={dict.admin.title}
      subtitle={dict.admin.subtitle}
      badge={dict.common.comingSoon}
    />
  );
}
