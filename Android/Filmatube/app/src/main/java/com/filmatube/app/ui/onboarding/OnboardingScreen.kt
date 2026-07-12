package com.filmatube.app.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.filmatube.app.R
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeTextButton
import com.filmatube.app.ui.theme.FilmatubeGreen
import com.filmatube.app.ui.theme.FilmatubeSpacing
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val titleRes: Int,
    val descRes: Int,
)

private val onboardingPages = listOf(
    OnboardingPage(Icons.Outlined.Download, R.string.onboarding_1_title, R.string.onboarding_1_desc),
    OnboardingPage(Icons.Outlined.Groups, R.string.onboarding_2_title, R.string.onboarding_2_desc),
    OnboardingPage(Icons.Outlined.Theaters, R.string.onboarding_3_title, R.string.onboarding_3_desc),
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    fun finish() = viewModel.completeOnboarding(onFinished)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // Skip
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (!isLastPage) {
                FilmatubeTextButton(
                    text = stringResource(R.string.onboarding_skip),
                    onClick = { finish() },
                )
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            OnboardingPageContent(onboardingPages[page])
        }

        PageIndicator(
            pageCount = onboardingPages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = FilmatubeSpacing.xl),
        )

        FilmatubePrimaryButton(
            text = stringResource(
                if (isLastPage) R.string.onboarding_get_started else R.string.onboarding_next,
            ),
            onClick = {
                if (isLastPage) {
                    finish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.xl)
                .padding(bottom = FilmatubeSpacing.xl)
                .height(52.dp),
        )
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(FilmatubeGreen.copy(alpha = 0.10f), Color.Transparent),
                    ),
                ),
        )
        Column(
            modifier = Modifier.padding(horizontal = FilmatubeSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.lg),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = FilmatubeGreen,
                    modifier = Modifier.size(52.dp),
                )
            }
            Text(
                text = stringResource(page.titleRes),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(page.descRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot-width")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                label = "dot-color",
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}
