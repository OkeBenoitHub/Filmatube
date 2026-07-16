package com.filmatube.app.ui.parties

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.data.parties.PartyStatus
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeSecondaryButton
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeShapes
import com.filmatube.app.ui.theme.FilmatubeSpacing
import java.text.DateFormat
import java.util.Date

/**
 * Watch-party lobby: the movie, when it starts, who's coming. The host invites
 * (followers or a board's members), starts and ends the party; guests join or leave.
 * When live, everyone gets the "Watch together" button into the synced player.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyScreen(
    onBack: () -> Unit,
    onWatch: (movieId: String, partyId: String) -> Unit,
    viewModel: PartyViewModel = hiltViewModel(),
) {
    val party by viewModel.party.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val isMember by viewModel.isMember.collectAsStateWithLifecycle()
    val isHost by viewModel.isHost.collectAsStateWithLifecycle()
    val myBoards by viewModel.myBoards.collectAsStateWithLifecycle()
    val inviteResult by viewModel.inviteResult.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    var showBoardPicker by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val invitedText = stringResource(R.string.party_invited_count)

    LaunchedEffect(inviteResult) {
        inviteResult?.let {
            snackbar.showSnackbar("$invitedText ${it.count}")
            viewModel.dismissInviteResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.party_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
            )
        },
    ) { padding ->
        val p = party ?: return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(FilmatubeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.lg),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
                    AsyncImage(
                        model = p.moviePoster,
                        contentDescription = p.movieTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(84.dp)
                            .aspectRatio(2f / 3f)
                            .clip(FilmatubeShapes.medium),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                        Text(p.movieTitle, style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.party_hosted_by, p.hostName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = when (p.status) {
                                PartyStatus.LIVE -> stringResource(R.string.party_status_live)
                                PartyStatus.ENDED -> stringResource(R.string.party_status_ended)
                                else -> DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date(p.scheduledAtMs))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (p.isLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── primary action ─────────────────────────────────
            item {
                when {
                    p.isEnded -> Text(
                        stringResource(R.string.party_over),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    p.isLive && isMember -> FilmatubePrimaryButton(
                        text = stringResource(R.string.party_watch_together),
                        onClick = { onWatch(p.movieId, p.id) },
                        leadingIcon = Icons.Filled.PlayArrow,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    isHost -> FilmatubePrimaryButton(
                        text = stringResource(R.string.party_start),
                        onClick = viewModel::start,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    !isMember -> FilmatubePrimaryButton(
                        text = stringResource(R.string.party_join),
                        onClick = viewModel::join,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    else -> Text(
                        stringResource(R.string.party_waiting_host),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── host tools ─────────────────────────────────────
            if (isHost && !p.isEnded) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                        FilmatubeSecondaryButton(
                            text = stringResource(R.string.party_invite_followers),
                            onClick = viewModel::inviteFollowers,
                            enabled = !busy,
                            leadingIcon = Icons.Filled.PersonAdd,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        FilmatubeSecondaryButton(
                            text = stringResource(R.string.party_invite_board),
                            onClick = { showBoardPicker = true },
                            enabled = !busy,
                            leadingIcon = Icons.Filled.Groups,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (p.isLive) {
                            FilmatubeSecondaryButton(
                                text = stringResource(R.string.party_end),
                                onClick = viewModel::end,
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            } else if (isMember && !isHost && !p.isEnded) {
                item {
                    FilmatubeSecondaryButton(
                        text = stringResource(R.string.party_leave),
                        onClick = viewModel::leave,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ── guest list ─────────────────────────────────────
            item {
                Text(
                    stringResource(R.string.party_members, p.memberCount),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(members, key = { it.uid }) { member ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    UserAvatar(url = member.avatar, name = member.name, size = 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(member.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (member.role == "host") {
                                stringResource(R.string.party_role_host)
                            } else {
                                stringResource(R.string.party_role_guest)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // Handoff: lets the host leave without killing the room.
                    if (isHost && member.role != "host" && !p.isEnded) {
                        TextButton(onClick = { viewModel.makeHost(member.uid) }, enabled = !busy) {
                            Text(stringResource(R.string.party_make_host))
                        }
                    }
                }
            }
        }
    }

    if (showBoardPicker) {
        AlertDialog(
            onDismissRequest = { showBoardPicker = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBoardPicker = false }) {
                    Text(stringResource(R.string.comments_cancel_reply))
                }
            },
            title = { Text(stringResource(R.string.party_invite_board)) },
            text = {
                if (myBoards.isEmpty()) {
                    Text(stringResource(R.string.boards_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(myBoards, key = { it.id }) { board ->
                            Text(
                                board.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.inviteBoard(board)
                                        showBoardPicker = false
                                    }
                                    .padding(vertical = FilmatubeSpacing.md),
                            )
                        }
                    }
                }
            },
        )
    }
}
