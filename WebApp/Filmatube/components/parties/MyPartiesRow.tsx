import Link from "next/link";
import { MonitorPlay } from "lucide-react";
import type { Party } from "@/lib/parties";
import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Strip of my upcoming/live watch parties — mirrors the Android Community feed row. */
export function MyPartiesRow({ parties, dict }: { parties: Party[]; dict: Dictionary["catalog"] }) {
  if (parties.length === 0) return null;

  return (
    <section>
      <h2 className="text-lg font-bold text-ink">{dict.partyMyParties}</h2>
      <div className="mt-3 flex gap-4 overflow-x-auto pb-2">
        {parties.map((p) => (
          <Link key={p.id} href={`/parties/${p.id}`} className="group w-28 shrink-0">
            <div className="relative aspect-[2/3] overflow-hidden rounded-lg border border-surface-border bg-surface-hover">
              {p.moviePoster ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={p.moviePoster}
                  alt=""
                  className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                />
              ) : (
                <span className="flex h-full items-center justify-center">
                  <MonitorPlay className="h-6 w-6 text-ink-faint" aria-hidden />
                </span>
              )}
              {p.status === "live" && (
                <span className="absolute left-1.5 top-1.5 rounded bg-brand-500 px-1.5 py-0.5 text-[10px] font-bold text-white">
                  {dict.partyStatusLive}
                </span>
              )}
            </div>
            <p className="mt-1.5 truncate text-sm text-ink">{p.movieTitle}</p>
            <p className="truncate text-xs text-ink-faint">
              {p.status === "live"
                ? dict.partyStatusLive
                : new Date(p.scheduledAtMs).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
            </p>
          </Link>
        ))}
      </div>
    </section>
  );
}
