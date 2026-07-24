"use client";

import { useEffect, useRef, useState, useTransition, type ComponentType } from "react";
import { useRouter } from "next/navigation";
import { Check, Globe, Link2, Lock, Info, Share2, Trash2 } from "lucide-react";
import { deleteCollection, saveCollection } from "@/app/collections/actions";
import type { Collection } from "@/lib/collections";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { cn } from "@/lib/utils";

interface Pos {
  top: number;
  left: number;
}

/**
 * Context menu for a collection thumbnail — opened by the 3-dot button or right-click. Mirrors
 * the movie menu (MovieMenu): positioned at the trigger, clamped to the viewport, closes on
 * outside click / Escape / scroll. Owner-only actions (visibility, delete) appear only when
 * [isOwner], matching who the server actions will actually let through.
 */
export function CollectionMenu({
  collection,
  position,
  dict,
  isOwner,
  onClose,
}: {
  collection: Collection;
  position: Pos;
  dict: Dictionary["catalog"];
  isOwner: boolean;
  onClose: () => void;
}) {
  const router = useRouter();
  const ref = useRef<HTMLDivElement>(null);
  const [coords, setCoords] = useState<Pos>(position);
  const [copied, setCopied] = useState(false);
  const [pending, start] = useTransition();

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const r = el.getBoundingClientRect();
    const pad = 8;
    let top = position.top;
    let left = position.left;
    if (left + r.width > window.innerWidth - pad) left = window.innerWidth - r.width - pad;
    if (top + r.height > window.innerHeight - pad) top = window.innerHeight - r.height - pad;
    setCoords({ top: Math.max(pad, top), left: Math.max(pad, left) });
  }, [position]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", onKey);
    window.addEventListener("scroll", onClose, true);
    return () => {
      document.removeEventListener("keydown", onKey);
      window.removeEventListener("scroll", onClose, true);
    };
  }, [onClose]);

  const url = typeof window !== "undefined" ? `${window.location.origin}/collections/${collection.id}` : "";

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(onClose, 900);
    } catch {
      onClose();
    }
  };

  const share = async () => {
    if (typeof navigator !== "undefined" && navigator.share) {
      try {
        await navigator.share({ title: collection.title, url });
      } catch {
        /* dismissed */
      }
      onClose();
    } else {
      void copyLink();
    }
  };

  const toggleVisibility = () => {
    start(async () => {
      await saveCollection(collection.id, {
        title: collection.title,
        coverUrl: collection.coverUrl,
        isPublic: !collection.isPublic,
      });
      onClose();
    });
  };

  const remove = () => {
    if (!confirm(dict.collectionDeleteConfirm)) return;
    start(async () => {
      await deleteCollection(collection.id);
      onClose();
    });
  };

  return (
    <div
      className="fixed inset-0 z-[60]"
      onMouseDown={onClose}
      onContextMenu={(e) => { e.preventDefault(); onClose(); }}
    >
      <div
        ref={ref}
        role="menu"
        style={{ top: coords.top, left: coords.left }}
        onMouseDown={(e) => e.stopPropagation()}
        className="absolute w-56 overflow-hidden rounded-xl border border-surface-border bg-surface-card shadow-2xl shadow-black/50"
      >
        <div className="border-b border-surface-border px-3 py-2.5">
          <p className="truncate text-sm font-semibold text-ink">{collection.title || dict.collectionUntitled}</p>
        </div>
        <div className="py-1">
          <Item icon={Info} label={dict.collectionOpen} onClick={() => { onClose(); router.push(`/collections/${collection.id}`); }} />
          <Item icon={copied ? Check : Link2} label={copied ? dict.copied : dict.copyLink} onClick={copyLink} accent={copied} />
          <Item icon={Share2} label={dict.shareAction} onClick={share} />
          {isOwner && (
            <>
              <Divider />
              <Item
                icon={collection.isPublic ? Lock : Globe}
                label={collection.isPublic ? dict.collectionMakePrivate : dict.collectionMakePublic}
                onClick={toggleVisibility}
                disabled={pending}
              />
              <Item icon={Trash2} label={dict.collectionDelete} onClick={remove} disabled={pending} danger />
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function Item({
  icon: Icon,
  label,
  onClick,
  accent,
  danger,
  disabled,
}: {
  icon: ComponentType<{ className?: string; "aria-hidden"?: boolean }>;
  label: string;
  onClick: () => void;
  accent?: boolean;
  danger?: boolean;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      role="menuitem"
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "flex w-full items-center gap-3 px-3 py-2 text-left text-sm transition-colors hover:bg-surface-hover disabled:opacity-50",
        danger ? "text-red-400" : "text-ink",
      )}
    >
      <Icon className={cn("h-4 w-4 shrink-0", danger ? "text-red-400" : accent ? "text-brand-400" : "text-ink-muted")} aria-hidden />
      {label}
    </button>
  );
}

function Divider() {
  return <div className="my-1 h-px bg-surface-border" />;
}
