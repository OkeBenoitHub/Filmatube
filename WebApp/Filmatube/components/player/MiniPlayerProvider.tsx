"use client";

import { createContext, useContext, useState, type ReactNode } from "react";
import { usePathname } from "next/navigation";
import { PersistentPlayer } from "@/components/player/PersistentPlayer";
import type { Dictionary } from "@/lib/i18n/dictionaries";

export interface ActiveSubtitle {
  lang: string;
  url: string;
}

export interface UpNextMovie {
  id: string;
  poster: string;
  title: string;
}

export interface ActiveMovie {
  id: string;
  poster: string;
  title: string;
  subtitles: ActiveSubtitle[];
  upNext: UpNextMovie | null;
  /** Non-null when opened from a live watch-party lobby — enables the sync engine. */
  partyId: string | null;
}

interface MiniPlayerContext {
  open: (movie: ActiveMovie) => void;
  close: () => void;
}

const Ctx = createContext<MiniPlayerContext | null>(null);

export function useMiniPlayer(): MiniPlayerContext {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useMiniPlayer must be used within a MiniPlayerProvider");
  return ctx;
}

/**
 * Hosts a single persistent <video> above the routed content so playback survives
 * navigation: full-screen on /watch/[id], a floating mini-player elsewhere.
 */
export function MiniPlayerProvider({
  dict,
  children,
}: {
  dict: Dictionary["player"];
  children: ReactNode;
}) {
  const [active, setActive] = useState<ActiveMovie | null>(null);
  const pathname = usePathname();

  const onWatchRoute = active ? pathname === `/watch/${active.id}` : false;

  return (
    <Ctx.Provider value={{ open: setActive, close: () => setActive(null) }}>
      {children}
      {active && (
        <PersistentPlayer
          key={active.id}
          active={active}
          minimized={!onWatchRoute}
          dict={dict}
          onClose={() => setActive(null)}
        />
      )}
    </Ctx.Provider>
  );
}
