import { HomeClient } from "@/components/catalog/HomeClient";
import { getDict, getLocale } from "@/lib/i18n/server";

/**
 * Home is client-rendered (see HomeClient): with Firestore's IndexedDB persistence the rows
 * paint instantly from the local cache and live-update, instead of paying two server-side
 * Firestore round-trips per navigation. The server shell only resolves i18n — the (catalog)
 * layout already gates auth.
 */
export default async function CatalogHomePage() {
  const [locale, dict] = await Promise.all([getLocale(), getDict()]);
  return <HomeClient dict={dict.catalog} genresDict={dict.genres} locale={locale} />;
}
