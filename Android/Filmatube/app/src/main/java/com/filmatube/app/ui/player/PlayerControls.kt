package com.filmatube.app.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
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

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L)) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
