package com.filmatube.app.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.filmatube.app.ui.components.PosterTile
import com.filmatube.app.ui.taste.Genre
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.util.LocaleController
import java.util.Calendar

@Composable
fun BrowseScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = LocaleController.currentTag()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.sm, vertical = FilmatubeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
            Text(stringResource(R.string.browse_title), style = MaterialTheme.typography.titleLarge)
        }

        // Sort + Year
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SortChip(stringResource(R.string.browse_sort_newest), state.sort == MovieSort.NEWEST) { viewModel.setSort(MovieSort.NEWEST) }
            SortChip(stringResource(R.string.browse_sort_rating), state.sort == MovieSort.RATING) { viewModel.setSort(MovieSort.RATING) }
            SortChip(stringResource(R.string.browse_sort_az), state.sort == MovieSort.ALPHA) { viewModel.setSort(MovieSort.ALPHA) }
            YearDropdown(selectedYear = state.year, onYearSelected = viewModel::setYear)
        }

        // Genre chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
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

        Box(modifier = Modifier.fillMaxSize()) {
            when (val movies = state.movies) {
                DataState.Loading -> LoadingView()
                DataState.Empty -> EmptyView(message = stringResource(R.string.browse_empty))
                is DataState.Error -> ErrorView(error = movies.error, onRetry = viewModel::load)
                is DataState.Success -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(FilmatubeSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                ) {
                    items(movies.data, key = { it.id }) { movie ->
                        PosterTile(
                            posterUrl = movie.posterUrl,
                            title = movie.title.get(language),
                            width = null,
                            onClick = { onMovieClick(movie.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilmatubeFilterChip(label = label, selected = selected, onClick = onClick)
}

@Composable
private fun YearDropdown(selectedYear: Int?, onYearSelected: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val years = remember(currentYear) { (currentYear downTo currentYear - 14).toList() }

    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(selectedYear?.toString() ?: stringResource(R.string.browse_year)) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
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
