/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  images: {
    remotePatterns: [
      { protocol: "https", hostname: "image.tmdb.org" }, // TMDB artwork
      { protocol: "https", hostname: "**.r2.dev" }, // R2 public dev domain
      { protocol: "https", hostname: "**.r2.cloudflarestorage.com" }, // R2 S3 endpoint
    ],
  },
};

export default nextConfig;
