import { cn } from "@/lib/utils";

/** Circular avatar: image when present, otherwise a green gradient circle with the initial. */
export function Avatar({
  url,
  name,
  size = 96,
  className,
}: {
  url?: string | null;
  name: string;
  size?: number;
  className?: string;
}) {
  const initial = name.trim().charAt(0).toUpperCase() || "?";
  if (url) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={url}
        alt={name}
        width={size}
        height={size}
        className={cn("rounded-full object-cover", className)}
        style={{ width: size, height: size }}
      />
    );
  }
  return (
    <div
      className={cn(
        "flex items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-brand-700 font-bold text-white",
        className,
      )}
      style={{ width: size, height: size, fontSize: size * 0.4 }}
    >
      {initial}
    </div>
  );
}
