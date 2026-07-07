package com.filmatube.app.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.filmatube.app.ui.theme.FilmatubeSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val SWIPE_SEEK_WINDOW_MS = 90_000f // full-width horizontal swipe ≈ 90s

private enum class DragMode { UNDECIDED, SEEK, BRIGHTNESS, VOLUME }

private sealed interface GestureIndicator {
    data class Seek(val label: String, val forward: Boolean) : GestureIndicator
    data class Brightness(val fraction: Float) : GestureIndicator
    data class Volume(val fraction: Float) : GestureIndicator
}

/**
 * Transparent gesture surface over the video:
 *  - single tap → [onTap] (toggles controls)
 *  - double tap → seek ±10s depending on the tapped half
 *  - horizontal drag → scrub/seek
 *  - vertical drag on the left half → screen brightness
 *  - vertical drag on the right half → media volume
 * A transient centre pill shows the current adjustment.
 */
@Composable
fun PlayerGestureBox(
    player: Player,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val window = remember(view) { view.context.playerActivity()?.window }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var indicator by remember { mutableStateOf<GestureIndicator?>(null) }
    val scope = rememberCoroutineScope()
    var hideJob by remember { mutableStateOf<Job?>(null) }

    fun flash(next: GestureIndicator?) {
        indicator = next
        hideJob?.cancel()
        if (next != null) hideJob = scope.launch { delay(700); indicator = null }
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { offset ->
                        val forward = offset.x > size.width / 2
                        val delta = if (forward) 10_000L else -10_000L
                        val duration = player.duration.coerceAtLeast(0L)
                        val max = if (duration > 0) duration else Long.MAX_VALUE
                        player.seekTo((player.currentPosition + delta).coerceIn(0L, max))
                        flash(GestureIndicator.Seek(if (forward) "+10s" else "-10s", forward))
                    },
                )
            }
            .pointerInput(Unit) {
                var mode = DragMode.UNDECIDED
                var totalX = 0f
                var totalY = 0f
                var startPos = 0L
                var startBrightness = 0.5f
                var startVolume = 0
                var leftHalf = false

                detectDragGestures(
                    onDragStart = { offset ->
                        mode = DragMode.UNDECIDED
                        totalX = 0f
                        totalY = 0f
                        startPos = player.currentPosition
                        leftHalf = offset.x < size.width / 2
                        startBrightness = currentBrightness(window)
                        startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    },
                    onDragEnd = {
                        indicator = null
                        hideJob?.cancel()
                    },
                    onDragCancel = {
                        indicator = null
                        hideJob?.cancel()
                    },
                    onDrag = { change, drag ->
                        change.consume()
                        totalX += drag.x
                        totalY += drag.y

                        if (mode == DragMode.UNDECIDED && (abs(totalX) > 24f || abs(totalY) > 24f)) {
                            mode = when {
                                abs(totalX) > abs(totalY) -> DragMode.SEEK
                                leftHalf -> DragMode.BRIGHTNESS
                                else -> DragMode.VOLUME
                            }
                        }

                        when (mode) {
                            DragMode.SEEK -> {
                                val duration = player.duration.coerceAtLeast(1L)
                                val deltaMs = ((totalX / size.width) * SWIPE_SEEK_WINDOW_MS).toLong()
                                player.seekTo((startPos + deltaMs).coerceIn(0L, duration))
                                indicator = GestureIndicator.Seek(signedSeconds(deltaMs), deltaMs >= 0)
                            }
                            DragMode.BRIGHTNESS -> {
                                val f = (startBrightness - totalY / size.height).coerceIn(0.01f, 1f)
                                setBrightness(window, f)
                                indicator = GestureIndicator.Brightness(f)
                            }
                            DragMode.VOLUME -> {
                                val f = (startVolume / maxVolume.toFloat() - totalY / size.height).coerceIn(0f, 1f)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (f * maxVolume).roundToInt(), 0)
                                indicator = GestureIndicator.Volume(f)
                            }
                            DragMode.UNDECIDED -> Unit
                        }
                    },
                )
            },
    ) {
        indicator?.let { GestureIndicatorPill(it, Modifier.align(Alignment.Center)) }
    }
}

@Composable
private fun GestureIndicatorPill(indicator: GestureIndicator, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon: ImageVector = when (indicator) {
                is GestureIndicator.Seek -> if (indicator.forward) Icons.Filled.FastForward else Icons.Filled.FastRewind
                is GestureIndicator.Brightness -> Icons.Filled.BrightnessHigh
                is GestureIndicator.Volume -> Icons.Filled.VolumeUp
            }
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            when (indicator) {
                is GestureIndicator.Seek -> Text(
                    indicator.label,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = FilmatubeSpacing.sm),
                )
                is GestureIndicator.Brightness -> ProgressPill(indicator.fraction)
                is GestureIndicator.Volume -> ProgressPill(indicator.fraction)
            }
        }
    }
}

@Composable
private fun ProgressPill(fraction: Float) {
    LinearProgressIndicator(
        progress = { fraction },
        color = MaterialTheme.colorScheme.primary,
        trackColor = Color.White.copy(alpha = 0.3f),
        modifier = Modifier
            .padding(start = FilmatubeSpacing.sm)
            .width(120.dp),
    )
}

private fun signedSeconds(ms: Long): String {
    val seconds = ms / 1000
    return if (seconds >= 0) "+${seconds}s" else "${seconds}s"
}

private fun currentBrightness(window: Window?): Float {
    val b = window?.attributes?.screenBrightness ?: 0.5f
    return if (b < 0f) 0.5f else b // < 0 means "follow system" — start from a mid baseline
}

private fun setBrightness(window: Window?, value: Float) {
    window ?: return
    val attrs = window.attributes
    attrs.screenBrightness = value
    window.attributes = attrs
}

private fun Context.playerActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
