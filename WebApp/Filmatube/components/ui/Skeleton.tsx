import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

/** Shimmering placeholder block (matches the Android shimmer). */
export function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("animate-pulse rounded-md bg-surface-hover", className)} {...props} />;
}
