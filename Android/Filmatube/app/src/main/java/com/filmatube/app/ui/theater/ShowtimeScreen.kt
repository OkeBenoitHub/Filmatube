package com.filmatube.app.ui.theater

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.data.theater.Showtime
import com.filmatube.app.data.theater.ShowtimeStatus
import com.filmatube.app.data.theater.TheaterAttendee
import com.filmatube.app.data.theater.TheaterMessage
import com.filmatube.app.ui.components.EmptyView
import com.filmatube.app.ui.components.LoadingView
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeTextField
import com.filmatube.app.ui.components.FilmatubeSecondaryButton
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeShapes
import com.filmatube.app.ui.theme.FilmatubeSpacing


/**
 * A single showtime: what it is, when it starts, who's coming — and the way in.
 *
 * The screen changes shape with the showtime's status rather than pushing you between
 * routes: `scheduled` shows the RSVP, `lobby` opens the countdown, faces and pre-show
 * chat, and `live` offers the door into the synced room.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowtimeScreen(
    onBack: () -> Unit,
    onEnterTheater: (movieId: String, showtimeId: String) -> Unit,
    viewModel: ShowtimeViewModel = hiltViewModel(),
) {
    val showtime by viewModel.showtime.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val attendees by viewModel.attendees.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val presentCount by viewModel.presentCount.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val spoiler by viewModel.spoiler.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val rateLimited by viewModel.rateLimitedForMs.collectAsStateWithLifecycle()
    val nowMs by rememberTickingClock()

    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val revealed = remember { mutableStateMapOf<String, Boolean>() }

    val throttledMsg = stringResource(R.string.theater_chat_too_fast)
    LaunchedEffect(rateLimited) {
        if (rateLimited != null) {
            snackbar.showSnackbar(throttledMsg)
            viewModel.clearRateLimited()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theater_showtime_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        // Was a bare early return, which rendered an empty screen both while loading and if
        // the showtime was deleted out from under you — indistinguishable from a hang.
        val s = showtime
        if (s == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                if (loaded) {
                    EmptyView(
                        icon = Icons.Outlined.Theaters,
                        title = stringResource(R.string.theater_gone_title),
                        message = stringResource(R.string.theater_gone_message),
                    )
                } else {
                    LoadingView()
                }
            }
            return@Scaffold
        }
        val inLobby = s.status == ShowtimeStatus.LOBBY

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(FilmatubeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.lg),
            ) {
                item { ShowtimeHeader(s, nowMs) }

                // ── the way in / the way to say you're coming ──
                item {
                    when {
                        s.status == ShowtimeStatus.ENDED -> Text(
                            stringResource(R.string.theater_over),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        s.isOpen -> FilmatubePrimaryButton(
                            text = stringResource(
                                if (s.isLive) R.string.theater_enter else R.string.theater_enter_lobby,
                            ),
                            onClick = { onEnterTheater(s.movieId, s.id) },
                            leadingIcon = Icons.Filled.PlayArrow,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        attendance.waitlisted -> FilmatubeSecondaryButton(
                            text = stringResource(R.string.theater_waitlist_leave),
                            onClick = viewModel::toggleRsvp,
                            enabled = !busy,
                            leadingIcon = Icons.Filled.HourglassEmpty,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        attendance.going -> FilmatubeSecondaryButton(
                            text = stringResource(R.string.theater_rsvp_cancel),
                            onClick = viewModel::toggleRsvp,
                            enabled = !busy,
                            leadingIcon = Icons.Filled.Check,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        // A full room queues you rather than turning you away — cancellations
                        // are common, and this is exactly when you want telling.
                        s.isFull -> FilmatubePrimaryButton(
                            text = stringResource(R.string.theater_waitlist_join),
                            onClick = viewModel::toggleRsvp,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        else -> FilmatubePrimaryButton(
                            text = stringResource(R.string.theater_rsvp),
                            onClick = viewModel::toggleRsvp,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ── remind me ──
                if (attendance.going && !s.isOpen) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = FilmatubeSpacing.md),
                            ) {
                                Text(
                                    stringResource(R.string.theater_remind_title),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    stringResource(R.string.theater_remind_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = attendance.remind, onCheckedChange = { viewModel.toggleRemind() })
                        }
                    }
                }

                // ── who's coming / who's here ──
                item {
                    Column {
                        Text(
                            stringResource(
                                if (s.isLive) R.string.theater_watching else R.string.theater_going,
                                s.attendeesCount,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        // Once the doors open, "here now" is the number that matters.
                        if (s.isOpen) {
                            Text(
                                stringResource(R.string.theater_in_room_now, presentCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (attendees.isNotEmpty()) {
                    item { AttendeeStrip(attendees) }
                }

                // ── pre-show chat ──
                if (inLobby || s.isLive) {
                    item {
                        Text(
                            stringResource(R.string.theater_preshow_chat),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    if (messages.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.theater_chat_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        ChatLine(
                            message = message,
                            revealed = revealed[message.id] == true,
                            onReveal = { revealed[message.id] = true },
                            onReport = { viewModel.report(message) },
                        )
                    }
                }
            }

            // The composer sits outside the scrolling list so it stays put as chat arrives.
            if (inLobby || s.isLive) {
                TheaterComposer(
                    draft = draft,
                    spoiler = spoiler,
                    onDraftChange = viewModel::setDraft,
                    onSpoilerChange = viewModel::setSpoiler,
                    onSend = viewModel::send,
                )
            }
        }
    }

    // Follow the conversation as it grows.
    LaunchedEffect(messages.size) {
        val last = listState.layoutInfo.totalItemsCount - 1
        if (messages.isNotEmpty() && last >= 0) listState.animateScrollToItem(last)
    }
}

@Composable
private fun ShowtimeHeader(showtime: Showtime, nowMs: Long) {
    Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
        AsyncImage(
            model = showtime.posterUrl,
            contentDescription = showtime.movieTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(96.dp)
                .aspectRatio(2f / 3f)
                .clip(FilmatubeShapes.medium),
        )
        Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
            Text(showtime.movieTitle, style = MaterialTheme.typography.titleLarge)
            Text(
                startTimeLabel(showtime),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                countdownLabel(showtime, nowMs),
                style = MaterialTheme.typography.titleMedium,
                color = if (showtime.isLive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (showtime.capacity > 0) {
                Text(
                    stringResource(R.string.theater_capacity, showtime.attendeesCount, showtime.capacity),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AttendeeStrip(attendees: List<TheaterAttendee>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
        items(attendees, key = { it.uid }) { attendee ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(56.dp),
            ) {
                UserAvatar(url = attendee.avatar.ifBlank { null }, name = attendee.name, size = 44.dp)
                Text(
                    attendee.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChatLine(
    message: TheaterMessage,
    revealed: Boolean,
    onReveal: () -> Unit,
    onReport: () -> Unit,
) {
    var reported by remember(message.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
    ) {
        UserAvatar(url = message.userAvatar.ifBlank { null }, name = message.userName, size = 28.dp)
        Column {
            Text(
                message.userName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (message.isMine) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (message.isSpoiler && !revealed) {
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
                Text(message.text, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Only other people's lines: reporting your own is noise for the moderation queue.
        if (!message.isMine) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { reported = true; onReport() }, enabled = !reported) {
                Text(
                    stringResource(if (reported) R.string.reviews_reported else R.string.reviews_report),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TheaterComposer(
    draft: String,
    spoiler: Boolean,
    onDraftChange: (String) -> Unit,
    onSpoilerChange: (Boolean) -> Unit,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(FilmatubeSpacing.md),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
        ) {
            FilmatubeTextField(
                value = draft,
                onValueChange = onDraftChange,
                label = stringResource(R.string.theater_chat_hint),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.party_chat_send),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = spoiler, onCheckedChange = onSpoilerChange)
            Text(
                stringResource(R.string.reviews_spoiler_toggle),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
