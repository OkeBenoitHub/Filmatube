"use client";

import { useTransition } from "react";
import { Loader2, Plus } from "lucide-react";
import { createCollection } from "@/app/collections/actions";

export function NewCollectionButton({ label }: { label: string }) {
  const [pending, startTransition] = useTransition();

  return (
    <button
      type="button"
      disabled={pending}
      onClick={() =>
        startTransition(async () => {
          // The action creates the doc and redirects server-side (one round-trip).
          await createCollection();
        })
      }
      className="inline-flex h-9 items-center gap-2 rounded-lg bg-brand-500 px-4 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
    >
      {pending ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden /> : <Plus className="h-4 w-4" aria-hidden />}
      {label}
    </button>
  );
}
