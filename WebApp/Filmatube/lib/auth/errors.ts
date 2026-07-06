import type { Dictionary } from "@/lib/i18n/dictionaries";

/** Maps a Firebase Auth error code to a localized message. */
export function mapAuthError(code: string | undefined, errors: Dictionary["auth"]["errors"]): string {
  switch (code) {
    case "auth/invalid-credential":
    case "auth/wrong-password":
    case "auth/user-not-found":
      return errors.invalidCredentials;
    case "auth/email-already-in-use":
      return errors.emailInUse;
    case "auth/weak-password":
      return errors.passwordShort;
    case "auth/invalid-email":
      return errors.emailInvalid;
    case "auth/too-many-requests":
      return errors.tooMany;
    case "auth/network-request-failed":
      return errors.network;
    default:
      return errors.generic;
  }
}

/** Extracts the `code` from a thrown Firebase error. */
export function errorCode(e: unknown): string | undefined {
  if (typeof e === "object" && e !== null && "code" in e) {
    return String((e as { code: unknown }).code);
  }
  return undefined;
}
