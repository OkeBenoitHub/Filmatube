/** Uploads an avatar to R2 via the presign API (cookie session) and returns the public URL. */
export async function uploadAvatar(file: File): Promise<string> {
  const presignRes = await fetch("/api/uploads/presign", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ bucket: "avatars", filename: file.name, contentType: file.type }),
  });
  if (!presignRes.ok) throw new Error("presign failed");
  const { uploadUrl, publicUrl } = (await presignRes.json()) as {
    uploadUrl: string;
    publicUrl: string | null;
  };

  const putRes = await fetch(uploadUrl, {
    method: "PUT",
    headers: { "Content-Type": file.type },
    body: file,
  });
  if (!putRes.ok) throw new Error("upload failed");
  if (!publicUrl) throw new Error("no public url");
  return publicUrl;
}
