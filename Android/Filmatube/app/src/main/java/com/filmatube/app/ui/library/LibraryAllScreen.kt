package com.filmatube.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.ui.components.CollectionCoverCard
import com.filmatube.app.ui.components.ContinueWatchingTile
import com.filmatube.app.ui.components.EmptyView
import com.filmatube.app.ui.components.MoviePosterTile
import com.filmatube.app.ui.components.PageHero
import com.filmatube.app.ui.theme.FilmatubeSpacing

const val LIBRARY_SECTION_CONTINUE = "continue"
const val LIBRARY_SECTION_WATCHLIST = "watchlist"
const val LIBRARY_SECTION_COLLECTIONS = "collections"

/**
 * Full "See all" screen for a Library section. Leads with the shared [PageHero] (gradient tile +
 * title) like the other pages; the collections view adds a name search, mirroring the web
 * Collections page.
 */
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
    val section = viewModel.section

    var query by remember { mutableStateOf("") }
    val isCollections = section == LIBRARY_SECTION_COLLECTIONS

    val titleRes = when (section) {
        LIBRARY_SECTION_CONTINUE -> R.string.row_continue_watching
        LIBRARY_SECTION_COLLECTIONS -> R.string.library_collections
        else -> R.string.detail_watch_later
    }
    val heroIcon = when (section) {
        LIBRARY_SECTION_CONTINUE -> Icons.Outlined.PlayCircle
        LIBRARY_SECTION_COLLECTIONS -> Icons.Outlined.Layers
        else -> Icons.Outlined.Bookmark
    }
    val filteredCollections = remember(collections, query) {
        val term = query.trim().lowercase()
        if (term.isEmpty()) collections else collections.filter { it.title.lowercase().contains(term) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        val isEmpty = when (section) {
            LIBRARY_SECTION_CONTINUE -> continueWatching.isEmpty()
            LIBRARY_SECTION_COLLECTIONS -> collections.isEmpty()
            else -> watchlist.isEmpty()
        }

        val minSize = if (isCollections) 160.dp else 110.dp
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minSize),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = FilmatubeSpacing.lg,
                end = FilmatubeSpacing.lg,
                bottom = FilmatubeSpacing.xxl,
            ),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            // The hero + search already sit inside the grid's horizontal inset, so PageHero adds
            // none of its own (it would otherwise drift inward by a second lg).
            item(span = { GridItemSpan(maxLineSpan) }) {
                PageHero(
                    eyebrow = stringResource(R.string.my_library),
                    title = stringResource(titleRes),
                    subtitle = if (isCollections) stringResource(R.string.collections_subtitle) else null,
                    icon = heroIcon,
                    horizontalPadding = 0.dp,
                )
            }

            if (isCollections && !isEmpty) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.collections_search_hint)) },
                    )
                }
            }

            if (isEmpty) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(FilmatubeSpacing.xxl), contentAlignment = Alignment.Center) {
                        EmptyView(icon = Icons.Outlined.Inbox, title = stringResource(titleRes), message = stringResource(R.string.library_empty))
                    }
                }
            } else {
                when (section) {
                    LIBRARY_SECTION_CONTINUE -> items(continueWatching, key = { it.movie.id }) { entry ->
                        ContinueWatchingTile(
                            posterUrl = entry.movie.posterUrl,
                            title = entry.movie.title.get(language),
                            progress = entry.progress,
                            onClick = { onMovieClick(entry.movie.id) },
                            width = null,
                        )
                    }
                    LIBRARY_SECTION_COLLECTIONS -> {
                        if (filteredCollections.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    stringResource(R.string.search_no_results),
                                    modifier = Modifier.padding(FilmatubeSpacing.lg),
                                )
                            }
                        } else {
                            items(filteredCollections, key = { it.id }) { collection ->
                                CollectionCoverCard(collection = collection, onClick = { onCollectionClick(collection.id) }, width = null)
                            }
                        }
                    }
                    else -> items(watchlist, key = { it.id }) { movie ->
                        MoviePosterTile(movie = movie, language = language, width = null, onClick = { onMovieClick(movie.id) })
                    }
                }
            }
        }
    }
}
