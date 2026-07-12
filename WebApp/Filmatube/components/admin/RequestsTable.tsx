"use client";

import { useState, useTransition } from "react";
import { Check, X } from "lucide-react";
import { decideRequest } from "@/app/admin/requests/actions";
import type { RequestRow } from "@/lib/admin/requests";
import type { Dictionary } from "@/lib/i18n/dictionaries";

export function RequestsTable({ requests, dict }: { requests: RequestRow[]; dict: Dictionary["admin"] }) {
  if (requests.length === 0) {
    return <p className="py-12 text-center text-ink-muted">{dict.requestsQueueEmpty}</p>;
  }
  return (
    <div className="space-y-3">
      {requests.map((r) => (
        <RequestCard key={r.id} request={r} dict={dict} />
      ))}
    </div>
  );
}

function RequestCard({ request, dict }: { request: RequestRow; dict: Dictionary["admin"] }) {
  const [reason, setReason] = useState("");
  const [movieId, setMovieId] = useState("");
  const [pending, startTransition] = useTransition();

  const inputCls =
    "w-full rounded-lg border border-surface-border bg-surface-hover px-3 py-1.5 text-sm text-ink placeholder:text-ink-faint focus:border-brand-500 focus:outline-none";

  return (
    <div className="rounded-xl border border-surface-border p-4">
      <div className="flex items-center justify-between gap-2">
        <p className="font-medium text-ink">{request.title}</p>
        <span
          className={
            request.status === "approved"
              ? "rounded-full bg-brand-500/15 px-2.5 py-0.5 text-xs font-semibold text-brand-400"
              : request.status === "rejected"
                ? "rounded-full bg-red-500/15 px-2.5 py-0.5 text-xs font-semibold text-red-400"
                : "rounded-full bg-amber-500/15 px-2.5 py-0.5 text-xs font-semibold text-amber-400"
          }
        >
          {request.status}
        </span>
      </div>
      {request.note && <p className="mt-1 text-sm text-ink-muted">{request.note}</p>}

      {request.status === "pending" && (
        <div className="mt-3 space-y-2">
          <div className="grid gap-2 sm:grid-cols-2">
            <input value={reason} onChange={(e) => setReason(e.target.value)} placeholder={dict.requestReason} className={inputCls} />
            <input value={movieId} onChange={(e) => setMovieId(e.target.value)} placeholder={dict.requestLinkMovie} className={inputCls} />
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              disabled={pending}
              onClick={() => startTransition(() => decideRequest(request.id, "approved", reason, movieId))}
              className="inline-flex items-center gap-1.5 rounded-lg bg-brand-500 px-3 py-1.5 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-50"
            >
              <Check className="h-4 w-4" aria-hidden />
              {dict.requestApprove}
            </button>
            <button
              type="button"
              disabled={pending}
              onClick={() => startTransition(() => decideRequest(request.id, "rejected", reason, ""))}
              className="inline-flex items-center gap-1.5 rounded-lg border border-surface-border px-3 py-1.5 text-sm text-ink-muted hover:bg-surface-hover disabled:opacity-50"
            >
              <X className="h-4 w-4" aria-hidden />
              {dict.requestReject}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
