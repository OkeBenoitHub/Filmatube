import Link from "next/link";
import { cn } from "@/lib/utils";

/** Filmatube logo mark + wordmark, linking home. */
export function Wordmark({ className, href = "/" }: { className?: string; href?: string }) {
  return (
    <Link href={href} aria-label="Filmatube" className={cn("flex items-center gap-2", className)}>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src="/logo.png" alt="" className="h-8 w-8" />
      <span className="text-lg font-bold tracking-tight text-ink">Filmatube</span>
    </Link>
  );
}
