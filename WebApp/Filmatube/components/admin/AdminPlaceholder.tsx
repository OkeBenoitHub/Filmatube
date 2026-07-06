/** Section placeholder used by admin pages that aren't built yet (Phase 3+). */
export function AdminPlaceholder({ title, comingSoon }: { title: string; comingSoon: string }) {
  return (
    <div>
      <h1 className="text-2xl font-bold text-ink">{title}</h1>
      <div className="mt-6 rounded-xl border border-dashed border-surface-border p-12 text-center text-ink-muted">
        {comingSoon}
      </div>
    </div>
  );
}
