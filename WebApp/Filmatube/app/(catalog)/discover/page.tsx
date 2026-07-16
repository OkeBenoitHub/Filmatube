import { UsersRound } from "lucide-react";
import { SuggestionList } from "@/components/social/SuggestionList";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict } from "@/lib/i18n/server";
import { getSuggestedUsers } from "@/lib/social";

export default async function DiscoverPage() {
  const [user, dict] = await Promise.all([getCurrentUser(), getDict()]);
  const suggestions = user ? await getSuggestedUsers(user.uid) : [];
  const c = dict.catalog;

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 md:px-6">
      {/* ── Hero header (Spotitube pattern, green) ─────────────── */}
      <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-end">
        <div className="flex h-36 w-36 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-900 shadow-2xl shadow-brand-900/50 sm:h-48 sm:w-48">
          <UsersRound className="h-16 w-16 text-white sm:h-20 sm:w-20" aria-hidden />
        </div>
        <div className="text-center sm:text-left">
          <p className="text-xs font-bold uppercase tracking-widest text-ink-muted">{c.discoverEyebrow}</p>
          <h1 className="mt-1 text-4xl font-black leading-none tracking-tight text-ink md:text-6xl">
            {c.discoverPeople}
          </h1>
          <p className="mt-2 text-sm text-ink-muted">{c.discoverSubtitle}</p>
        </div>
      </div>

      {/* ── Suggestions ────────────────────────────────────────── */}
      <section className="mt-12">
        <h2 className="text-lg font-bold text-ink">{c.discoverTitle}</h2>
        <div className="mt-4">
          <SuggestionList users={suggestions} dict={c} />
        </div>
      </section>
    </div>
  );
}
