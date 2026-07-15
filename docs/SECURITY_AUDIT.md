# Security Audit — App Check, Download Encryption, Rules (Day 117)

## App Check
- **Client:** installed in `FilmatubeApp.initAppCheck()` with `setTokenAutoRefreshEnabled(true)`.
  Provider is variant-specific — **debug** provider in `src/debug` (register the debug token in
  Firebase), **Play Integrity** in `src/release`. The debug provider is not shipped in release APKs.
- **Web:** App Check is not yet wired on the web client (server routes are protected by the httpOnly
  session cookie + admin claim). Optional hardening: add reCAPTCHA-Enterprise App Check for the web.
- **⚠️ Console action (required for enforcement):** enable **App Check enforcement** for
  Cloud Firestore, Authentication and (if used) Cloud Functions in the Firebase console. Until
  enforcement is turned on, App Check tokens are collected but not required. Do this after confirming
  real devices pass Play Integrity in internal testing (enforcing too early blocks legitimate users).

## Download encryption (verified)
`DownloadEncryption` implements **envelope encryption**:
- A 256-bit **master key** is generated in the **Android Keystore** (`AES/GCM/NoPadding`,
  non-exportable, `PURPOSE_ENCRYPT|DECRYPT`).
- A random 16-byte **content key** is generated once, wrapped (AES-GCM, 128-bit tag, random IV)
  by the master key, and stored Base64 (`iv:ciphertext`) in private `SharedPreferences`.
- Media3 encrypts downloaded segments at rest with the content key (`AesCipherDataSink`) and decrypts
  transparently on offline playback (`AesCipherDataSource`).
- The master key never leaves the Keystore; the stored blob is useless without it. ✔️ Sound.

## Firestore rules audit
Reviewed every collection. The `admin` custom claim (not the users doc) is authoritative; all
per-user subtrees are owner-gated; catalog is admin-write.

**Fixed this pass**
- **`users/{uid}/notifications` create was unauthenticated-broad** — any signed-in user could write
  arbitrary notifications (including forged `system` broadcasts with malicious deep links) into any
  inbox. Now requires `actorId == request.auth.uid` **and** `type != 'system'` (system/broadcast
  notifications are written only by the admin server, which bypasses rules). Deployed.

**Verified safe**
- `follows`, `reports`, `reviews`/`comments`/`ratings`, `feed` create — all validate the writer's
  uid (`followerId`/`reporterId`/`userId`/`actorId == auth.uid`).
- `movies` read gated on `status == 'published'` (or admin); write admin-only.
- `broadcasts` admin-only; `fcmTokens` / `settings` self-only; `collections` owner/public.

**Known findings (tracked for launch hardening — not code-changed this pass)**
1. **User-doc PII exposure:** `users/{uid}` is readable by any signed-in user (needed for public
   profiles: displayName/avatar/bio/genres), but the doc also holds `email`. Firestore can't filter
   fields on read → move `email` (and any private fields) to a `users/{uid}/private/*` subtree or a
   separate private doc, and keep only public profile fields in `users/{uid}`. Schema change touching
   both clients — do before public launch.
2. **Ban enforcement:** `isBanned` is a data field but rules don't block a banned user's reads/writes
   (their Firebase Auth stays valid). Enforce via a **custom claim** `banned: true` set by an admin
   action + checked in an `isSignedIn`-style helper, rather than an in-rule `get()` on the user doc.
3. **Abuse rate-limiting:** client-side fan-out (feed/notifications) is attributable but not
   rate-limited; a Cloud Function gateway would allow throttling/spam controls.

## Status
Rules re-deployed to filmatubelive. Download encryption verified. App Check installed on Android
(enforcement is a console toggle). Two schema/claim hardening items tracked for the launch checklist.
