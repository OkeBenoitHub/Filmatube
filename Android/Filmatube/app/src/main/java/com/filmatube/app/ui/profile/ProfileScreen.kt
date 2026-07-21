package com.filmatube.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Settings
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
                onOpenFollowers = onOpenFollowers,
                onOpenFollowing = onOpenFollowing,
                onOpenSuggestions = onOpenSuggestions,
                onOpenInbox = onOpenInbox,
                onOpenNotifications = onOpenNotifications,
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
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onOpenInbox: () -> Unit,
    onOpenNotifications: () -> Unit,
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
                value = "0",
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

        BadgesSection()

        Spacer(Modifier.height(FilmatubeSpacing.xl))
    }
}

@Composable
private fun BadgesSection() {
    Column(
        // The parent Column pads each child individually rather than as a group, so this
        // needs its own horizontal inset or it sits flush against the screen edge.
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FilmatubeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
    ) {
        Text(stringResource(R.string.profile_badges), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.profile_badges_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
