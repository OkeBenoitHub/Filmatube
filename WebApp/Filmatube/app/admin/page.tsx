import { Film, Inbox, Theater, Users, type LucideIcon } from "lucide-react";
import { Card } from "@/components/ui/Card";
import { Skeleton } from "@/components/ui/Skeleton";
import { getDict } from "@/lib/i18n/server";

/**
 * Dashboard skeleton. Real metrics + recent activity are wired to Firestore
 * aggregates in Phase 9 (Day 113).
 */
export default async function AdminDashboardPage() {
  const dict = await getDict();
  const a = dict.admin;

  const stats: { icon: LucideIcon; label: string }[] = [
    { icon: Users, label: a.users },
    { icon: Film, label: a.movies },
    { icon: Inbox, label: a.requests },
    { icon: Theater, label: a.showtimes },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-ink">{a.dashboard}</h1>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {stats.map((stat) => (
          <Card key={stat.label} className="p-4">
            <div className="flex items-center justify-between">
              <span className="text-sm text-ink-muted">{stat.label}</span>
              <stat.icon className="h-4 w-4 text-ink-faint" aria-hidden />
            </div>
            <Skeleton className="mt-3 h-7 w-16" />
          </Card>
        ))}
      </div>

      <Card className="p-4">
        <h2 className="mb-4 font-semibold text-ink">{a.recentActivity}</h2>
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="flex items-center gap-3">
              <Skeleton className="h-9 w-9 rounded-full" />
              <div className="flex-1 space-y-1.5">
                <Skeleton className="h-3 w-1/3" />
                <Skeleton className="h-3 w-1/2" />
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
