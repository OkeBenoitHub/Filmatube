# Auth Parity QA — Android ↔ Web (Day 28)

Verifies that the **same Firebase account** behaves identically on Android and Web.
Both clients talk to the same Firebase project (**filmatubelive**) and the same Firestore/R2.

## Guaranteed by design
- **One backend:** identical Firebase Auth + Firestore + R2 for both platforms.
- **Identical user doc:** `users/{uid}` defaults are the same on first sign-in —
  Android `UserRepositoryImpl.ensureUserDocument` and Web `lib/user.ts ensureUserDocument`
  both write `email, displayName, bio, avatarUrl, language, followersCount, followingCount,
  genrePreferences[], contentLanguage, tasteCompleted:false, isAdmin:false, isBanned:false,
  createdAt, lastActiveAt`.
- **Same admin model:** the `admin` custom claim gates admin everywhere; set via
  `scripts/set-admin.mjs` (web) and it applies to Android too.
- **Same taste keys:** genre keys (`action`…`western`), `contentLanguage` (`en|fr|both`),
  and `language` (`en|fr`) are shared, so taste set on one platform is understood by the other.
- **Realtime sync:** profile edits/stats use Firestore listeners, so a change on one client
  appears on the other without a refresh.

## Session model (expected difference, not a bug)
- **Android:** Firebase client session (persisted by the SDK).
- **Web:** httpOnly `__session` cookie minted from the ID token by `/api/auth/session`;
  route protection via `middleware.ts` (cookie presence) + `requireUser`/`requireAdmin`.

## Manual checklist
Run web with `npm run dev` (in `WebApp/Filmatube`); Android on an emulator/device with Play services.

- [ ] **Register on Web** (email/password) → redirected to taste onboarding → pick genres/lang →
      lands on home. Firestore shows `users/{uid}` with `tasteCompleted:true`.
- [ ] **Sign in same account on Android** → goes straight to app (taste already done), profile
      shows the same display name.
- [ ] **Edit bio/name on Android** → open `/account` on Web → the change is reflected.
- [ ] **Upload avatar** on either platform (needs R2 configured) → appears on both.
- [ ] **Google Sign-In** works on both (same Google account → same `uid`, one user doc).
- [ ] **Forgot password** sends the reset email from both.
- [ ] **Language toggle** (Android settings / Web switcher) persists to the user doc and
      switches UI language.
- [ ] **Multi-profiles**: profiles created on Android appear under `/account/profiles` on Web
      (both read `users/{uid}/profiles`).
- [ ] **Admin claim**: after `set-admin.mjs`, sign out/in → `/admin` is accessible on Web and
      admin-bucket uploads work on Android.

## Status
Both builds green as of Week 4 close (Android `assembleDebug`, Web `next build`).
Runtime checklist above is for manual verification once R2 buckets + a running web server are available.
