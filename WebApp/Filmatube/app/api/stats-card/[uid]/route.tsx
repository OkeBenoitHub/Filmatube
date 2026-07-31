import { ImageResponse } from "next/og";
import { getUserBadges } from "@/lib/achievements";
import { getUserStats, watchHours } from "@/lib/stats";
import { getUserProfile } from "@/lib/user";

export const runtime = "nodejs";

/**
 * Shareable stats card (Day 202) — a 1200×630 PNG of a user's watch stats, so sharing produces a
 * real image rather than a screenshot. Only aggregate, non-sensitive figures are drawn; the same
 * numbers already show on the public profile.
 */
export async function GET(_req: Request, { params }: { params: Promise<{ uid: string }> }) {
  const { uid } = await params;
  const [profile, stats, badges] = await Promise.all([
    getUserProfile(uid),
    getUserStats(uid),
    getUserBadges(uid),
  ]);

  const tiles: [string, string][] = [
    [String(watchHours(stats)), "HOURS WATCHED"],
    [String(stats.moviesCompleted), "MOVIES"],
    [String(badges.size), "BADGES"],
    [String(stats.currentStreak), "DAY STREAK"],
  ];

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          justifyContent: "space-between",
          background: "linear-gradient(135deg, #0b1210 0%, #0f1f18 55%, #12301f 100%)",
          padding: 64,
          color: "#F3F5F4",
          fontFamily: "sans-serif",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 20 }}>
          <div
            style={{
              width: 64,
              height: 64,
              borderRadius: 18,
              background: "linear-gradient(135deg, #22C55E, #15803D)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 34,
            }}
          >
            🎬
          </div>
          <div style={{ display: "flex", flexDirection: "column" }}>
            <span style={{ fontSize: 26, color: "#9BA8A2", letterSpacing: 2 }}>FILMATUBE</span>
            <span style={{ fontSize: 46, fontWeight: 800 }}>{profile?.displayName || "Filmatube viewer"}</span>
          </div>
        </div>

        <div style={{ display: "flex", gap: 20 }}>
          {tiles.map(([value, label]) => (
            <div
              key={label}
              style={{
                flex: 1,
                display: "flex",
                flexDirection: "column",
                gap: 6,
                padding: "28px 24px",
                borderRadius: 24,
                background: "rgba(255,255,255,0.05)",
                border: "1px solid rgba(255,255,255,0.10)",
              }}
            >
              <span style={{ fontSize: 68, fontWeight: 800, lineHeight: 1 }}>{value}</span>
              <span style={{ fontSize: 20, color: "#9BA8A2", letterSpacing: 1.5 }}>{label}</span>
            </div>
          ))}
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", fontSize: 24 }}>
          <span style={{ color: "#9BA8A2" }}>
            {stats.topGenres.length > 0 ? stats.topGenres.join("  ·  ").toUpperCase() : "MOVIES, TOGETHER"}
          </span>
          <span style={{ color: "#4ADE80", fontWeight: 700 }}>filmatube</span>
        </div>
      </div>
    ),
    {
      width: 1200,
      height: 630,
      // Stats change at most daily; cache so a shared card being unfurled by many clients
      // doesn't re-render the image each time (it's public and CPU-bound).
      headers: { "Cache-Control": "public, max-age=3600, s-maxage=86400" },
    },
  );
}
