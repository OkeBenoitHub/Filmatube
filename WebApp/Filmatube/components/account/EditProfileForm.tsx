"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { doc, updateDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";
import { uploadAvatar } from "@/lib/upload/avatar";
import type { Dictionary } from "@/lib/i18n/dictionaries";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ErrorBanner } from "@/components/auth/AuthBits";
import { Avatar } from "./Avatar";

interface Props {
  uid: string;
  initialName: string;
  initialBio: string;
  initialAvatarUrl: string;
  dict: Dictionary["account"];
}

export function EditProfileForm({ uid, initialName, initialBio, initialAvatarUrl, dict }: Props) {
  const router = useRouter();
  const fileInput = useRef<HTMLInputElement>(null);

  const [name, setName] = useState(initialName);
  const [bio, setBio] = useState(initialBio);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<string>(initialAvatarUrl);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  function onPick(e: React.ChangeEvent<HTMLInputElement>) {
    const picked = e.target.files?.[0];
    if (picked) {
      setFile(picked);
      setPreview(URL.createObjectURL(picked));
    }
  }

  async function save(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSaving(true);
    try {
      const avatarUrl = file ? await uploadAvatar(file) : null;
      await updateDoc(doc(db, "users", uid), {
        displayName: name.trim(),
        bio: bio.trim(),
        ...(avatarUrl ? { avatarUrl } : {}),
      });
      router.push("/account");
      router.refresh();
    } catch {
      setError(dict.saveError);
      setSaving(false);
    }
  }

  return (
    <form onSubmit={save} className="mx-auto flex max-w-md flex-col items-center gap-5 px-6 py-10">
      <button
        type="button"
        onClick={() => fileInput.current?.click()}
        className="group relative rounded-full"
        aria-label={dict.changePhoto}
      >
        <Avatar url={preview || null} name={name} size={96} />
        <span className="absolute bottom-0 right-0 rounded-full bg-brand-500 p-1.5 text-white">✎</span>
      </button>
      <input ref={fileInput} type="file" accept="image/*" hidden onChange={onPick} />

      <div className="w-full space-y-3">
        <Input placeholder={dict.name} value={name} onChange={(e) => setName(e.target.value)} />
        <textarea
          placeholder={dict.bio}
          value={bio}
          onChange={(e) => setBio(e.target.value)}
          rows={3}
          className="w-full rounded-lg border border-surface-border bg-surface px-3 py-2 text-sm text-ink placeholder:text-ink-faint focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400"
        />
        {error && <ErrorBanner>{error}</ErrorBanner>}
        <Button type="submit" loading={saving} className="w-full">
          {dict.save}
        </Button>
      </div>
    </form>
  );
}
