"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Search } from "lucide-react";

/**
 * Board search box. Pushes `?q=` (preserving the type filter) after a short debounce —
 * the server page does the matching, so results survive a reload or a shared link.
 */
export function BoardSearch({ initialQuery, type, placeholder }: { initialQuery: string; type?: string; placeholder: string }) {
  const router = useRouter();
  const [value, setValue] = useState(initialQuery);

  useEffect(() => {
    if (value === initialQuery) return;
    const t = setTimeout(() => {
      const params = new URLSearchParams();
      if (type) params.set("type", type);
      if (value.trim()) params.set("q", value.trim());
      const qs = params.toString();
      router.replace(qs ? `/boards?${qs}` : "/boards");
    }, 300);
    return () => clearTimeout(t);
  }, [value, initialQuery, type, router]);

  return (
    <div className="relative">
      <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-faint" aria-hidden />
      <input
        type="search"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder={placeholder}
        aria-label={placeholder}
        className="h-9 w-full rounded-full border border-surface-border bg-surface pl-9 pr-3 text-sm text-ink placeholder:text-ink-faint focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 sm:w-64"
      />
    </div>
  );
}
