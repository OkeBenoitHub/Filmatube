package com.filmatube.app.ui.profile

import androidx.annotation.StringRes
import com.filmatube.app.R

/** The badge catalogue (v1.3, Day 200) — ids match achievements/{uid}/badges/{badgeId}. */
enum class Badge(val id: String, val emoji: String, @StringRes val labelRes: Int, @StringRes val descRes: Int) {
    FIRST_WATCH("first_watch", "🎬", R.string.badge_first_watch, R.string.badge_first_watch_desc),
    BINGE_WATCHER("binge_watcher", "🍿", R.string.badge_binge_watcher, R.string.badge_binge_watcher_desc),
    CINEPHILE("cinephile", "🎥", R.string.badge_cinephile, R.string.badge_cinephile_desc),
    CRITIC("critic", "✍️", R.string.badge_critic, R.string.badge_critic_desc),
    SOCIAL_BUTTERFLY("social_butterfly", "🦋", R.string.badge_social_butterfly, R.string.badge_social_butterfly_desc),
    PREMIERE_GOER("premiere_goer", "🎟️", R.string.badge_premiere_goer, R.string.badge_premiere_goer_desc),
    RECRUITER("recruiter", "🤝", R.string.badge_recruiter, R.string.badge_recruiter_desc),
}
