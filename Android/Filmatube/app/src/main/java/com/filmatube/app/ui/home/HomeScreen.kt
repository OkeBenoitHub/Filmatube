package com.filmatube.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.ui.components.ContentRow
import com.filmatube.app.ui.components.ContentRowShimmer
import com.filmatube.app.ui.components.EmptyView
import com.filmatube.app.ui.components.ErrorView
import com.filmatube.app.ui.components.PosterTile
import com.filmatube.app.ui.taste.Genre
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.ui.theme.PosterTileWidth
import com.filmatube.app.util.LocaleController

@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit,
    onBrowse: (String?) -> Unit,
    onPlay: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = LocaleController.currentTag()

    Column(modifier = Modifier.fillMaxSize()) {
        HomeTopBar()
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> HomeLoading()
                state.error != null -> ErrorView(error = state.error!!, onRetry = viewModel::load)
                state.isEmpty -> EmptyView(
                    title = stringResource(R.string.home_empty_title),
                    message = stringResource(R.string.home_empty_message),
                )
                else -> HomeContent(state, language, onMovieClick, onBrowse, onPlay)
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    language: String,
    onMovieClick: (String) -> Unit,
    onBrowse: (String?) -> Unit,
    onPlay: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.lg),
    ) {
        if (state.featured.isNotEmpty()) {
            FeaturedHero(movies = state.featured, language = language, onMovieClick = onMovieClick)
        }
        if (state.continueWatching.isNotEmpty()) {
            ContentRow(
                title = stringResource(R.string.row_continue_watching),
                items = state.continueWatching,
                key = { it.movie.id },
            ) { item ->
                ContinueWatchingTile(item = item, language = language, onPlay = onPlay)
            }
        }
        if (state.trending.isNotEmpty()) {
            MovieRow(stringResource(R.string.row_trending), state.trending, language, onMovieClick) { onBrowse(null) }
        }
        if (state.newReleases.isNotEmpty()) {
            MovieRow(stringResource(R.string.row_new_releases), state.newReleases, language, onMovieClick) { onBrowse(null) }
        }
        state.genreRows.forEach { row ->
            MovieRow(genreLabel(row.genreKey), row.movies, language, onMovieClick) { onBrowse(row.genreKey) }
        }
        if (state.comingSoon.isNotEmpty()) {
            MovieRow(stringResource(R.string.row_coming_soon), state.comingSoon, language, onMovieClick) { onBrowse(null) }
        }
        Spacer(Modifier.height(FilmatubeSpacing.xxl))
    }
}

@Composable
private fun ContinueWatchingTile(
    item: ContinueWatchingItem,
    language: String,
    onPlay: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(PosterTileWidth)
            .clickable { onPlay(item.movie.id) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = item.movie.posterUrl,
                contentDescription = item.movie.title.get(language),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            LinearProgressIndicator(
                progress = { item.progress.coerceIn(0f, 1f) },
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp),
            )
        }
        Spacer(Modifier.height(FilmatubeSpacing.sm))
        Text(
            text = item.movie.title.get(language),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MovieRow(
    title: String,
    movies: List<Movie>,
    language: String,
    onMovieClick: (String) -> Unit,
    onSeeAll: () -> Unit,
) {
    ContentRow(title = title, items = movies, key = { it.id }, onSeeAll = onSeeAll) { movie ->
        PosterTile(
            posterUrl = movie.posterUrl,
            title = movie.title.get(language),
            onClick = { onMovieClick(movie.id) },
        )
    }
}

@Composable
private fun HomeLoading() {
    Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.lg)) {
        Box(
            modifier = Modifier
                .padding(FilmatubeSpacing.lg)
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        repeat(3) { ContentRowShimmer() }
    }
}

@Composable
private fun genreLabel(key: String): String {
    val res = Genre.entries.find { it.key == key }?.labelRes
    return if (res != null) stringResource(res) else key.replaceFirstChar { it.uppercase() }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
            TopBarIcon(Icons.Outlined.Notifications, stringResource(R.string.notifications))
            TopBarIcon(Icons.Outlined.Person, stringResource(R.string.nav_profile))
        }
    }
}

@Composable
private fun TopBarIcon(icon: ImageVector, contentDescription: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
