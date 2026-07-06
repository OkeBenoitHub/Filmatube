package com.filmatube.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.theme.FilmatubeSpacing

/** Auto-rotating featured-movie hero. */
@Composable
fun FeaturedHero(
    movies: List<Movie>,
    language: String,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (movies.isEmpty()) return
    val pagerState = rememberPagerState { movies.size }

    LaunchedEffect(movies.size) {
        if (movies.size <= 1) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(5_000)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % movies.size)
        }
    }

    Box(modifier = modifier.fillMaxWidth().height(440.dp)) {
        HorizontalPager(state = pagerState) { page ->
            HeroSlide(movie = movies[page], language = language, onClick = onMovieClick)
        }
        if (movies.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = FilmatubeSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(movies.size) { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.4f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSlide(movie: Movie, language: String, onClick: (String) -> Unit) {
    Box(modifier = Modifier.fillMaxSize().clickable { onClick(movie.id) }) {
        AsyncImage(
            model = movie.backdropUrl.ifBlank { movie.posterUrl },
            contentDescription = movie.title.get(language),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(FilmatubeSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
        ) {
            Text(
                text = movie.title.get(language),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(movie.year)
                    if (movie.ageRating.isNotBlank()) append("  •  ${movie.ageRating}")
                    if (movie.averageRating > 0) append("  •  ★ ${"%.1f".format(movie.averageRating)}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            FilmatubePrimaryButton(
                text = stringResource(R.string.detail_play),
                onClick = { onClick(movie.id) },
                leadingIcon = Icons.Filled.PlayArrow,
            )
        }
    }
}
