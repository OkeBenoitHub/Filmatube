package com.filmatube.app.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.data.boards.BOARD_REACTIONS
import com.filmatube.app.data.boards.BoardMessage
import com.filmatube.app.data.boards.BoardTypes
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeSecondaryButton
import com.filmatube.app.ui.components.FilmatubeTextField
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDetailScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onOpenMembers: () -> Unit,
    viewModel: BoardDetailViewModel = hiltViewModel(),
) {
    val board by viewModel.board.collectAsStateWithLifecycle()
    val isMember by viewModel.isMember.collectAsStateWithLifecycle()
    val amMuted by viewModel.amMuted.collectAsStateWithLifecycle()
    val invitedCount by viewModel.invitedCount.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val typingNames by viewModel.typingNames.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val spoiler by viewModel.spoiler.collectAsStateWithLifecycle()
    val replyingTo by viewModel.replyingTo.collectAsStateWithLifecycle()
    val isOwner = viewModel.isOwner
    val pinnedId = board?.pinnedMessageId.orEmpty()
    val pinnedMessage = messages.firstOrNull { it.id == pinnedId }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val revealed = remember { mutableStateMapOf<String, Boolean>() }

    // Keep the newest message in view as they arrive / as you send.
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val invitedManyMsg = invitedCount?.let { stringResource(R.string.board_invited, it) }
    val invitedNoneMsg = stringResource(R.string.board_invited_none)
    LaunchedEffect(invitedCount) {
        val count = invitedCount ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(if (count > 0) invitedManyMsg!! else invitedNoneMsg)
        viewModel.clearInvited()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(board?.title ?: stringResource(R.string.boards_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMembers) {
                        Icon(Icons.Filled.Groups, contentDescription = stringResource(R.string.board_members_title))
                    }
                },
            )
        },
    ) { padding ->
        val b = board
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                if (b?.coverUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = b.coverUrl,
                        contentDescription = b.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                            ),
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Column(
                modifier = Modifier.padding(FilmatubeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                    Text(b?.title ?: "", style = MaterialTheme.typography.headlineSmall)
                    if (b?.isOfficial == true) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = stringResource(R.string.board_official),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (b != null) {
                    val typeLabel = stringResource(
                        if (b.type == BoardTypes.MOVIE) R.string.board_type_movie else R.string.board_type_general,
                    )
                    Text(
                        "$typeLabel · ${stringResource(R.string.boards_members, b.memberCount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (b?.description?.isNotBlank() == true) {
                    Text(
                        b.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (b != null) {
                    Row(
                        modifier = Modifier.padding(top = FilmatubeSpacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                    ) {
                        if (viewModel.isOwner) {
                            FilmatubePrimaryButton(
                                text = stringResource(R.string.board_invite),
                                onClick = viewModel::invite,
                                leadingIcon = Icons.Filled.PersonAdd,
                                modifier = Modifier.weight(1f),
                            )
                        } else if (isMember) {
                            FilmatubeSecondaryButton(
                                text = stringResource(R.string.board_leave),
                                onClick = viewModel::toggleMembership,
                                modifier = Modifier.weight(1f),
                            )
                            FilmatubePrimaryButton(
                                text = stringResource(R.string.board_invite),
                                onClick = viewModel::invite,
                                leadingIcon = Icons.Filled.PersonAdd,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            FilmatubePrimaryButton(
                                text = stringResource(R.string.board_join),
                                onClick = viewModel::toggleMembership,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Pinned message banner
            pinnedMessage?.let { pin ->
                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isOwner) { viewModel.pinMessage(pin) }
                            .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                    ) {
                        Icon(Icons.Filled.PushPin, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.board_pinned), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(
                                pin.text.ifBlank { pin.movieTitle },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Messages
            if (messages.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.board_no_messages),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(FilmatubeSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            myUid = viewModel.myUid,
                            isOwner = isOwner,
                            isPinned = msg.id == pinnedId,
                            revealed = revealed[msg.id] == true,
                            onReveal = { revealed[msg.id] = true },
                            onDelete = { viewModel.deleteMessage(msg) },
                            onReply = { viewModel.setReplyTo(msg) },
                            onReact = { emoji -> viewModel.toggleReaction(msg, emoji) },
                            onPin = { viewModel.pinMessage(msg) },
                            onReport = { viewModel.reportMessage(msg) },
                            onMovieClick = onMovieClick,
                        )
                    }
                }
            }

            // Typing indicator
            if (typingNames.isNotEmpty()) {
                Text(
                    text = if (typingNames.size == 1) {
                        stringResource(R.string.board_typing_one, typingNames.first())
                    } else {
                        stringResource(R.string.board_typing_many)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.xs),
                )
            }

            // Composer / mute / join gate
            if (amMuted) {
                Text(
                    stringResource(R.string.board_you_muted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(FilmatubeSpacing.md),
                )
            } else if (isMember || isOwner) {
                ChatComposer(
                    draft = draft,
                    spoiler = spoiler,
                    replyingToName = replyingTo?.userName,
                    onCancelReply = { viewModel.setReplyTo(null) },
                    onDraftChange = viewModel::setDraft,
                    onSpoilerChange = viewModel::setSpoiler,
                    onSend = viewModel::send,
                )
            } else {
                FilmatubePrimaryButton(
                    text = stringResource(R.string.board_join_to_chat),
                    onClick = viewModel::toggleMembership,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FilmatubeSpacing.md),
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: BoardMessage,
    myUid: String?,
    isOwner: Boolean,
    isPinned: Boolean,
    revealed: Boolean,
    onReveal: () -> Unit,
    onDelete: () -> Unit,
    onReply: () -> Unit,
    onReact: (String) -> Unit,
    onPin: () -> Unit,
    onReport: () -> Unit,
    onMovieClick: (String) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    var reported by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
        UserAvatar(url = message.userAvatar, name = message.userName, size = 32.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                Text(
                    message.userName,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (message.isMine) {
                    Text(
                        stringResource(R.string.reviews_delete),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onDelete),
                    )
                }
            }

            // Reply quote
            if (message.replyToName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.padding(vertical = 2.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = FilmatubeSpacing.sm, vertical = 4.dp)) {
                        Text(message.replyToName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(
                            message.replyToText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Movie card
            if (message.movieId.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.padding(top = 2.dp).clickable { onMovieClick(message.movieId) },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                        modifier = Modifier.padding(FilmatubeSpacing.sm),
                    ) {
                        AsyncImage(
                            model = message.moviePoster,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(width = 44.dp, height = 66.dp).clip(RoundedCornerShape(6.dp)),
                        )
                        Text(message.movieTitle, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            // Text (with spoiler gate)
            if (message.text.isNotBlank()) {
                if (message.hasSpoiler && !revealed) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.clickable(onClick = onReveal),
                    ) {
                        Text(
                            stringResource(R.string.reviews_spoiler_hidden),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(FilmatubeSpacing.sm),
                        )
                    }
                } else {
                    if (message.hasSpoiler) {
                        Text(
                            stringResource(R.string.reviews_spoiler_chip),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(message.text, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Reaction chips
            val grouped = message.reactions.values.groupingBy { it }.eachCount()
            if (grouped.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
                ) {
                    grouped.forEach { (emoji, count) ->
                        val mine = myUid != null && message.reactions[myUid] == emoji
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable { onReact(emoji) },
                        ) {
                            Text(
                                "$emoji $count",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.board_reply),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onReply).padding(vertical = 4.dp, horizontal = 2.dp),
                )
                Text(
                    stringResource(R.string.board_react),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { pickerOpen = !pickerOpen }
                        .padding(vertical = 4.dp, horizontal = FilmatubeSpacing.sm),
                )
                if (isOwner) {
                    Text(
                        stringResource(if (isPinned) R.string.board_unpin else R.string.board_pin),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(onClick = onPin).padding(vertical = 4.dp, horizontal = 2.dp),
                    )
                }
                if (!message.isMine) {
                    Text(
                        stringResource(if (reported) R.string.board_reported_msg else R.string.board_report_msg),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable(enabled = !reported) { onReport(); reported = true }
                            .padding(vertical = 4.dp, horizontal = FilmatubeSpacing.sm),
                    )
                }
            }
            if (pickerOpen) {
                Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                    BOARD_REACTIONS.forEach { emoji ->
                        Text(
                            emoji,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .clickable { onReact(emoji); pickerOpen = false }
                                .padding(4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatComposer(
    draft: String,
    spoiler: Boolean,
    replyingToName: String?,
    onCancelReply: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSpoilerChange: (Boolean) -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(FilmatubeSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
        ) {
            if (replyingToName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.board_replying_to, replyingToName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.comments_cancel_reply),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable(onClick = onCancelReply).padding(4.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = spoiler, onCheckedChange = onSpoilerChange)
                Text(
                    stringResource(R.string.reviews_spoiler_toggle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = FilmatubeSpacing.xs),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                FilmatubeTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    label = stringResource(R.string.board_message_hint),
                    singleLine = false,
                    minLines = 1,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.board_send))
                }
            }
        }
    }
}
