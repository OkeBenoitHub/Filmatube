package com.filmatube.app.ui.boards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.data.boards.BoardMember
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: MembersViewModel = hiltViewModel(),
) {
    val members by viewModel.members.collectAsStateWithLifecycle()
    val ownerId by viewModel.ownerId.collectAsStateWithLifecycle()
    val amOwner = ownerId.isNotBlank() && ownerId == viewModel.myUid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.board_members_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(members, key = { it.uid }) { member ->
                MemberRow(
                    member = member,
                    canModerate = amOwner && member.uid != viewModel.myUid && member.role != "owner",
                    onClick = { onUserClick(member.uid) },
                    onToggleMute = { viewModel.toggleMute(member) },
                    onRemove = { viewModel.remove(member) },
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: BoardMember,
    canModerate: Boolean,
    onClick: () -> Unit,
    onToggleMute: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(url = member.avatar, name = member.name, size = 40.dp)
        androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
            Text(
                member.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = buildList {
                if (member.role == "owner") add(stringResource(R.string.board_role_owner))
                if (member.muted) add(stringResource(R.string.board_muted_label))
            }.joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (canModerate) {
            TextButton(onClick = onToggleMute) {
                Text(stringResource(if (member.muted) R.string.board_unmute else R.string.board_mute))
            }
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.board_remove), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
