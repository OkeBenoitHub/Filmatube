"use client";

import { createContext, useCallback, useContext, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { LOCALE_COOKIE, type Locale } from "@/lib/i18n/config";
import { dictionaries, type Dictionary } from "@/lib/i18n/dictionaries";

interface I18nContextValue {
  locale: Locale;
  dict: Dictionary;
  setLocale: (locale: Locale) => void;
}

const I18nContext = createContext<I18nContextValue | null>(null);

/**
 * Client-side i18n context. The locale itself is resolved on the server (cookie /
 * Accept-Language) and passed in; switching writes the cookie and re-renders the
 * tree via router.refresh() so server components pick up the new language too.
 */
export function LocaleProvider({
  initialLocale,
  children,
}: {
  initialLocale: Locale;
  children: ReactNode;
}) {
  const router = useRouter();

  const setLocale = useCallback(
    (locale: Locale) => {
      document.cookie = `${LOCALE_COOKIE}=${locale};path=/;max-age=31536000;samesite=lax`;
      router.refresh();
    },
    [router],
  );

  return (
    <I18nContext.Provider
      value={{ locale: initialLocale, dict: dictionaries[initialLocale], setLocale }}
    >
      {children}
    </I18nContext.Provider>
  );
}

/** Read translations in client components: `const { dict, locale, setLocale } = useI18n()`. */
export function useI18n(): I18nContextValue {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error("useI18n must be used within a LocaleProvider");
  return ctx;
}
