import Link from "next/link";
import { MessagesSquare, Plus } from "lucide-react";
import { BoardCard } from "@/components/boards/BoardCard";
import { BoardSearch } from "@/components/boards/BoardSearch";
import { PageHero } from "@/components/ui/PageHero";
import { getCurrentUser } from "@/lib/auth/session";
import { getBoards, getFeaturedBoards, getMyBoards, BOARD_TYPES } from "@/lib/boards";
import { getDict } from "@/lib/i18n/server";
import { cn } from "@/lib/utils";

export default async function BoardsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const [user, dict, sp] = await Promise.all([getCurrentUser(), getDict(), searchParams]);
  const c = dict.catalog;

  const raw = typeof sp.type === "string" ? sp.type : "";
  const type = raw === BOARD_TYPES.MOVIE || raw === BOARD_TYPES.GENERAL ? raw : undefined;
  const q = (typeof sp.q === "string" ? sp.q : "").trim();

  const [featured, allBoards, mine] = await Promise.all([
    getFeaturedBoards(),
    getBoards(type, 200),
    user ? getMyBoards(user.uid) : Promise.resolve([]),
  ]);

  // Firestore has no substring search — match title/description/movie over the fetched page.
  const needle = q.toLowerCase();
  const boards = needle
    ? allBoards.filter((b) =>
        [b.title, b.description, b.movieTitle].some((f) => f.toLowerCase().includes(needle)),
      )
    : allBoards;

  const filters = [
    { value: "", label: c.boardsFilterAll },
    { value: BOARD_TYPES.MOVIE, label: c.boardsFilterMovies },
    { value: BOARD_TYPES.GENERAL, label: c.boardsFilterGeneral },
  ];

  return (
    <div className="mx-auto max-w-6xl px-4 py-8 md:px-6">
      <PageHero icon={MessagesSquare} eyebrow={c.boardsEyebrow} title={c.boards} subtitle={c.boardsSubtitle}>
        <Link
          href="/boards/new"
          className="inline-flex h-10 items-center gap-2 rounded-full bg-brand-500 px-5 text-sm font-semibold text-white transition-colors hover:bg-brand-600"
        >
          <Plus className="h-4 w-4" aria-hidden />
          {c.newBoard}
        </Link>
      </PageHero>

      {featured.length > 0 && !q && (
        <section className="mt-12">
          <h2 className="text-lg font-bold text-ink">{c.boardsFeatured}</h2>
          <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
            {featured.map((b) => (
              <BoardCard key={b.id} board={b} dict={c} />
            ))}
          </div>
        </section>
      )}

      {mine.length > 0 && !q && (
        <section className="mt-12">
          <h2 className="text-lg font-bold text-ink">{c.boardsMine}</h2>
          <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
            {mine.map((b) => (
              <BoardCard key={b.id} board={b} dict={c} />
            ))}
          </div>
        </section>
      )}

      <section className="mt-12">
        <div className="flex flex-wrap items-center gap-3">
          <h2 className="text-lg font-bold text-ink">{c.boardsAll}</h2>
          <div className="order-last w-full sm:order-none sm:ml-auto sm:w-auto">
            <BoardSearch initialQuery={q} type={type} placeholder={c.boardSearchPlaceholder} />
          </div>
          <div className="flex gap-1.5">
            {filters.map((f) => {
              const active = (type ?? "") === f.value;
              const params = new URLSearchParams();
              if (f.value) params.set("type", f.value);
              if (q) params.set("q", q);
              const qs = params.toString();
              return (
                <Link
                  key={f.value || "all"}
                  href={qs ? `/boards?${qs}` : "/boards"}
                  aria-current={active ? "page" : undefined}
                  className={cn(
                    "rounded-full border px-3 py-1 text-xs font-semibold transition-colors",
                    active
                      ? "border-brand-500 bg-brand-500 text-white"
                      : "border-surface-border text-ink-muted hover:bg-surface-hover hover:text-ink",
                  )}
                >
                  {f.label}
                </Link>
              );
            })}
          </div>
        </div>

        {boards.length === 0 ? (
          <p className="py-16 text-center text-ink-muted">{q ? c.boardsNoResults : c.boardsEmpty}</p>
        ) : (
          <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
            {boards.map((b) => (
              <BoardCard key={b.id} board={b} dict={c} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
