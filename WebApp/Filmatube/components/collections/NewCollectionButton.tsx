"use client";

import { useTransition } from "react";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { createCollection } from "@/app/collections/actions";

export function NewCollectionButton({ label }: { label: string }) {
  const router = useRouter();
  const [pending, startTransition] = useTransition();

  return (
    <button
      type="button"
      disabled={pending}
      onClick={() =>
        startTransition(async () => {
          const id = await createCollection();
          router.push(`/collections/${id}`);
        })
      }
      className="inline-flex h-9 items-center gap-2 rounded-lg bg-brand-500 px-4 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
    >
      <Plus className="h-4 w-4" aria-hidden />
      {label}
    </button>
  );
}
