package com.filmatube.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onMovieClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val trending by viewModel.trending.collectAsStateWithLifecycle()
    val recent by viewModel.recentSearches.collectAsStateWithLifecycle()
    val language = LocaleController.currentTag()
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(FilmatubeSpacing.lg),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.search_clear))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.onSubmit()
                keyboard?.hide()
            }),
        )

        // Genre filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
        ) {
            item {
                FilmatubeFilterChip(
                    label = stringResource(R.string.browse_all),
                    selected = filters.genre == null,
                    onClick = { viewModel.setGenre(null) },
                )
            }
            items(Genre.entries, key = { it.key }) { genre ->
                FilmatubeFilterChip(
                    label = stringResource(genre.labelRes),
                    selected = filters.genre == genre.key,
                    onClick = { viewModel.setGenre(genre.key) },
                )
            }
        }

        // Year + rating filters
        Row(
            modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
        ) {
            YearDropdown(filters.year, viewModel::setYear)
            RatingDropdown(filters.minRating, viewModel::setMinRating)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (query.isBlank()) {
                Suggestions(
                    recent = recent,
                    trendingTitles = trending.map { it.title.get(language) },
                    onUse = viewModel::useTerm,
                    onClearRecent = viewModel::clearRecent,
                )
            } else {
                when (val r = results) {
                    DataState.Loading -> LoadingView()
                    DataState.Empty -> EmptyView(message = stringResource(R.string.search_no_results))
                    is DataState.Error -> ErrorView(error = r.error, onRetry = { viewModel.onQueryChange(query) })
                    is DataState.Success -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        contentPadding = PaddingValues(FilmatubeSpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                    ) {
                        items(r.data, key = { it.id }) { movie ->
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Suggestions(
    recent: List<String>,
    trendingTitles: List<String>,
    onUse: (String) -> Unit,
    onClearRecent: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FilmatubeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
    ) {
        if (recent.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.search_recent), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClearRecent) { Text(stringResource(R.string.search_clear)) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                recent.forEach { term ->
                    SuggestionChip(onClick = { onUse(term) }, label = { Text(term) })
                }
            }
        }
        if (trendingTitles.isNotEmpty()) {
            Text(stringResource(R.string.search_trending), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                trendingTitles.forEach { title ->
                    SuggestionChip(onClick = { onUse(title) }, label = { Text(title) })
                }
            }
        }
    }
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
            DropdownMenuItem(text = { Text(stringResource(R.string.browse_year_all)) }, onClick = { onYearSelected(null); expanded = false })
            years.forEach { year ->
                DropdownMenuItem(text = { Text(year.toString()) }, onClick = { onYearSelected(year); expanded = false })
            }
        }
    }
}

@Composable
private fun RatingDropdown(minRating: Double, onRatingSelected: (Double) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = when {
        minRating >= 4.0 -> stringResource(R.string.search_rating_4)
        minRating >= 3.0 -> stringResource(R.string.search_rating_3)
        else -> stringResource(R.string.search_rating)
    }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, contentDescription = null) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.search_rating_any)) }, onClick = { onRatingSelected(0.0); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.search_rating_3)) }, onClick = { onRatingSelected(3.0); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.search_rating_4)) }, onClick = { onRatingSelected(4.0); expanded = false })
        }
    }
}
