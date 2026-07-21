import { notFound } from "next/navigation";
import { Clapperboard } from "lucide-react";
import { ShowtimeRoom } from "@/components/theater/ShowtimeRoom";
import { PageHero } from "@/components/ui/PageHero";
import { getCurrentUser } from "@/lib/auth/session";
import { getMyAttendance, getShowtime, getShowtimeAttendees } from "@/lib/theater";
import { getDict } from "@/lib/i18n/server";

export default async function ShowtimePage({ params }: { params: Promise<{ id: string }> }) {
  const [{ id }, user, dict] = await Promise.all([params, getCurrentUser(), getDict()]);
  const c = dict.catalog;

  // Unlike a party, a showtime is public — any signed-in visitor may look at the lineup.
  const showtime = await getShowtime(id);
  if (!showtime) notFound();

  const [attendees, attendance] = await Promise.all([
    getShowtimeAttendees(id),
    user ? getMyAttendance(id, user.uid) : Promise.resolve({ going: false, remind: false }),
  ]);

  return (
    <div className="mx-auto max-w-4xl px-4 py-8 md:px-6">
      <PageHero
        icon={Clapperboard}
        eyebrow={c.theaterEyebrow}
        title={c.theaterShowtimeTitle}
        subtitle={c.theaterSubtitle}
      />
      <div className="mt-10">
        <ShowtimeRoom
          initialShowtime={showtime}
          initialAttendees={attendees}
          initialAttendance={attendance}
          dict={c}
        />
      </div>
    </div>
  );
}
