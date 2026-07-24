package com.filmatube.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.ui.components.CollectionCoverCard
import com.filmatube.app.ui.components.ContinueWatchingTile
import com.filmatube.app.ui.components.MoviePosterTile
import com.filmatube.app.ui.theme.FilmatubeBrandGreen
import com.filmatube.app.ui.theme.FilmatubeBrandGreenDeep
import com.filmatube.app.ui.theme.FilmatubeSpacing

/**
 * Personal library, mirroring the web page: a hero, then Continue Watching, Collections and Watch
 * Later — each a horizontal row with a "See all". Collections can be created here (the + in its
 * header) and opened to view/edit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onCollectionClick: (String) -> Unit,
    onSeeAllContinue: () -> Unit,
    onSeeAllCollections: () -> Unit,
    onSeeAllWatchlist: () -> Unit,
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
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.settings_back))
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
            // Hero — a gradient icon tile beside a big title, like the web library.
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
                        Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        stringResource(R.string.my_library),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            // Continue Watching.
            if (continueWatching.isNotEmpty()) {
                item {
                    LibrarySectionHeader(
                        title = stringResource(R.string.row_continue_watching),
                        onSeeAll = onSeeAllContinue,
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                    ) {
                        items(continueWatching, key = { it.movie.id }) { entry ->
                            ContinueWatchingTile(
                                posterUrl = entry.movie.posterUrl,
                                title = entry.movie.title.get(language),
                                progress = entry.progress,
                                onClick = { onMovieClick(entry.movie.id) },
                            )
                        }
                    }
                }
            }

            // Collections — header always shown so the + create button is reachable even when empty.
            item {
                LibrarySectionHeader(
                    title = stringResource(R.string.library_collections),
                    // Create, then open the new collection's editor — mirrors the web New button.
                    onAdd = { viewModel.createCollection { id -> onCollectionClick(id) } },
                    onSeeAll = if (collections.isNotEmpty()) onSeeAllCollections else null,
                )
            }
            if (collections.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.collection_none_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg),
                    )
                }
            } else {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                    ) {
                        items(collections, key = { it.id }) { collection ->
                            CollectionCoverCard(collection = collection, onClick = { onCollectionClick(collection.id) })
                        }
                    }
                }
            }

            // Watch Later.
            item {
                LibrarySectionHeader(
                    title = stringResource(R.string.detail_watch_later),
                    onSeeAll = if (watchlist.isNotEmpty()) onSeeAllWatchlist else null,
                )
            }
            if (watchlist.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(FilmatubeSpacing.xxl), contentAlignment = Alignment.Center) {
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
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                    ) {
                        items(watchlist.take(18), key = { it.id }) { movie ->
                            MoviePosterTile(movie = movie, language = language, onClick = { onMovieClick(movie.id) })
                        }
                    }
                }
            }
        }
    }
}

/** Section header used across the Library — a modest titleMedium title with optional + / See all. */
@Composable
private fun LibrarySectionHeader(
    title: String,
    onAdd: (() -> Unit)? = null,
    onSeeAll: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = FilmatubeSpacing.lg, end = FilmatubeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onAdd != null) {
                IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.collection_new), modifier = Modifier.size(20.dp))
                }
            }
            if (onSeeAll != null) {
                TextButton(onClick = onSeeAll) { Text(stringResource(R.string.action_see_all)) }
            }
        }
    }
}
