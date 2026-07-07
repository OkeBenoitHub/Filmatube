package com.filmatube.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.filmatube.app.ui.components.ErrorView

/**
 * Full-screen movie player. Progressive MP4 from a token-protected R2 URL via
 * [PlayerViewModel], rendered under a custom Compose control overlay
 * ([PlayerControls]) with immersive fullscreen. Gestures arrive on Day 45.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val player = viewModel.player
    val controlState = rememberPlayerControlState(player)

    var controlsVisible by remember { mutableStateOf(true) }
    var immersive by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }

    ImmersiveFullscreen(immersive)

    // Pause playback when the screen leaves the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-hide the controls after a few seconds of no interaction while playing.
    LaunchedEffect(interaction, controlsVisible, controlState.isPlaying, controlState.isScrubbing) {
        if (controlsVisible && controlState.isPlaying && !controlState.isScrubbing) {
            kotlinx.coroutines.delay(3500)
            controlsVisible = false
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
            modifier = Modifier.fillMaxSize(),
        )

        // Tap anywhere to toggle the controls.
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

        if (controlsVisible && uiState == PlayerUiState.Ready) {
            PlayerControls(
                player = player,
                state = controlState,
                immersive = immersive,
                onToggleImmersive = { immersive = !immersive },
                onBack = onBack,
                onInteract = { interaction++ },
            )
        }

        when (val state = uiState) {
            PlayerUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
            is PlayerUiState.Error -> ErrorView(error = state.error, onRetry = viewModel::load)
            PlayerUiState.Ready -> Unit
        }
    }
}
