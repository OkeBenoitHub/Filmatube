import { Skeleton } from "@/components/ui/Skeleton";

/** Instant route-level skeleton shown while catalog pages fetch on the server. */
export default function CatalogLoading() {
  return (
    <div className="mx-auto max-w-6xl space-y-8 px-4 py-8 md:px-6">
      <Skeleton className="h-9 w-56" />
      <div className="space-y-3">
        <Skeleton className="h-5 w-40" />
        <div className="flex gap-3 overflow-hidden">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="aspect-[2/3] w-36 shrink-0 rounded-xl" />
          ))}
        </div>
      </div>
      <div className="space-y-3">
        <Skeleton className="h-5 w-32" />
        <div className="flex gap-3 overflow-hidden">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="aspect-[2/3] w-36 shrink-0 rounded-xl" />
          ))}
        </div>
      </div>
    </div>
  );
}
