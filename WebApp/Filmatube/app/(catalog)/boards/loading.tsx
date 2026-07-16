import { Skeleton } from "@/components/ui/Skeleton";

/** Instant skeleton for the boards pages (hero + grid). */
export default function BoardsLoading() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-8 md:px-6">
      <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-end">
        <Skeleton className="h-36 w-36 shrink-0 rounded-2xl sm:h-48 sm:w-48" />
        <div className="w-full space-y-3">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-12 w-64" />
          <Skeleton className="h-4 w-80 max-w-full" />
        </div>
      </div>
      <div className="mt-12 grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <Skeleton key={i} className="aspect-video rounded-xl" />
        ))}
      </div>
    </div>
  );
}
