# Cloudflare R2 setup

One-time manual setup for the buckets that back Filmatube media. The app code (`WebApp/Filmatube/lib/r2.ts`)
talks to R2 via the S3-compatible API; these steps create the buckets and apply CORS + lifecycle.

## 1. Create buckets
Dashboard → R2 → Create bucket (or via wrangler):

```bash
wrangler r2 bucket create filmatube-videos
wrangler r2 bucket create filmatube-images
wrangler r2 bucket create filmatube-avatars
wrangler r2 bucket create filmatube-subtitles
```

- `filmatube-images`, `filmatube-avatars`, `filmatube-subtitles` → enable **public access** (or attach a custom
  domain) and set `R2_PUBLIC_BASE_URL` to that domain.
- `filmatube-videos` → keep **private**; served only via short-lived presigned/token URLs (Day 12).

## 2. Create an R2 API token
Dashboard → R2 → Manage R2 API Tokens → Create (Object Read & Write). Note the
**Access Key ID**, **Secret Access Key**, and your **Account ID**.

## 3. Fill env (`WebApp/Filmatube/.env.local`)
```
R2_ACCOUNT_ID=...
R2_ACCESS_KEY_ID=...
R2_SECRET_ACCESS_KEY=...
R2_BUCKET_VIDEOS=filmatube-videos
R2_BUCKET_IMAGES=filmatube-images
R2_BUCKET_AVATARS=filmatube-avatars
R2_BUCKET_SUBTITLES=filmatube-subtitles
# Each public bucket has its OWN public URL (pub-*.r2.dev or custom domain). videos = private.
R2_PUBLIC_URL_IMAGES=https://pub-xxxx.r2.dev
R2_PUBLIC_URL_AVATARS=https://pub-yyyy.r2.dev
R2_PUBLIC_URL_SUBTITLES=https://pub-zzzz.r2.dev
```

## 4. Apply CORS + lifecycle
Using the AWS CLI pointed at R2 (credentials = the R2 token above):

```bash
ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
for B in filmatube-videos filmatube-images filmatube-avatars filmatube-subtitles; do
  aws s3api put-bucket-cors      --bucket "$B" --cors-configuration file://cors.json           --endpoint-url "$ENDPOINT"
  aws s3api put-bucket-lifecycle-configuration --bucket "$B" --lifecycle-configuration file://lifecycle.json --endpoint-url "$ENDPOINT"
done
```

Update `cors.json` `AllowedOrigins` with the real production web origin before applying.
