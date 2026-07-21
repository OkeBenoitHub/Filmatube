package com.filmatube.app.ui.splash

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.ui.components.FilmatubeLogo
import com.filmatube.app.ui.theme.FilmatubeGreen
import com.filmatube.app.ui.theme.FilmatubeSpacing
import kotlinx.coroutines.delay

private const val MIN_SPLASH_MS = 1600L

/**
 * Animated splash — the logo carries over from the system splash and settles, a green glow
 * sits behind it and the wordmark fades up, then routes to onboarding (first run) or the
 * main app once the flag is read.
 */
@Composable
fun SplashScreen(
    onNavigate: (SplashDestination) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    var started by remember { mutableStateOf(false) }
    var minTimeElapsed by remember { mutableStateOf(false) }

    /**
     * The mark settles rather than making an entrance.
     *
     * The system splash has already been showing this logo, full size, for the moment before
     * this screen existed — so springing up from 0.6 and fading in from nothing made it
     * vanish and pop back, which is what read as "two splash screens". Starting at full
     * opacity and a hair oversized lets the handoff look like one continuous logo that
     * settles into place, with the wordmark reveal below carrying the actual animation.
     */
    val logoScale by animateFloatAsState(
        targetValue = if (started) 1f else 1.06f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logo-scale",
    )
    val wordmarkAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 300),
        label = "wordmark-alpha",
    )

    LaunchedEffect(Unit) {
        started = true
        delay(MIN_SPLASH_MS)
        minTimeElapsed = true
    }

    LaunchedEffect(minTimeElapsed, destination) {
        val target = destination
        if (minTimeElapsed && target != null) {
            onNavigate(target)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        // Ambient green glow behind the mark.
        Box(
            modifier = Modifier
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            FilmatubeGreen.copy(alpha = 0.14f),
                            androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.lg),
        ) {
            FilmatubeLogo(
                size = 104.dp,
                modifier = Modifier.scale(logoScale),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(wordmarkAlpha),
            )
        }
    }
}
