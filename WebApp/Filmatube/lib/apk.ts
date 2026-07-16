/**
 * Android APK metadata for the landing download section, /install and /changelog.
 *
 * Values track the real build (`Android/Filmatube/app/build.gradle.kts`): versionName 1.0.0-rc1,
 * minSdk 24 (Android 7.0), ~5.5 MB for the R8-minified release APK. Override per environment —
 * `NEXT_PUBLIC_APK_URL` is what turns the download button live once the signed APK is hosted.
 */
export const APK = {
  version: process.env.NEXT_PUBLIC_APK_VERSION ?? "1.0.0-rc1",
  size: process.env.NEXT_PUBLIC_APK_SIZE ?? "5.5 MB",
  updated: process.env.NEXT_PUBLIC_APK_DATE ?? "July 2026",
  minAndroid: "Android 7.0+",
  /** Empty until a signed APK is published — the UI degrades to a "coming soon" state. */
  url: process.env.NEXT_PUBLIC_APK_URL ?? "",
} as const;

export const apkFileName = `filmatube_v${APK.version}.apk`;
