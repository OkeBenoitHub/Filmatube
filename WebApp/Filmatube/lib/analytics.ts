import { getAnalytics, isSupported, logEvent } from "firebase/analytics";
import { firebaseApp } from "@/lib/firebase";

let ready: Promise<ReturnType<typeof getAnalytics> | null> | null = null;

/** Lazily resolve Firebase Analytics (browser + supported only). */
function analytics() {
  if (typeof window === "undefined") return Promise.resolve(null);
  if (!ready) {
    ready = isSupported()
      .then((ok) => (ok ? getAnalytics(firebaseApp) : null))
      .catch(() => null);
  }
  return ready;
}

/** Log a player analytics event (no-op if analytics is unavailable). */
export async function logPlayerEvent(name: string, params: Record<string, string>) {
  const a = await analytics();
  if (a) logEvent(a, name, params);
}
