import { notFound } from "next/navigation";
import { MonitorPlay } from "lucide-react";
import { PartyLobby } from "@/components/parties/PartyLobby";
import { PageHero } from "@/components/ui/PageHero";
import { getCurrentUser } from "@/lib/auth/session";
import { getParty, getPartyMembers, isPartyMember } from "@/lib/parties";
import { getDict } from "@/lib/i18n/server";

export default async function PartyPage({ params }: { params: Promise<{ id: string }> }) {
  const [{ id }, user, dict] = await Promise.all([params, getCurrentUser(), getDict()]);
  const c = dict.catalog;

  // getParty returns null for non-members — parties are private, same as the read rule.
  const party = await getParty(id, user?.uid);
  if (!party) notFound();

  const [members, member] = await Promise.all([
    getPartyMembers(id),
    user ? isPartyMember(id, user.uid) : Promise.resolve(false),
  ]);

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 md:px-6">
      <PageHero icon={MonitorPlay} eyebrow={c.partyEyebrow} title={c.partyTitle} subtitle={c.partySubtitle} />
      <div className="mt-10">
        <PartyLobby initialParty={party} initialMembers={members} initialIsMember={member} dict={c} />
      </div>
    </div>
  );
}
