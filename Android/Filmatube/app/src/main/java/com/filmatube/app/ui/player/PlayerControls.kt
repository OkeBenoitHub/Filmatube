package com.filmatube.app.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import com.filmatube.app.R
import com.filmatube.app.ui.theme.FilmatubeSpacing
import kotlinx.coroutines.delay

private const val SEEK_STEP_MS = 10_000L

/** Observable snapshot of playback progress, driven by a [Player.Listener] + polling. */
class PlayerControlState {
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var isEnded by mutableStateOf(false)
    var position by mutableLongStateOf(0L)
    var duration by mutableLongStateOf(0L)
    var isScrubbing by mutableStateOf(false)
    var scrubPosition by mutableLongStateOf(0L)
}

@Composable
fun rememberPlayerControlState(player: Player): PlayerControlState {
    val state = remember(player) { PlayerControlState() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state.isPlaying = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                state.isEnded = playbackState == Player.STATE_ENDED
                state.isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    state.duration = player.duration.coerceAtLeast(0L)
                }
            }
        }
        player.addListener(listener)
        state.isPlaying = player.isPlaying
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!state.isScrubbing) {
                state.position = player.currentPosition.coerceAtLeast(0L)
                state.duration = player.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    return state
}

/**
 * Custom control overlay (used with `useController = false`): centre transport
 * (rewind 10s / play-pause / forward 10s), a scrubbable seek bar with a time
 * preview bubble, and an immersive-fullscreen toggle.
 */
@Composable
fun PlayerControls(
    player: Player,
    state: PlayerControlState,
    immersive: Boolean,
    onToggleImmersive: () -> Unit,
    onBack: () -> Unit,
    onLock: () -> Unit,
    onCycleResize: () -> Unit,
    onEnterPip: () -> Unit,
    subtitleLanguages: List<String>,
    selectedSubtitle: String?,
    onSelectSubtitle: (String?) -> Unit,
    onOpenSubtitleStyle: () -> Unit,
    audioTracks: List<AudioTrackOption>,
    onSelectAudio: (String) -> Unit,
    playbackSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    sleepOption: SleepTimerOption?,
    sleepRemainingMs: Long?,
    onSetSleepTimer: (SleepTimerOption?) -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = if (state.isScrubbing) state.scrubPosition else state.position
    val fraction = if (state.duration > 0) shown.toFloat() / state.duration else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
    ) {
        // Top bar — back.
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(FilmatubeSpacing.sm)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.detail_back),
                tint = Color.White,
            )
        }

        // Top bar — lock.
        IconButton(
            onClick = onLock,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(FilmatubeSpacing.sm)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = stringResource(R.string.player_lock),
                tint = Color.White,
            )
        }

        // Centre transport.
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlIcon(Icons.Filled.Replay10, R.string.player_rewind, size = 44.dp) {
                onInteract()
                player.seekTo((player.currentPosition - SEEK_STEP_MS).coerceAtLeast(0L))
            }
            ControlIcon(
                imageVector = when {
                    state.isEnded -> Icons.Filled.Replay
                    state.isPlaying -> Icons.Filled.Pause
                    else -> Icons.Filled.PlayArrow
                },
                contentDescription = if (state.isPlaying) R.string.player_pause else R.string.player_play,
                size = 64.dp,
            ) {
                onInteract()
                when {
                    state.isEnded -> {
                        player.seekTo(0L)
                        player.play()
                    }
                    state.isPlaying -> player.pause()
                    else -> player.play()
                }
            }
            ControlIcon(Icons.Filled.Forward10, R.string.player_forward, size = 44.dp) {
                onInteract()
                val max = if (state.duration > 0) state.duration else Long.MAX_VALUE
                player.seekTo((player.currentPosition + SEEK_STEP_MS).coerceAtMost(max))
            }
        }

        // Bottom scrubber.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = FilmatubeSpacing.md, vertical = FilmatubeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
        ) {
            Text(formatTime(shown), color = Color.White, style = MaterialTheme.typography.labelMedium)
            Slider(
                value = fraction.coerceIn(0f, 1f),
                onValueChange = { value ->
                    onInteract()
                    state.isScrubbing = true
                    state.scrubPosition = (value * state.duration).toLong()
                },
                onValueChangeFinished = {
                    player.seekTo(state.scrubPosition)
                    state.position = state.scrubPosition
                    state.isScrubbing = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                ),
                modifier = Modifier.weight(1f),
            )
            Text(formatTime(state.duration), color = Color.White, style = MaterialTheme.typography.labelMedium)
            if (audioTracks.size > 1) {
                AudioMenu(
                    tracks = audioTracks,
                    onSelect = { onInteract(); onSelectAudio(it) },
                )
            }
            SpeedMenu(speed = playbackSpeed, onSelect = { onInteract(); onSelectSpeed(it) })
            SleepMenu(
                option = sleepOption,
                remainingMs = sleepRemainingMs,
                onSelect = { onInteract(); onSetSleepTimer(it) },
            )
            if (subtitleLanguages.isNotEmpty()) {
                SubtitleMenu(
                    languages = subtitleLanguages,
                    selected = selectedSubtitle,
                    onSelect = { onInteract(); onSelectSubtitle(it) },
                    onOpenStyle = { onInteract(); onOpenSubtitleStyle() },
                )
            }
            IconButton(onClick = { onInteract(); onCycleResize() }) {
                Icon(
                    Icons.Filled.AspectRatio,
                    contentDescription = stringResource(R.string.player_resize),
                    tint = Color.White,
                )
            }
            IconButton(onClick = { onInteract(); onEnterPip() }) {
                Icon(
                    Icons.Filled.PictureInPictureAlt,
                    contentDescription = stringResource(R.string.player_pip),
                    tint = Color.White,
                )
            }
            IconButton(onClick = { onInteract(); onToggleImmersive() }) {
                Icon(
                    if (immersive) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = stringResource(R.string.player_fullscreen),
                    tint = Color.White,
                )
            }
        }
    }
}

