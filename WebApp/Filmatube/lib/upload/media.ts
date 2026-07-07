/** Uploads a file to a public R2 bucket via the presign API and returns its public URL. */
export async function uploadPublic(file: File, bucket: "images" | "subtitles" | "avatars"): Promise<string> {
  const presignRes = await fetch("/api/uploads/presign", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ bucket, filename: file.name, contentType: file.type || "application/octet-stream" }),
  });
  if (!presignRes.ok) throw new Error("presign failed");
  const { uploadUrl, publicUrl } = (await presignRes.json()) as {
    uploadUrl: string;
    publicUrl: string | null;
  };

  const putRes = await fetch(uploadUrl, {
    method: "PUT",
    headers: { "Content-Type": file.type || "application/octet-stream" },
    body: file,
  });
  if (!putRes.ok) throw new Error("upload failed");
  if (!publicUrl) throw new Error("no public url");
  return publicUrl;
}
