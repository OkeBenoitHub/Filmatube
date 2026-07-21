package com.filmatube.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.data.download.DownloadRepository
import com.filmatube.app.data.library.WatchlistRepository
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.ui.theme.FilmatubeShapes
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.ui.theme.PosterTileWidth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Navigation a movie option sheet needs, wherever it's opened from.
 *
 * Supplied through a CompositionLocal rather than threaded as parameters: the sheet hangs off
 * [PosterTile], which appears in home rows, browse, search, library, actor and "more like
 * this" — six screens whose signatures would all have to grow the same three lambdas, plus
 * every intermediate composable in between. The graph provides these once. The cost is that a
 * tile rendered outside the provider silently gets no-ops, which is why the default throws in
 * debug via [LocalMovieActions]'s error default.
 */
data class MovieActionHandlers(
    val onPlay: (String) -> Unit,
    val onOpenDetails: (String) -> Unit,
    val onCreateParty: (String) -> Unit,
)

val LocalMovieActions: ProvidableCompositionLocal<MovieActionHandlers> = compositionLocalOf {
    error("No MovieActionHandlers provided — wrap the nav graph in ProvideMovieActions")
}

@Composable
fun ProvideMovieActions(handlers: MovieActionHandlers, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMovieActions provides handlers, content = content)
}

/**
 * Watch Later + download state for the option sheet.
 *
 * One ViewModel shared by every tile on screen rather than one per tile: both flows are
 * whole-collection observers, so per-tile instances would open a Firestore listener and a Room
 * query for every poster in a grid.
 */
@HiltViewModel
class MovieActionsViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    val savedIds: StateFlow<List<String>> = watchlistRepository.observeSavedIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloadedIds: StateFlow<List<String>> = downloadRepository.items()
        .map { items -> items.map { it.movieId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleSaved(movieId: String) {
        viewModelScope.launch { watchlistRepository.toggle(movieId) }
    }

    fun toggleDownload(movie: Movie, isDownloaded: Boolean) {
        viewModelScope.launch {
            if (isDownloaded) downloadRepository.cancel(movie.id)
            else runCatching { downloadRepository.download(movie) }
        }
    }
}

/**
 * The movie option sheet: what you get from the tile's ⋮ button or a long press.
 *
 * A bottom sheet rather than a dropdown because it can show the poster and title — in a dense
 * grid, a bare menu leaves you unsure which movie you're acting on. Only one-tap actions live
 * here; recommending to a friend and sharing to a board need a picker, so they stay on the
 * detail screen where there's room for one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieOptionsSheet(
    movie: Movie,
    language: String,
    onDismiss: () -> Unit,
    viewModel: MovieActionsViewModel = hiltViewModel(),
) {
    val actions = LocalMovieActions.current
    val sheetState = rememberModalBottomSheetState()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()

    val isSaved = movie.id in savedIds
    val isDownloaded = movie.id in downloadedIds
    val title = movie.title.get(language)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // Header: which movie this is about.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.lg)
                .padding(bottom = FilmatubeSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(56.dp)
                    .aspectRatio(2f / 3f)
                    .clip(FilmatubeShapes.medium),
            )
            Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(movie.year)
                        if (movie.ageRating.isNotBlank()) append("  •  ${movie.ageRating}")
                        if (movie.averageRating > 0) append("  •  ★ ${"%.1f".format(movie.averageRating)}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // A coming-soon title has nothing to stream, so playing and downloading are hidden
        // rather than shown-and-failing.
        if (!movie.isComingSoon) {
            OptionRow(Icons.Filled.PlayArrow, stringResource(R.string.detail_play)) {
                onDismiss(); actions.onPlay(movie.id)
            }
        }

        OptionRow(
            icon = if (isSaved) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkAdd,
            label = stringResource(if (isSaved) R.string.detail_saved else R.string.detail_watch_later),
            highlighted = isSaved,
        ) {
            viewModel.toggleSaved(movie.id)
            onDismiss()
        }

        if (!movie.isComingSoon) {
            OptionRow(
                icon = if (isDownloaded) Icons.Outlined.DownloadDone else Icons.Outlined.Download,
                label = stringResource(
                    if (isDownloaded) R.string.movie_options_downloaded else R.string.movie_options_download,
                ),
                highlighted = isDownloaded,
            ) {
                viewModel.toggleDownload(movie, isDownloaded)
                onDismiss()
            }

            OptionRow(Icons.Outlined.Groups, stringResource(R.string.party_create_action)) {
                onDismiss(); actions.onCreateParty(movie.id)
            }
        }

        OptionRow(Icons.Outlined.Info, stringResource(R.string.movie_options_details)) {
            onDismiss(); actions.onOpenDetails(movie.id)
        }

        // Breathing room above the gesture bar.
        Column(modifier = Modifier.padding(bottom = FilmatubeSpacing.xl)) {}
    }
}

/**
 * A [PosterTile] wired to the option sheet — the tile every movie grid and row should use.
 *
 * Owns the open/closed state per tile so a screen doesn't have to track "which poster's menu
 * is showing"; the sheet itself is only composed once opened, so a 40-poster grid pays nothing
 * for menus nobody has touched.
 */
@Composable
fun MoviePosterTile(
    movie: Movie,
    language: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = PosterTileWidth,
) {
    var showOptions by rememberSaveable(movie.id) { mutableStateOf(false) }

    PosterTile(
        posterUrl = movie.posterUrl,
        title = movie.title.get(language),
        modifier = modifier,
        width = width,
        onClick = onClick,
        onMoreClick = { showOptions = true },
    )

    if (showOptions) {
        MovieOptionsSheet(movie = movie, language = language, onDismiss = { showOptions = false })
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.lg),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
