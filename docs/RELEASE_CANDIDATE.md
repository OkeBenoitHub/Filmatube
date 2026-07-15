# Android Release Candidate + Hardening Week Review (Day 119)

Closes **Phase 9 Week 17 (Days 113–119)** — the Android performance, quality and security hardening
week, ending in a signable release candidate.

## Release candidate
- **Version:** `versionName = "1.0.0-rc1"`, `versionCode = 1`.
- **Artifacts (R8 + resource shrinking):**
  | Output | Size |
  |---|---|
  | `app-release-unsigned.apk` | ~6 MB |
  | `app-release.aab` (Play upload) | ~11 MB |
- **Signing:** a `signingConfigs.release` reads from a gitignored `keystore.properties`
  (`storeFile`/`storePassword`/`keyAlias`/`keyPassword`). When present, `assembleRelease` /
  `bundleRelease` produce **signed** artifacts; when absent (CI / local checks) the release is
  unsigned. `keystore.properties`, `*.jks`, `*.keystore` are gitignored.

### To cut a signed RC (user action)
1. Create an upload keystore: `keytool -genkeypair -v -keystore filmatube-upload.jks -alias filmatube -keyalg RSA -keysize 2048 -validity 10000`.
2. Add `Android/Filmatube/keystore.properties` with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
3. `./gradlew bundleRelease` → upload `app-release.aab` to the Play Console (internal testing track).
4. Enable **App Check enforcement** once real devices pass Play Integrity (see `SECURITY_AUDIT.md`).

## Week 17 summary (Days 113–119)
| Day | Area | Outcome |
|---|---|---|
| 113 | Firestore queries/indexes/pagination | All paths index-backed + capped; bounded review/comment observers. `PERF_QUERY_AUDIT.md` |
| 114 | Coil / memory / APK / cold-start | Bounded image caches; **R8 + shrink → APK 28 MB→6 MB**; fixed latent release-only compile bug. `PERF_APP_AUDIT.md` |
| 115 | Crashlytics + Performance | (Perf plugin + Crashlytics already wired; reviewed.) |
| 116 | Unit tests + CI gating | Pure logic extracted + 8 unit tests; CI runs `testDebugUnitTest`. |
| 117 | Security | Tightened notifications rule (anti-forgery); verified download encryption + App Check. `SECURITY_AUDIT.md` |
| 118 | Accessibility + FR sweep | 48dp rating touch targets; EN/FR parity confirmed. `A11Y_L10N_AUDIT.md` |
| 119 | Release candidate + review | Signable RC config, `1.0.0-rc1`, APK/AAB build green. |

## Pre-launch checklist (carried forward)
- [ ] Provide `keystore.properties` and produce a **signed** AAB; smoke-test the R8 release build
      (auth, playback, downloads, uploads, serialization) before shipping.
- [ ] Enable **App Check enforcement** (Firestore/Auth) after Play Integrity passes on real devices.
- [ ] Security schema items from `SECURITY_AUDIT.md`: split user-doc PII (`email` → private subtree);
      enforce bans via a custom claim.
- [ ] `GOOGLE_SERVICES_JSON` GitHub secret so the Android CI can run on PRs.
- [ ] Optional: deploy the rating-aggregation Cloud Function (Blaze) already committed.

## Status
Both platforms green. Android RC (`1.0.0-rc1`) assembles as signed APK + AAB when a keystore is
provided. Phase 9 Week 17 hardening complete.
