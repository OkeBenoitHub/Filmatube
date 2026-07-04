import "server-only";

import { cookies, headers } from "next/headers";
import { defaultLocale, isLocale, LOCALE_COOKIE, type Locale } from "./config";
import { dictionaries, type Dictionary } from "./dictionaries";

/** Resolve the request locale: explicit cookie first, then Accept-Language, then default. */
export async function getLocale(): Promise<Locale> {
  const cookieValue = (await cookies()).get(LOCALE_COOKIE)?.value;
  if (isLocale(cookieValue)) return cookieValue;

  const acceptLanguage = (await headers()).get("accept-language") ?? "";
  const first = acceptLanguage.split(",")[0]?.trim().toLowerCase();
  if (first?.startsWith("fr")) return "fr";

  return defaultLocale;
}

/** Dictionary for the current request — use in server components/pages. */
export async function getDict(): Promise<Dictionary> {
  return dictionaries[await getLocale()];
}
