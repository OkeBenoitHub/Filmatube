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
import androidx.compose.ui.text.style.TextAlign
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
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeGold
import com.filmatube.app.ui.theme.FilmatubeSpacing

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val followerCount by viewModel.followerCount.collectAsStateWithLifecycle()
    val followingCount by viewModel.followingCount.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.nav_profile),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                )
            }
        }

        when (val s = state) {
            DataState.Loading -> LoadingView()
            is DataState.Error -> ErrorView(error = s.error, onRetry = {})
            DataState.Empty -> LoadingView()
            is DataState.Success -> ProfileContent(
                profile = s.data,
                followerCount = followerCount,
                followingCount = followingCount,
                onEditProfile = onEditProfile,
                onOpenFollowers = onOpenFollowers,
                onOpenFollowing = onOpenFollowing,
            )
        }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    followerCount: Int,
    followingCount: Int,
    onEditProfile: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FilmatubeSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
    ) {
        Spacer(Modifier.height(FilmatubeSpacing.md))

        UserAvatar(url = profile.avatarUrl, name = profile.displayName, size = 96.dp)

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
            Text(profile.displayName, style = MaterialTheme.typography.headlineSmall)
            if (profile.isAdmin) {
                Surface(shape = MaterialTheme.shapes.small, color = FilmatubeGold.copy(alpha = 0.18f)) {
                    Text(
                        text = stringResource(R.string.profile_admin),
                        style = MaterialTheme.typography.labelMedium,
                        color = FilmatubeGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            if (profile.bio.isNotBlank()) {
                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        FilmatubeSecondaryButton(
            text = stringResource(R.string.profile_edit),
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth(),
        )

        StatsRow(
            followerCount = followerCount,
            followingCount = followingCount,
            onOpenFollowers = onOpenFollowers,
            onOpenFollowing = onOpenFollowing,
        )

        BadgesSection()

        Spacer(Modifier.height(FilmatubeSpacing.xl))
    }
}

@Composable
private fun StatsRow(
    followerCount: Int,
    followingCount: Int,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Stat(count = followerCount.toLong(), label = stringResource(R.string.profile_followers), onClick = onOpenFollowers)
        Stat(count = followingCount.toLong(), label = stringResource(R.string.profile_following), onClick = onOpenFollowing)
        Stat(count = 0L, label = stringResource(R.string.profile_watched))
    }
}

@Composable
private fun Stat(count: Long, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Text(count.toString(), style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BadgesSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
