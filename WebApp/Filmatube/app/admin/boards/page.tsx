import { BoardsTable } from "@/components/admin/BoardsTable";
import { getAllBoards } from "@/lib/boards";
import { getDict } from "@/lib/i18n/server";

export default async function AdminBoardsPage() {
  const [dict, boards] = await Promise.all([getDict(), getAllBoards()]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-ink">{dict.admin.boardsTitle}</h1>
        <p className="mt-1 text-sm text-ink-muted">{dict.admin.boardsSubtitle}</p>
      </div>
      <BoardsTable boards={boards} dict={dict.admin} />
    </div>
  );
}
