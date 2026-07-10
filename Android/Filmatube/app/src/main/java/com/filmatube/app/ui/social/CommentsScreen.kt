package com.filmatube.app.ui.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.data.social.Comment
import com.filmatube.app.ui.components.FilmatubeTextField
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: CommentsViewModel = hiltViewModel(),
) {
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val spoilerFree by viewModel.spoilerFree.collectAsStateWithLifecycle()
    val revealed = remember { mutableStateMapOf<String, Boolean>() }
    val reported = remember { mutableStateMapOf<String, Boolean>() }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }

    val topLevel = comments.filter { it.parentId == null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.comments_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(FilmatubeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
            ) {
                if (topLevel.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = FilmatubeSpacing.xl), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.comments_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                topLevel.forEach { parent ->
                    item(key = parent.id) {
                        CommentItem(
                            comment = parent,
                            spoilerFree = spoilerFree,
                            revealed = revealed[parent.id] == true,
                            reported = reported[parent.id] == true,
                            onReveal = { revealed[parent.id] = true },
                            onToggleLike = { viewModel.toggleLike(parent) },
                            onReport = { viewModel.report(parent); reported[parent.id] = true },
                            onReply = { replyingTo = parent },
                            onDelete = { viewModel.delete(parent) },
                            onUserClick = { if (parent.userId.isNotBlank()) onUserClick(parent.userId) },
                        )
                    }
                    val replies = comments.filter { it.parentId == parent.id }
                    replies.forEach { reply ->
                        item(key = reply.id) {
                            CommentItem(
                                comment = reply,
                                spoilerFree = spoilerFree,
                                revealed = revealed[reply.id] == true,
                                reported = reported[reply.id] == true,
                                onReveal = { revealed[reply.id] = true },
                                onToggleLike = { viewModel.toggleLike(reply) },
                                onReport = { viewModel.report(reply); reported[reply.id] = true },
                                onReply = null,
                                onDelete = { viewModel.delete(reply) },
                                onUserClick = { if (reply.userId.isNotBlank()) onUserClick(reply.userId) },
                                modifier = Modifier.padding(start = FilmatubeSpacing.xl),
                            )
                        }
                    }
                }
            }

            CommentComposer(
                replyingToName = replyingTo?.userName,
                onCancelReply = { replyingTo = null },
                onSend = { text, spoiler ->
                    viewModel.post(text, spoiler, replyingTo?.id)
                    replyingTo = null
                },
            )
        }
    }
}

@Composable
private fun CommentComposer(
    replyingToName: String?,
    onCancelReply: () -> Unit,
    onSend: (String, Boolean) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var spoiler by remember { mutableStateOf(false) }

    Surface(tonalElevation = 2.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(FilmatubeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
        ) {
            if (replyingToName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.comments_replying_to, replyingToName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onCancelReply) { Text(stringResource(R.string.comments_cancel_reply)) }
                }
            }
            FilmatubeTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.comments_hint),
                singleLine = false,
                minLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = spoiler, onCheckedChange = { spoiler = it })
                Text(
                    stringResource(R.string.reviews_spoiler_toggle),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = FilmatubeSpacing.sm).weight(1f),
                )
                IconButton(
                    onClick = {
                        onSend(text, spoiler)
                        text = ""
                        spoiler = false
                    },
                    enabled = text.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.comments_send))
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    spoilerFree: Boolean,
    revealed: Boolean,
    reported: Boolean,
    onReveal: () -> Unit,
    onToggleLike: () -> Unit,
    onReport: () -> Unit,
    onReply: (() -> Unit)?,
    onDelete: () -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
            UserAvatar(
                url = comment.userAvatar,
                name = comment.userName,
                size = 30.dp,
                modifier = Modifier.clickable { onUserClick() },
            )
            Text(
                comment.userName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (comment.isMine) {
                Text(
                    stringResource(R.string.reviews_you),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (comment.hasSpoiler && spoilerFree && !revealed) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth().clickable { onReveal() },
            ) {
                Text(
                    stringResource(R.string.reviews_spoiler_hidden),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(FilmatubeSpacing.sm),
                )
            }
        } else {
            if (comment.hasSpoiler) {
                Text(
                    stringResource(R.string.reviews_spoiler_chip),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToggleLike) {
                Icon(
                    Icons.Filled.ThumbUp,
                    contentDescription = stringResource(R.string.reviews_like),
                    tint = if (comment.likedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 2.dp),
                )
                Text(
                    text = if (comment.likeCount > 0) "${comment.likeCount}" else "",
                    color = if (comment.likedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onReply != null) {
                TextButton(onClick = onReply) { Text(stringResource(R.string.comments_reply)) }
            }
            if (comment.isMine) {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.reviews_delete)) }
            } else {
                TextButton(onClick = onReport, enabled = !reported) {
                    Text(stringResource(if (reported) R.string.reviews_reported else R.string.reviews_report))
                }
            }
        }
    }
}
