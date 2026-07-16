import { MessagesSquare } from "lucide-react";
import { BoardForm } from "@/components/boards/BoardForm";
import { PageHero } from "@/components/ui/PageHero";
import { getDict } from "@/lib/i18n/server";

export default async function NewBoardPage() {
  const dict = await getDict();
  const c = dict.catalog;

  return (
    <div className="mx-auto max-w-3xl px-4 py-8 md:px-6">
      <PageHero
        icon={MessagesSquare}
        eyebrow={c.boardsEyebrow}
        title={c.createBoard}
        subtitle={c.createBoardSubtitle}
      />
      <div className="mt-12">
        <BoardForm dict={c} />
      </div>
    </div>
  );
}
