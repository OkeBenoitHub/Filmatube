package com.filmatube.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.filmatube.app.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.filmatube.app.ui.components.ErrorView
import com.filmatube.app.ui.theme.FilmatubeSpacing

/**
 * Full-screen movie player. Progressive MP4 from a token-protected R2 URL via
 * [PlayerViewModel], rendered under a custom Compose control overlay
 * ([PlayerControls]) with immersive fullscreen. Gestures arrive on Day 45.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onPlayNext: (String) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val resumePrompt by viewModel.resumePrompt.collectAsStateWithLifecycle()
    val subtitleLanguages by viewModel.subtitleLanguages.collectAsStateWithLifecycle()
    val selectedSubtitle by viewModel.selectedSubtitle.collectAsStateWithLifecycle()
    val subtitleStyle by viewModel.subtitleStyle.collectAsStateWithLifecycle()
    val audioTracks by viewModel.audioTracks.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val sleepOption by viewModel.sleepOption.collectAsStateWithLifecycle()
    val sleepRemainingMs by viewModel.sleepRemainingMs.collectAsStateWithLifecycle()
    val upNext by viewModel.upNext.collectAsStateWithLifecycle()
    val introStartMs by viewModel.introStartMs.collectAsStateWithLifecycle()
    val introEndMs by viewModel.introEndMs.collectAsStateWithLifecycle()
    val player = viewModel.player
    var upNextDismissed by remember { mutableStateOf(false) }
    val controlState = rememberPlayerControlState(player)
    val activity = LocalContext.current.findComponentActivity()
    val isInPip = rememberIsInPipMode()

    var controlsVisible by remember { mutableStateOf(true) }
    var immersive by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var showSubtitleStyle by remember { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }

    ImmersiveFullscreen(immersive && !isInPip)

    // Let the system auto-enter PiP when the user leaves while a video is playing.
    LaunchedEffect(controlState.isPlaying) {
        activity?.setPipAutoEnter(controlState.isPlaying)
    }

    // Pause playback when the screen leaves the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-hide the controls (or the lock button) after a few seconds of no interaction.
    LaunchedEffect(interaction, controlsVisible, locked, controlState.isPlaying, controlState.isScrubbing) {
        val idlePlayback = controlState.isPlaying && !controlState.isScrubbing
        if (controlsVisible && (locked || idlePlayback)) {
            kotlinx.coroutines.delay(3500)
            controlsVisible = false
        }
    }

    // Reveal controls + autoplay the "Up Next" movie when the current one ends.
    LaunchedEffect(controlState.isEnded) {
        if (!controlState.isEnded) return@LaunchedEffect
        controlsVisible = true
        val next = upNext
        if (next != null && !upNextDismissed) {
            viewModel.logFeature("up_next")
            onPlayNext(next.id)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    keepScreenOn = true // KEEP_SCREEN_ON while watching
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = {
                it.resizeMode = resizeMode
                applySubtitleStyle(it, subtitleStyle)
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (isInPip) {
            // In Picture-in-Picture: video only, no controls or gestures.
        } else if (locked) {
            // Locked: swallow all gestures, only a tap-revealed unlock button.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        controlsVisible = !controlsVisible
                        interaction++
                    },
            )
            if (controlsVisible) {
                IconButton(
                    onClick = { locked = false; controlsVisible = true; interaction++ },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(FilmatubeSpacing.md)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                ) {
                    Icon(
                        Icons.Filled.LockOpen,
                        contentDescription = stringResource(R.string.player_unlock),
                        tint = Color.White,
                    )
                }
            }
        } else {
            // Unlocked: full gesture surface + custom controls.
            PlayerGestureBox(
                player = player,
                onTap = { controlsVisible = !controlsVisible; interaction++ },
                modifier = Modifier.fillMaxSize(),
            )

            if (controlsVisible && uiState == PlayerUiState.Ready) {
                PlayerControls(
                    player = player,
                    state = controlState,
                    immersive = immersive,
                    onToggleImmersive = { immersive = !immersive },
                    onBack = onBack,
                    onLock = { locked = true; controlsVisible = false },
                    onCycleResize = {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    onEnterPip = { activity?.enterPip() },
                    subtitleLanguages = subtitleLanguages,
                    selectedSubtitle = selectedSubtitle,
                    onSelectSubtitle = viewModel::selectSubtitle,
                    onOpenSubtitleStyle = { showSubtitleStyle = true },
                    audioTracks = audioTracks,
                    onSelectAudio = viewModel::selectAudio,
                    playbackSpeed = playbackSpeed,
                    onSelectSpeed = viewModel::setPlaybackSpeed,
                    sleepOption = sleepOption,
                    sleepRemainingMs = sleepRemainingMs,
                    onSetSleepTimer = viewModel::setSleepTimer,
                    onInteract = { interaction++ },
                )
            }
        }

        if (!isInPip) {
            when (val state = uiState) {
                PlayerUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
                is PlayerUiState.Error -> ErrorView(error = state.error, onRetry = viewModel::load)
                PlayerUiState.Ready -> Unit
            }

            // Mid-playback buffering (distinct from initial load).
            if (uiState == PlayerUiState.Ready && controlState.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }

            resumePrompt?.let { positionMs ->
                LaunchedEffect(positionMs) {
                    kotlinx.coroutines.delay(6000)
                    viewModel.dismissResumePrompt()
                }
                ResumeBar(
                    positionMs = positionMs,
                    onStartOver = { viewModel.startOver() },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(top = 64.dp),
                )
            }

            // Skip intro (admin markers).
            if (!locked && introEndMs > introStartMs && controlState.position in introStartMs until introEndMs) {
                Button(
                    onClick = { viewModel.logFeature("skip_intro"); player.seekTo(introEndMs) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(bottom = 96.dp, end = FilmatubeSpacing.lg),
                ) {
                    Text(stringResource(R.string.player_skip_intro))
                }
            }

            // Up Next card in the last 30s.
            val next = upNext
            val remaining = controlState.duration - controlState.position
            if (!locked && next != null && controlState.duration > 0 && !upNextDismissed && remaining in 1..30_000) {
                UpNextCard(
                    movie = next,
                    secondsLeft = ((remaining + 999) / 1000).toInt().coerceAtLeast(0),
                    onPlayNow = { viewModel.logFeature("up_next"); onPlayNext(next.id) },
                    onDismiss = { upNextDismissed = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(bottom = 96.dp, end = FilmatubeSpacing.lg),
                )
            }
        }
    }

    if (showSubtitleStyle) {
        SubtitleSettingsSheet(
            style = subtitleStyle,
            onStyleChange = viewModel::setSubtitleStyle,
            onDismiss = { showSubtitleStyle = false },
        )
    }
}

@Composable
private fun ResumeBar(
    positionMs: Long,
    onStartOver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
            .padding(start = FilmatubeSpacing.lg, end = FilmatubeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.player_resuming_from, formatTime(positionMs)),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onStartOver) {
            Text(stringResource(R.string.player_start_over), color = MaterialTheme.colorScheme.primary)
        }
    }
}
