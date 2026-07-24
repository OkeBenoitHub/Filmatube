/** Badge catalogue (v1.3, Day 200) — order + emoji. Names/descriptions live in the dictionary. */
export const BADGE_IDS = [
  "first_watch",
  "binge_watcher",
  "cinephile",
  "critic",
  "social_butterfly",
  "premiere_goer",
  "recruiter",
] as const;

export type BadgeId = (typeof BADGE_IDS)[number];

export const BADGE_EMOJI: Record<BadgeId, string> = {
  first_watch: "🎬",
  binge_watcher: "🍿",
  cinephile: "🎥",
  critic: "✍️",
  social_butterfly: "🦋",
  premiere_goer: "🎟️",
  recruiter: "🤝",
};
