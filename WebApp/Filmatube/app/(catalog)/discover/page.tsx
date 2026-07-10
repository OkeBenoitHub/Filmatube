import { SuggestionList } from "@/components/social/SuggestionList";
import { getCurrentUser } from "@/lib/auth/session";
import { getDict } from "@/lib/i18n/server";
import { getSuggestedUsers } from "@/lib/social";

export default async function DiscoverPage() {
  const [user, dict] = await Promise.all([getCurrentUser(), getDict()]);
  const suggestions = user ? await getSuggestedUsers(user.uid) : [];
  const c = dict.catalog;

  return (
    <div className="mx-auto max-w-2xl px-4 py-6 md:px-6">
      <h1 className="text-2xl font-bold text-ink">{c.discoverTitle}</h1>
      <div className="mt-6">
        <SuggestionList users={suggestions} dict={c} />
      </div>
    </div>
  );
}
