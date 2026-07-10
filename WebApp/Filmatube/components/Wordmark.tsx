import Link from "next/link";
import { Film } from "lucide-react";
import { cn } from "@/lib/utils";

/** Filmatube logo mark + wordmark, linking home. */
export function Wordmark({ className, href = "/" }: { className?: string; href?: string }) {
  return (
    <Link href={href} aria-label="Filmatube" className={cn("flex items-center gap-2", className)}>
      <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-brand-500 to-brand-700">
        <Film className="h-4 w-4 text-white" aria-hidden />
      </span>
      <span className="text-lg font-bold tracking-tight text-ink">Filmatube</span>
    </Link>
  );
}
