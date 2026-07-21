package com.filmatube.app.ui.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.domain.repository.MovieSort
import com.filmatube.app.domain.util.DataState
import com.filmatube.app.ui.components.EmptyView
import com.filmatube.app.ui.components.ErrorView
import com.filmatube.app.ui.components.FilmatubeFilterChip
import com.filmatube.app.ui.components.LoadingView
import com.filmatube.app.ui.components.MoviePosterTile
import com.filmatube.app.ui.taste.Genre
import com.filmatube.app.ui.taste.genreLabel
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.util.LocaleController
import java.util.Calendar

/** One filter chip, described as data so its position in the row can be looked up by [id]. */
private data class ChipSpec(
    val id: String,
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * Index of [genreKey]'s chip in the genre row, or -1 if it has none.
 *
 * The row renders "All" first and then [Genre.entries], so every genre sits one position
 * later than its enum ordinal. Extracted from the composable so the off-by-one is covered by
 * a test across all genres rather than spot-checked on whichever one happened to be open.
 *
 * Returns -1 for a key with no chip — possible if a stored `genrePreferences` value outlives
 * its enum entry — and callers skip scrolling rather than jumping to "All".
 */
internal fun genreChipIndex(genreKey: String?): Int {
    if (genreKey == null) return -1
    val ordinal = Genre.entries.indexOfFirst { it.key == genreKey }
    return if (ordinal < 0) -1 else ordinal + 1
}

/**
 * Browse: the whole catalog behind a sort + genre + year filter.
 *
 * The filter bar sits on its own raised surface so the grid scrolls *under* something, rather
 * than the controls and content sharing one flat plane. Both filter rows scroll horizontally —
 * a fixed Row clipped its last chip on narrow screens once the year control was added.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = LocaleController.currentTag()

    val resultCount = (state.movies as? DataState.Success)?.data?.size

    // Name the slice you're actually looking at. Arriving from Home's "See all" otherwise
    // gave a screen headed "Browse" with no hint which row sent you here.
    val heading = when {
        state.genre != null -> genreLabel(state.genre!!)
        state.comingSoon -> stringResource(R.string.row_coming_soon)
        state.sort == MovieSort.POPULAR -> stringResource(R.string.row_trending)
        state.sort == MovieSort.RATING -> stringResource(R.string.browse_sort_rating)
        else -> stringResource(R.string.browse_title)
    }

    // Declared as data rather than hand-written item{} blocks so the reveal-scroll below can
    // address a chip by id — hand-counted indices would rot the moment these are reordered.
    val sortChips = listOf(
        ChipSpec("NEWEST", stringResource(R.string.browse_sort_newest), state.sort == MovieSort.NEWEST) {
            viewModel.setSort(MovieSort.NEWEST)
        },
        ChipSpec("POPULAR", stringResource(R.string.row_trending), state.sort == MovieSort.POPULAR) {
            viewModel.setSort(MovieSort.POPULAR)
        },
        ChipSpec("RATING", stringResource(R.string.browse_sort_rating), state.sort == MovieSort.RATING) {
            viewModel.setSort(MovieSort.RATING)
        },
        // Not a sort but a scope; it lives here because it's the other axis a Home row
        // narrows by, and it has to be un-toggleable.
        ChipSpec("COMING_SOON", stringResource(R.string.row_coming_soon), state.comingSoon) {
            viewModel.toggleComingSoon()
        },
        ChipSpec("ALPHA", stringResource(R.string.browse_sort_az), state.sort == MovieSort.ALPHA) {
            viewModel.setSort(MovieSort.ALPHA)
        },
    )

    val sortRowState = rememberLazyListState()
    val genreRowState = rememberLazyListState()

    /**
     * Bring the chip you arrived on into view.
     *
     * Landing from a "See all" left the rows scrolled to the start, so the chip explaining
     * what you were looking at sat off-screen to the right. Runs once on entry — re-running
     * on every selection change would yank the row around while you were browsing it.
     *
     * Coming Soon needs the explicit precedence below: it's selected *alongside* the default
     * "Newest" sort, so simply finding the first selected chip would stop at Newest and
     * never scroll.
     */
    LaunchedEffect(Unit) {
        val sortFocus = when {
            state.comingSoon -> sortChips.indexOfFirst { it.id == "COMING_SOON" }
            state.sort != MovieSort.NEWEST -> sortChips.indexOfFirst { it.id == state.sort.name }
            else -> -1
        }
        // Land one chip early so there's visible context to the left, making it obvious the
        // row scrolls rather than looking like the earlier chips don't exist.
        if (sortFocus > 0) sortRowState.scrollToItem(sortFocus - 1)

        val genreFocus = genreChipIndex(state.genre)
        if (genreFocus > 0) genreRowState.scrollToItem(genreFocus - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(heading, style = MaterialTheme.typography.titleLarge)
                        // The count doubles as feedback that a filter actually did something.
                        if (resultCount != null) {
                            Text(
                                stringResource(R.string.browse_results, resultCount),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back),
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(visible = state.hasFilters) {
                        TextButton(onClick = viewModel::clearFilters) {
                            Text(stringResource(R.string.browse_clear))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Filter bar ────────────────────────────────────────
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(modifier = Modifier.padding(bottom = FilmatubeSpacing.sm)) {
                    // Sort + year share a row: they're both "how to order/narrow", as opposed
                    // to genre which is "what to see".
                    LazyRow(
                        state = sortRowState,
                        contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                    ) {
                        items(sortChips, key = { it.id }) { chip ->
                            FilmatubeFilterChip(
                                label = chip.label,
                                selected = chip.selected,
                                onClick = chip.onClick,
                            )
                        }
                        item { YearChip(selectedYear = state.year, onYearSelected = viewModel::setYear) }
                    }

                    LazyRow(
                        state = genreRowState,
                        contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                        modifier = Modifier.padding(top = FilmatubeSpacing.xs),
                    ) {
                        item {
                            FilmatubeFilterChip(
                                label = stringResource(R.string.browse_all),
                                selected = state.genre == null,
                                onClick = { viewModel.setGenre(null) },
                            )
                        }
                        items(Genre.entries, key = { it.key }) { genre ->
                            FilmatubeFilterChip(
                                label = stringResource(genre.labelRes),
                                selected = state.genre == genre.key,
                                onClick = { viewModel.setGenre(genre.key) },
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Results ───────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize()) {
                when (val movies = state.movies) {
                    DataState.Loading -> LoadingView()
                    DataState.Empty -> EmptyView(
                        title = stringResource(R.string.browse_empty_title),
                        message = stringResource(R.string.browse_empty),
                    )
                    is DataState.Error -> ErrorView(error = movies.error, onRetry = viewModel::load)
                    is DataState.Success -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 108.dp),
                        contentPadding = PaddingValues(
                            start = FilmatubeSpacing.lg,
                            end = FilmatubeSpacing.lg,
                            top = FilmatubeSpacing.lg,
                            bottom = FilmatubeSpacing.xxl,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.lg),
                    ) {
                        items(movies.data, key = { it.id }) { movie ->
                            MoviePosterTile(
                                movie = movie,
                                language = language,
                                width = null,
                                onClick = { onMovieClick(movie.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Year filter styled as a [FilterChip] rather than an AssistChip.
 *
 * It sits shoulder-to-shoulder with the sort chips, so it has to carry the same visual
 * weight: an AssistChip is a different chip family, and its trailing icon defaults to 24dp
 * against the chips' 18dp, which made this control read as noticeably bigger than its
 * neighbours. Selecting a year also fills it in, so an active filter is visible at a glance.
 */
@Composable
private fun YearChip(selectedYear: Int?, onYearSelected: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val years = remember(currentYear) { (currentYear downTo currentYear - 14).toList() }

    Box {
        FilterChip(
            selected = selectedYear != null,
            onClick = { expanded = true },
            label = { Text(selectedYear?.toString() ?: stringResource(R.string.browse_year)) },
            trailingIcon = {
                Icon(
                    Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.browse_year_all)) },
                onClick = { onYearSelected(null); expanded = false },
            )
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString()) },
                    onClick = { onYearSelected(year); expanded = false },
                )
            }
        }
    }
}
