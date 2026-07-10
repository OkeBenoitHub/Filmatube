package com.filmatube.app.ui.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Column
import com.filmatube.app.R
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeSpacing

/** Shared user row: avatar, name, taste-match %, and a Follow/Following toggle. */
@Composable
fun FollowUserRow(
    name: String,
    avatarUrl: String,
    tasteMatch: Int,
    isFollowing: Boolean,
    onToggle: () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(url = avatarUrl, name = name, size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (tasteMatch > 0) {
                Text(
                    stringResource(R.string.taste_match, tasteMatch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (isFollowing) {
            OutlinedButton(onClick = onToggle) { Text(stringResource(R.string.following_action)) }
        } else {
            Button(onClick = onToggle) { Text(stringResource(R.string.follow_action)) }
        }
    }
}

@Composable
fun FollowListScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: FollowListViewModel = hiltViewModel(),
) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    val title = stringResource(
        if (viewModel.mode == FOLLOW_MODE_FOLLOWERS) R.string.profile_followers else R.string.profile_following,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.sm, vertical = FilmatubeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.settings_back))
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }

        if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.follow_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(FilmatubeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
            ) {
                items(users, key = { it.uid }) { user ->
                    FollowUserRow(
                        name = user.displayName,
                        avatarUrl = user.avatarUrl,
                        tasteMatch = user.tasteMatch,
                        isFollowing = user.isFollowing,
                        onToggle = { viewModel.toggleFollow(user.uid, !user.isFollowing) },
                        onClick = { onUserClick(user.uid) },
                    )
                }
            }
        }
    }
}
