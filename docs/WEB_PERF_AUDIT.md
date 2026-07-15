# Web Performance Audit (Day 120)

Next.js 15 App Router, `next build`.

## Applied
- **Caching headers** (`next.config.mjs headers()`): content-addressed brand assets
  (`logo.png`, `icon-192/512.png`, `apple-icon.png`, `firebase-messaging-sw.js`) →
  `Cache-Control: public, max-age=31536000, immutable`; `manifest.webmanifest` → 1-day cache.
- `poweredByHeader: false`, `compress: true`.
- **Code-splitting:** the App Router already splits per route; heavy client islands
  (`PersistentPlayer`, admin editors, social sections) are separate client chunks loaded only where
  mounted. Shared First-Load JS is **~101 kB** (good for a React 19 app).
- **Static asset caching:** `/_next/static/*` is hashed and served `immutable` by Next automatically.

## Image strategy
- `images.remotePatterns` allow TMDB + R2 hosts for `next/image` where used.
- Catalog posters/backdrops render via plain `<img>` straight from the **R2/CDN** origin (already
  edge-cached, correctly sized by the grid). This avoids routing every image through the Next
  optimizer (which on serverless adds per-image compute/cost) — an intentional trade-off. Revisit
  with `next/image` + a CDN loader if we later want AVIF/responsive srcsets.

## Lighthouse targets (manual, run against a deployed build)
Run `npx lighthouse https://<preview-url> --preset=desktop` and confirm:
- [ ] Performance ≥ 90 (landing + /home).
- [ ] LCP < 2.5 s — the landing hero is text/gradient (no blocking image); posters lazy-load.
- [ ] CLS < 0.1 — poster tiles reserve aspect-ratio boxes; the sticky header is fixed height.
- [ ] TBT low — minimal client JS on the landing (server components + `<details>` FAQ, no JS).
- [ ] "Serve static assets with efficient cache policy" passes (headers above).

## Follow-ups (not blocking)
- Consider `next/font` if a custom brand font is added (avoids layout shift).
- Optional route-level `dynamic = "force-static"` for the landing/legal pages once copy is final
  (they're currently dynamic for per-request locale — could pre-render both locales).

## Status
Build green, shared JS ~101 kB, caching headers in place. Lighthouse pass is a manual step against a
deployed preview.
