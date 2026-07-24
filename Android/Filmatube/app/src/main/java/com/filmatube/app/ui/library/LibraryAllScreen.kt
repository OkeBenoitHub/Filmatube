package com.filmatube.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.ui.components.CollectionCoverCard
import com.filmatube.app.ui.components.ContinueWatchingTile
import com.filmatube.app.ui.components.EmptyView
import com.filmatube.app.ui.components.MoviePosterTile
import com.filmatube.app.ui.theme.FilmatubeSpacing

const val LIBRARY_SECTION_CONTINUE = "continue"
const val LIBRARY_SECTION_WATCHLIST = "watchlist"
const val LIBRARY_SECTION_COLLECTIONS = "collections"

/** Full-grid "See all" screen for a single Library section. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryAllScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onCollectionClick: (String) -> Unit,
    viewModel: LibraryAllViewModel = hiltViewModel(),
) {
    val language = com.filmatube.app.util.LocaleController.currentTag()
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()

    val titleRes = when (viewModel.section) {
        LIBRARY_SECTION_CONTINUE -> R.string.row_continue_watching
        LIBRARY_SECTION_COLLECTIONS -> R.string.library_collections
        else -> R.string.detail_watch_later
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        val isEmpty = when (viewModel.section) {
            LIBRARY_SECTION_CONTINUE -> continueWatching.isEmpty()
            LIBRARY_SECTION_COLLECTIONS -> collections.isEmpty()
            else -> watchlist.isEmpty()
        }
        if (isEmpty) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyView(icon = Icons.Outlined.Inbox, title = stringResource(titleRes), message = stringResource(R.string.library_empty))
            }
            return@Scaffold
        }

        // Collections are 16:9 cards → wider min size; posters are narrower.
        val minSize = if (viewModel.section == LIBRARY_SECTION_COLLECTIONS) 160.dp else 110.dp
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minSize),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(FilmatubeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            when (viewModel.section) {
                LIBRARY_SECTION_CONTINUE -> items(continueWatching, key = { it.movie.id }) { entry ->
                    ContinueWatchingTile(
                        posterUrl = entry.movie.posterUrl,
                        title = entry.movie.title.get(language),
                        progress = entry.progress,
                        onClick = { onMovieClick(entry.movie.id) },
                        width = null,
                    )
                }
                LIBRARY_SECTION_COLLECTIONS -> items(collections, key = { it.id }) { collection ->
                    CollectionCoverCard(collection = collection, onClick = { onCollectionClick(collection.id) }, width = null)
                }
                else -> items(watchlist, key = { it.id }) { movie ->
                    MoviePosterTile(movie = movie, language = language, width = null, onClick = { onMovieClick(movie.id) })
                }
            }
        }
    }
}