private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private fun formatSpeed(speed: Float): String {
    val n = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
    return "${n}x"
}

@Composable
private fun AudioMenu(tracks: List<AudioTrackOption>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Filled.Audiotrack,
                contentDescription = stringResource(R.string.player_audio),
                tint = Color.White,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tracks.forEach { track ->
                DropdownMenuItem(
                    text = { Text(track.label) },
                    onClick = { expanded = false; onSelect(track.language) },
                )
            }
        }
    }
}

@Composable
private fun SpeedMenu(speed: Float, onSelect: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = formatSpeed(speed),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(horizontal = FilmatubeSpacing.sm, vertical = FilmatubeSpacing.xs),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PLAYBACK_SPEEDS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(formatSpeed(option)) },
                    onClick = { expanded = false; onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun SleepMenu(
    option: SleepTimerOption?,
    remainingMs: Long?,
    onSelect: (SleepTimerOption?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(horizontal = FilmatubeSpacing.sm, vertical = FilmatubeSpacing.xs),
        ) {
            Icon(
                Icons.Filled.Bedtime,
                contentDescription = stringResource(R.string.player_sleep_timer),
                tint = if (option != null) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.size(20.dp),
            )
            if (remainingMs != null) {
                Text(
                    text = formatTime(remainingMs),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.player_subtitles_off)) },
                onClick = { expanded = false; onSelect(null) },
            )
            SleepTimerOption.entries.forEach { opt ->
                val label = if (opt == SleepTimerOption.END_OF_MOVIE) {
                    stringResource(R.string.sleep_end_of_movie)
                } else {
                    stringResource(R.string.sleep_minutes, opt.minutes.toInt())
                }
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { expanded = false; onSelect(opt) },
                )
            }
        }
    }
}

@Composable
private fun SubtitleMenu(
    languages: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onOpenStyle: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                if (selected == null) Icons.Filled.ClosedCaptionOff else Icons.Filled.ClosedCaption,
                contentDescription = stringResource(R.string.player_subtitles),
                tint = Color.White,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.player_subtitles_off)) },
                onClick = { expanded = false; onSelect(null) },
            )
            languages.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang.uppercase()) },
                    onClick = { expanded = false; onSelect(lang) },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.player_subtitle_style)) },
                onClick = { expanded = false; onOpenStyle() },
            )
        }
    }
}

@Composable
private fun ControlIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: Int,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(size + 16.dp)) {
        Icon(
            imageVector,
            contentDescription = stringResource(contentDescription),
            tint = Color.White,
            modifier = Modifier.size(size),
        )
    }
}

/** Immersive sticky fullscreen: hides the system bars while [enabled]; restores on exit. */
@Composable
fun ImmersiveFullscreen(enabled: Boolean) {
    val view = LocalView.current
    val window = remember(view) { view.context.findActivity()?.window }

    LaunchedEffect(enabled, window) {
        val w = window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(w, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (enabled) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }

    DisposableEffect(window) {
        onDispose {
            val w = window ?: return@onDispose
            WindowCompat.getInsetsController(w, view).show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

internal fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
