package com.filmatube.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.domain.model.UserProfile
import com.filmatube.app.domain.util.DataState
import com.filmatube.app.ui.components.ErrorView
import com.filmatube.app.ui.components.FilmatubeSecondaryButton
import com.filmatube.app.ui.components.LoadingView
import com.filmatube.app.ui.components.PageHero
import com.filmatube.app.ui.components.QuickLinkCard
import com.filmatube.app.ui.components.StatCard
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeGold
import com.filmatube.app.ui.theme.FilmatubeSpacing

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenReferral: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val followerCount by viewModel.followerCount.collectAsStateWithLifecycle()
    val followingCount by viewModel.followingCount.collectAsStateWithLifecycle()
    val unreadNotifications by viewModel.unreadNotifications.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            DataState.Loading -> LoadingView()
            is DataState.Error -> ErrorView(error = s.error, onRetry = {})
            DataState.Empty -> LoadingView()
            is DataState.Success -> ProfileContent(
                profile = s.data,
                followerCount = followerCount,
                followingCount = followingCount,
                unreadNotifications = unreadNotifications,
                onEditProfile = onEditProfile,
                onOpenSettings = onOpenSettings,
                onOpenLibrary = onOpenLibrary,
                onOpenReferral = onOpenReferral,
                onOpenFollowers = onOpenFollowers,
                onOpenFollowing = onOpenFollowing,
                onOpenSuggestions = onOpenSuggestions,
                onOpenInbox = onOpenInbox,
                onOpenNotifications = onOpenNotifications,
                badges = badges,
                stats = stats,
            )
        }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    followerCount: Int,
    followingCount: Int,
    unreadNotifications: Int,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenReferral: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenInbox: () -> Unit,
    badges: Set<String>,
    stats: com.filmatube.app.data.achievements.UserStats,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
    ) {
        // Hero — the account page's shape, with the avatar standing in for the icon tile.
        PageHero(
            eyebrow = stringResource(R.string.profile_eyebrow),
            title = profile.displayName.ifBlank { stringResource(R.string.nav_profile) },
            subtitle = profile.bio.ifBlank { stringResource(R.string.profile_subtitle) },
            tile = {
                // Matched to PageHero's icon tile so Profile's header is the same weight as
                // every other screen's, rather than leading with an outsized portrait.
                UserAvatar(url = profile.avatarUrl, name = profile.displayName, size = 68.dp)
            },
            trailing = {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                    )
                }
            },
        )

        if (profile.isAdmin) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = FilmatubeGold.copy(alpha = 0.18f),
                modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg),
            ) {
                Text(
                    text = stringResource(R.string.profile_admin),
                    style = MaterialTheme.typography.labelMedium,
                    color = FilmatubeGold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        // Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            StatCard(
                value = followerCount.toString(),
                label = stringResource(R.string.profile_followers),
                onClick = onOpenFollowers,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = followingCount.toString(),
                label = stringResource(R.string.profile_following),
                onClick = onOpenFollowing,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = stats.moviesCompleted.toString(),
                label = stringResource(R.string.profile_watched),
                modifier = Modifier.weight(1f),
            )
        }

        FilmatubeSecondaryButton(
            text = stringResource(R.string.profile_edit),
            onClick = onEditProfile,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.lg),
        )

        // Destinations, spelled out. These were four unlabelled icon buttons in the header,
        // which gave no clue where any of them went.
        Column(
            modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
        ) {
            QuickLinkCard(
                icon = Icons.Outlined.VideoLibrary,
                title = stringResource(R.string.my_library),
                description = stringResource(R.string.profile_link_library_desc),
                onClick = onOpenLibrary,
            )
            QuickLinkCard(
                icon = Icons.Outlined.CardGiftcard,
                title = stringResource(R.string.referral_title),
                description = stringResource(R.string.profile_link_referral_desc),
                onClick = onOpenReferral,
            )
            QuickLinkCard(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.notifications_title),
                description = stringResource(R.string.profile_link_notifications_desc),
                badgeCount = unreadNotifications,
                onClick = onOpenNotifications,
            )
            QuickLinkCard(
                icon = Icons.Outlined.MailOutline,
                title = stringResource(R.string.inbox_title),
                description = stringResource(R.string.profile_link_inbox_desc),
                onClick = onOpenInbox,
            )
            QuickLinkCard(
                icon = Icons.Outlined.PersonAddAlt,
                title = stringResource(R.string.discover_people),
                description = stringResource(R.string.profile_link_people_desc),
                onClick = onOpenSuggestions,
            )
            QuickLinkCard(
                icon = Icons.Outlined.Settings,
                title = stringResource(R.string.settings_title),
                description = stringResource(R.string.profile_link_settings_desc),
                onClick = onOpenSettings,
            )
        }

        StatsSection(stats = stats)

        BadgesSection(earned = badges)

        Spacer(Modifier.height(FilmatubeSpacing.xl))
    }
}

/**
 * Watch stats. Single headline numbers, so these are stat tiles rather than any kind of plot;
 * the top-genre chips carry identity in their labels, not in colour.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatsSection(stats: com.filmatube.app.data.achievements.UserStats) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = FilmatubeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
    ) {
        Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                value = stats.watchHours.toString(),
                label = stringResource(R.string.stat_hours),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = stats.moviesCompleted.toString(),
                label = stringResource(R.string.stat_movies),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = stats.reviewsWritten.toString(),
                label = stringResource(R.string.stat_reviews),
                modifier = Modifier.weight(1f),
            )
        }
        if (stats.topGenres.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm), verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                Text(
                    stringResource(R.string.stat_top_genres),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                stats.topGenres.forEach { key ->
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Text(
                            genreLabel(key),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Localised genre name, falling back to the raw key for anything not in the catalogue. */
@Composable
private fun genreLabel(key: String): String {
    val res = com.filmatube.app.ui.taste.Genre.entries.find { it.key == key }?.labelRes
    return if (res != null) stringResource(res) else key.replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BadgesSection(earned: Set<String>) {
    Column(
        // The parent Column pads each child individually rather than as a group, so this
        // needs its own horizontal inset or it sits flush against the screen edge.
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FilmatubeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.profile_badges), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.badges_earned, earned.size, Badge.entries.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md), verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
            Badge.entries.forEach { badge ->
                val has = badge.id in earned
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(64.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (has) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (has) {
                            Text(badge.emoji, style = MaterialTheme.typography.titleLarge)
                        } else {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Text(
                        stringResource(badge.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (has) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = FilmatubeSpacing.xs),
                    )
                }
            }
        }
    }
}
