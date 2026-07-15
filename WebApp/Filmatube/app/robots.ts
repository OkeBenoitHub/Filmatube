import type { MetadataRoute } from "next";

const base = process.env.NEXT_PUBLIC_SITE_URL ?? "https://filmatube.app";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
      // Keep private/authed and API surfaces out of the index.
      disallow: ["/account", "/admin", "/api", "/inbox", "/notifications", "/home", "/library", "/collections"],
    },
    sitemap: `${base}/sitemap.xml`,
  };
}
