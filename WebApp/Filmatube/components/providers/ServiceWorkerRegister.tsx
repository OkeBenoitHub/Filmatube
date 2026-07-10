"use client";

import { useEffect } from "react";

/** Registers the best-effort save-for-later service worker (no-op if unsupported). */
export function ServiceWorkerRegister() {
  useEffect(() => {
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.register("/sw.js").catch(() => {
        /* ignore */
      });
    }
  }, []);
  return null;
}
