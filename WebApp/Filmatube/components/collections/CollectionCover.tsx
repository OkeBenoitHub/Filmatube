import { Layers } from "lucide-react";
import { cn } from "@/lib/utils";

// A handful of on-brand duotones. A collection with no cover gets a deterministic one (by title)
// so the same collection always looks the same, and a shelf of them reads as a varied set rather
// than a row of identical black boxes.
const GRADIENTS = [
  "from-brand-500 to-brand-800",
  "from-emerald-500 to-teal-800",
  "from-teal-500 to-brand-700",
  "from-green-600 to-emerald-900",
  "from-lime-600 to-emerald-800",
  "from-brand-600 to-green-900",
];

function pick(seed: string): string {
  let h = 0;
  for (let i = 0; i < seed.length; i += 1) h = (h * 31 + seed.charCodeAt(i)) | 0;
  return GRADIENTS[Math.abs(h) % GRADIENTS.length];
}

/**
 * A collection's cover: the uploaded image when there is one, otherwise a clean branded
 * placeholder (deterministic gradient + a Layers glyph and the title's monogram) — so empty
 * collections look intentional instead of like broken thumbnails.
 */
export function CollectionCover({
  coverUrl,
  title,
  className,
}: {
  coverUrl?: string;
  title?: string;
  className?: string;
}) {
  if (coverUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img src={coverUrl} alt="" className={cn("h-full w-full object-cover", className)} />
    );
  }
  const initial = (title ?? "").trim().charAt(0).toUpperCase();
  return (
    <div className={cn("relative flex h-full w-full items-center justify-center bg-gradient-to-br", pick(title ?? ""), className)}>
      {initial && (
        <span className="absolute inset-0 flex items-center justify-center text-7xl font-black text-white/15 select-none">
          {initial}
        </span>
      )}
      <Layers className="relative h-8 w-8 text-white/90" aria-hidden />
    </div>
  );
}
