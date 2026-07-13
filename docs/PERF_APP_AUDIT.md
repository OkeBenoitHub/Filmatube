# App Performance Audit — Coil, Memory, APK, Cold-start (Day 114)

## Image caching (Coil)
`FilmatubeApp.newImageLoader` now configures explicit two-tier caching:
- **Memory cache** — `maxSizePercent(0.25)` (25% of app RAM) for decoded bitmaps.
- **Disk cache** — 256 MB at `cacheDir/image_cache`, so posters/backdrops survive process death.
- `respectCacheHeaders(false)` — R2/CDN image URLs are content-addressed and immutable, so we
  ignore any `no-cache` headers and always serve from cache when present.
- `crossfade(true)` retained.

Result: poster grids and backdrops load from memory/disk after first view; far fewer network
round-trips and decodes when scrolling Home/Browse/Search.

## Memory & leak audit
| Area | Finding |
|---|---|
| ExoPlayer | Released in `PlayerViewModel.onCleared()`; `KEEP_SCREEN_ON` cleared with the view. ✓ |
| Firestore listeners | Every `addSnapshotListener` is inside a `callbackFlow { … awaitClose { registration.remove() } }` — no dangling listeners. ✓ |
| Coil bitmaps | Now bounded (25% RAM / 256 MB disk). ✓ |
| Singletons | Repositories inject `FirebaseFirestore`/`@ApplicationContext`; `DownloadUtil` holds a `SimpleCache` with the **application** context — no Activity leak. ✓ |
| Messaging service | Uses `EntryPointAccessors.fromApplication(applicationContext, …)` — no Activity capture. ✓ |
| Compose flows | Collected via `collectAsStateWithLifecycle` / `stateIn(WhileSubscribed)` — cancel with scope. ✓ |

No retained-Activity or listener leaks found.

## APK size
Enabled **R8 minify + resource shrinking** on the release build (`isMinifyEnabled = true`,
`isShrinkResources = true`) with `proguard-rules.pro` covering kotlinx.serialization generated
serializers, our `@Serializable` DTOs, Retrofit-annotated interfaces and `BuildConfig`.

| Build | APK |
|---|---|
| Debug (unshrunk, tooling) | ~28 MB |
| **Release (R8 + shrinkResources)** | **~6 MB** |

Also fixed a latent **release-only compile bug**: `DebugAppCheckProviderFactory`
(`debugImplementation`) was referenced unconditionally, so the release variant never compiled.
Moved provider selection into variant source sets — `src/debug/.../AppCheckProviderFactory.kt`
(debug provider) and `src/release/.../AppCheckProviderFactory.kt` (Play Integrity) — which also
keeps the debug provider **out** of release APKs.

> ⚠️ First R8 enablement: smoke-test a signed release build (auth, playback, downloads, uploads,
> serialization paths) before shipping. Keep rules are in place; verify at runtime.

## Cold-start
- `installSplashScreen()` is used (system splash → no blank window).
- `Application.onCreate` work is light: App Check auto-refresh (async), Crashlytics toggle, and
  idempotent notification-channel creation — no blocking I/O or network on the main thread.
- Firebase auto-inits from `google-services.json`; no eager heavy singletons.
- **Recommendation (future):** add a Baseline Profile via `androidx.benchmark:benchmark-macro` to
  AOT-compile the hot startup + first-scroll paths for an additional cold-start improvement.

## Status
Both variants build green — `assembleDebug` and `assembleRelease` (R8). Image caching bounded,
no leaks found, release APK ~6 MB.
