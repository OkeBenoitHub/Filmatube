/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  poweredByHeader: false,
  compress: true,
  images: {
    remotePatterns: [
      { protocol: "https", hostname: "image.tmdb.org" }, // TMDB artwork
      { protocol: "https", hostname: "**.r2.dev" }, // R2 public dev domain
      { protocol: "https", hostname: "**.r2.cloudflarestorage.com" }, // R2 S3 endpoint
    ],
  },
  async headers() {
    return [
      {
        // Security headers on every response.
        source: "/:path*",
        headers: [
          { key: "X-Frame-Options", value: "DENY" },
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
          { key: "Strict-Transport-Security", value: "max-age=63072000; includeSubDomains; preload" },
        ],
      },
      {
        // Content-addressed brand assets never change → cache hard.
        source: "/:file(logo.png|icon-192.png|icon-512.png|apple-icon.png|icon.png|firebase-messaging-sw.js)",
        headers: [{ key: "Cache-Control", value: "public, max-age=31536000, immutable" }],
      },
      {
        source: "/manifest.webmanifest",
        headers: [{ key: "Cache-Control", value: "public, max-age=86400" }],
      },
    ];
  },
};

export default nextConfig;
