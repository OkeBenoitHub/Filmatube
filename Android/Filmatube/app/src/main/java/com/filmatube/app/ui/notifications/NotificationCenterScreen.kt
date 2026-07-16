package com.filmatube.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.filmatube.app.data.notifications.AppNotification
import com.filmatube.app.data.notifications.NotificationTypes
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeSpacing
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBack: () -> Unit,
    onOpenMovie: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenBoard: (String) -> Unit,
    onOpenParty: (String) -> Unit,
    viewModel: NotificationCenterViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val dayAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
    val today = notifications.filter { it.createdAtMs >= dayAgo }
    val earlier = notifications.filter { it.createdAtMs < dayAgo }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
                actions = {
                    if (notifications.any { !it.read }) {
                        TextButton(onClick = viewModel::markAllRead) {
                            Text(stringResource(R.string.notifications_mark_all_read))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.notifications_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = FilmatubeSpacing.sm),
        ) {
            if (today.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.notifications_today)) }
                items(today, key = { it.id }) { n ->
                    NotificationRow(n, onOpen = { openTarget(n, viewModel, onOpenMovie, onOpenUser, onOpenBoard, onOpenParty) })
                }
            }
            if (earlier.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.notifications_earlier)) }
                items(earlier, key = { it.id }) { n ->
                    NotificationRow(n, onOpen = { openTarget(n, viewModel, onOpenMovie, onOpenUser, onOpenBoard, onOpenParty) })
                }
            }
        }
    }
}

private fun openTarget(
    n: AppNotification,
    viewModel: NotificationCenterViewModel,
    onOpenMovie: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenBoard: (String) -> Unit,
    onOpenParty: (String) -> Unit,
) {
    viewModel.markRead(n.id)
    when {
        n.partyId.isNotBlank() -> onOpenParty(n.partyId)
        n.boardId.isNotBlank() -> onOpenBoard(n.boardId)
        n.movieId.isNotBlank() -> onOpenMovie(n.movieId)
        n.actorId.isNotBlank() -> onOpenUser(n.actorId)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.sm),
    )
}

@Composable
private fun NotificationRow(n: AppNotification, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (n.read) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onOpen)
            .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(url = n.actorAvatar, name = n.actorName, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${n.actorName} ${actionText(n.type)}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = n.boardTitle.ifBlank { n.movieTitle }
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!n.read) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun actionText(type: String): String = stringResource(
    when (type) {
        NotificationTypes.FOLLOW -> R.string.notif_follow
        NotificationTypes.RECOMMENDATION -> R.string.notif_recommendation
        NotificationTypes.REPLY -> R.string.notif_reply
        NotificationTypes.REVIEW_LIKE -> R.string.notif_review_like
        NotificationTypes.BOARD_INVITE -> R.string.notif_board_invite
        NotificationTypes.PARTY_INVITE -> R.string.notif_party_invite
        else -> R.string.notif_recommendation
    },
)
