"use client";

import { useRef, useState } from "react";
import { CheckCircle2, UploadCloud } from "lucide-react";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/auth/AuthBits";

/** PUT a file to a presigned URL with progress via XHR (fetch can't report upload progress). */
function putWithProgress(url: string, file: File, onProgress: (pct: number) => void): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("PUT", url);
    xhr.setRequestHeader("Content-Type", file.type);
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) onProgress(Math.round((e.loaded / e.total) * 100));
    };
    xhr.onload = () => (xhr.status >= 200 && xhr.status < 300 ? resolve() : reject(new Error(String(xhr.status))));
    xhr.onerror = () => reject(new Error("network"));
    xhr.send(file);
  });
}

export function VideoUpload({
  videoKey,
  onUploaded,
  dict,
}: {
  videoKey: string;
  onUploaded: (key: string) => void;
  dict: Dictionary["adminMovies"];
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [error, setError] = useState(false);

  const uploading = progress !== null && progress < 100;

  async function onPick(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    setError(false);
    setProgress(0);
    try {
      const presignRes = await fetch("/api/uploads/presign", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ bucket: "videos", filename: file.name, contentType: file.type, prefix: "movies" }),
      });
      if (!presignRes.ok) throw new Error("presign");
      const { uploadUrl, key } = (await presignRes.json()) as { uploadUrl: string; key: string };
      await putWithProgress(uploadUrl, file, setProgress);
      onUploaded(key);
      setProgress(null);
    } catch {
      setError(true);
      setProgress(null);
    }
  }

  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-ink">{dict.video}</label>

      <div className="flex items-center gap-3">
        <Button type="button" variant="secondary" onClick={() => inputRef.current?.click()} disabled={uploading}>
          <UploadCloud className="mr-2 h-4 w-4" aria-hidden />
          {videoKey ? dict.replaceVideo : dict.uploadVideo}
        </Button>

        {videoKey && progress === null && (
          <span className="flex items-center gap-1.5 text-sm text-brand-300">
            <CheckCircle2 className="h-4 w-4" aria-hidden />
            {dict.videoUploaded}
          </span>
        )}
        {!videoKey && progress === null && <span className="text-sm text-ink-faint">{dict.noVideo}</span>}
      </div>

      {progress !== null && (
        <div className="space-y-1">
          <div className="h-2 w-full overflow-hidden rounded-full bg-surface-hover">
            <div className="h-full bg-brand-500 transition-all" style={{ width: `${progress}%` }} />
          </div>
          <p className="text-xs text-ink-muted">{dict.uploading} {progress}%</p>
        </div>
      )}

      {error && <ErrorBanner>{dict.uploadError}</ErrorBanner>}

      <input ref={inputRef} type="file" accept="video/*" hidden onChange={onPick} />
    </div>
  );
}
