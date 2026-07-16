import { Download, Smartphone } from "lucide-react";
import { getDict } from "@/lib/i18n/server";

const PLAY_STORE = "https://play.google.com/store/apps/details?id=com.filmatube.app";

/** Info page: offline downloads live in the Android app; the web offers PWA save-for-later. */
export default async function DownloadsInfoPage() {
  const dict = await getDict();
  const c = dict.catalog;

  return (
    <div className="mx-auto flex min-h-[70vh] max-w-xl flex-col items-center justify-center gap-5 px-6 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-full border border-surface-border bg-surface-hover">
        <Download className="h-7 w-7 text-brand-400" aria-hidden />
      </div>
      <h1 className="text-2xl font-bold text-ink">{c.offlineTitle}</h1>
      <p className="text-sm leading-relaxed text-ink-muted">{c.offlineBody}</p>
      <a
        href={PLAY_STORE}
        target="_blank"
        rel="noopener noreferrer"
        className="inline-flex h-11 items-center whitespace-nowrap gap-2 rounded-lg bg-brand-500 px-6 text-sm font-semibold text-white transition-colors hover:bg-brand-600"
      >
        <Smartphone className="h-4 w-4" aria-hidden />
        {c.getApp}
      </a>
    </div>
  );
}
