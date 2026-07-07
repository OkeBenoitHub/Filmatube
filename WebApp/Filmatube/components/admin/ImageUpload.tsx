"use client";

import { useRef, useState } from "react";
import { ImagePlus } from "lucide-react";
import { uploadPublic } from "@/lib/upload/media";
import { Button } from "@/components/ui/Button";

/** Small "upload" button that puts an image in R2 and returns its public URL. */
export function ImageUpload({ label, onUploaded }: { label: string; onUploaded: (url: string) => void }) {
  const ref = useRef<HTMLInputElement>(null);
  const [busy, setBusy] = useState(false);

  async function onPick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setBusy(true);
    try {
      const url = await uploadPublic(file, "images");
      onUploaded(url);
    } catch {
      /* surfaced by the URL staying empty */
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <Button type="button" variant="outline" size="sm" onClick={() => ref.current?.click()} loading={busy}>
        <ImagePlus className="mr-1.5 h-3.5 w-3.5" aria-hidden />
        {label}
      </Button>
      <input ref={ref} type="file" accept="image/*" hidden onChange={onPick} />
    </>
  );
}
