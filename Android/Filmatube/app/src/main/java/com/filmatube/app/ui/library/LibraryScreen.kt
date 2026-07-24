package com.filmatube.app.ui.library

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.ui.components.CollectionCoverCard
import com.filmatube.app.ui.components.ContentRow
import com.filmatube.app.ui.components.ContinueWatchingTile
import com.filmatube.app.ui.components.MoviePosterTile
import com.filmatube.app.ui.theme.FilmatubeBrandGreen
import com.filmatube.app.ui.theme.FilmatubeBrandGreenDeep
import com.filmatube.app.ui.theme.FilmatubeSpacing

/**
 * Personal library, mirroring the web page: a hero header, a Continue Watching row, and a
 * Watch Later grid. (Collections are a web-only feature and have no Android data layer yet.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onCollectionClick: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val language = com.filmatube.app.util.LocaleController.currentTag()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_library)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = FilmatubeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.lg),
        ) {
            // Hero — the web library leads with a gradient icon tile beside a big title.
            item {
                Row(
                    modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(FilmatubeBrandGreen, FilmatubeBrandGreenDeep))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.VideoLibrary,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        stringResource(R.string.my_library),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            if (continueWatching.isNotEmpty()) {
                item {
                    ContentRow(
                        title = stringResource(R.string.row_continue_watching),
                        items = continueWatching,
                        key = { it.movie.id },
                    ) { entry ->
                        ContinueWatchingTile(
                            posterUrl = entry.movie.posterUrl,
                            title = entry.movie.title.get(language),
                            progress = entry.progress,
                            onClick = { onMovieClick(entry.movie.id) },
                        )
                    }
                }
            }

            // Collections — mirrors the web Library's middle section: a row of cover cards, each
            // opening the collection. Read-only on Android (created on web); hidden when there
            // are none, since there's no way to create one here.
            if (collections.isNotEmpty()) {
                item {
                    ContentRow(
                        title = stringResource(R.string.library_collections),
                        items = collections,
                        key = { it.id },
                    ) { collection ->
                        CollectionCoverCard(
                            collection = collection,
                            onClick = { onCollectionClick(collection.id) },
                        )
                    }
                }
            }

            // Watch Later — its own titled section, then the saved grid.
            item {
                Text(
                    stringResource(R.string.detail_watch_later),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        start = FilmatubeSpacing.lg,
                        end = FilmatubeSpacing.lg,
                        top = FilmatubeSpacing.sm,
                    ),
                )
            }

            if (watchlist.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(FilmatubeSpacing.xxl),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp),
                            )
                            Text(
                                stringResource(R.string.library_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = FilmatubeSpacing.sm),
                            )
                        }
                    }
                }
            } else {
                // A wrapping poster grid, laid out row by row inside the LazyColumn so the whole
                // screen is one scroll rather than a grid nested in a column.
                item {
                    WatchLaterGrid(movies = watchlist, language = language, onMovieClick = onMovieClick)
                }
            }
        }
    }
}

@Composable
private fun WatchLaterGrid(movies: List<Movie>, language: String, onMovieClick: (String) -> Unit) {
    val columns = 3
    Column(
        modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
    ) {
        movies.chunked(columns).forEach { rowMovies ->
            Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
                rowMovies.forEach { movie ->
                    Box(modifier = Modifier.weight(1f)) {
                        MoviePosterTile(
                            movie = movie,
                            language = language,
                            width = null,
                            onClick = { onMovieClick(movie.id) },
                        )
                    }
                }
                // Pad the last row so its tiles keep the same width as full rows.
                repeat(columns - rowMovies.size) { Box(modifier = Modifier.weight(1f)) {} }
            }
        }
    }
}
