import { MonitorPlay } from "lucide-react";
import { FeaturePreview } from "@/components/FeaturePreview";
import { getDict } from "@/lib/i18n/server";

/** The real player route (`/watch/[id]`) and web player land in Phase 4 (Day 50). */
export default async function WatchPage() {
  const dict = await getDict();
  return (
    <FeaturePreview
      icon={MonitorPlay}
      title={dict.watch.title}
      subtitle={dict.watch.subtitle}
      badge={dict.common.comingSoon}
    />
  );
}
