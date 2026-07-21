package com.filmatube.app.ui.boards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.ui.components.FilmatubeFilterChip
import com.filmatube.app.ui.components.FilmatubeTextField
import com.filmatube.app.ui.components.PageHero
import com.filmatube.app.ui.theme.FilmatubeSpacing

/**
 * Board discovery, mirroring the web page: hero, then Featured, My boards and All boards as
 * card grids, with a search box and type filters over the last of them.
 *
 * One grid rather than the horizontal rows this used to use. A LazyRow of 240dp cards showed
 * two boards at a time and hid the rest behind a sideways scroll nobody performs; the web
 * lays every section out as a grid for the same reason.
 */
@Composable
fun BoardsScreen(
    onBoardClick: (String) -> Unit,
    onCreateBoard: () -> Unit,
    viewModel: BoardsViewModel = hiltViewModel(),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val featured by viewModel.featured.collectAsStateWithLifecycle()
    val boards by viewModel.boards.collectAsStateWithLifecycle()
    val myBoards by viewModel.myBoards.collectAsStateWithLifecycle()

    // Featured and "mine" are discovery shortcuts, not search results — hiding them while a
    // query is active keeps the answer to "what did I search for" on screen. Matches the web.
    val searching = query.isNotBlank()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateBoard,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.board_new)) },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = FilmatubeSpacing.lg,
                end = FilmatubeSpacing.lg,
                bottom = 96.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            fullWidth {
                PageHero(
                    eyebrow = stringResource(R.string.boards_eyebrow),
                    title = stringResource(R.string.boards_title),
                    subtitle = stringResource(R.string.boards_subtitle),
                    icon = Icons.AutoMirrored.Filled.Chat,
                    // The grid already insets its content; letting the hero pad again would
                    // push the header in twice as far as everything below it.
                    horizontalPadding = 0.dp,
                )
            }

            if (featured.isNotEmpty() && !searching) {
                fullWidth { SectionTitle(stringResource(R.string.boards_featured)) }
                items(featured, key = { "featured-${it.id}" }) { board ->
                    BoardCard(board = board, onClick = { onBoardClick(board.id) })
                }
            }

            if (myBoards.isNotEmpty() && !searching) {
                fullWidth { SectionTitle(stringResource(R.string.boards_my)) }
                items(myBoards, key = { "mine-${it.id}" }) { board ->
                    BoardCard(board = board, onClick = { onBoardClick(board.id) })
                }
            }

            fullWidth { SectionTitle(stringResource(R.string.boards_all)) }

            fullWidth {
                FilmatubeTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    label = stringResource(R.string.boards_search_hint),
                    leadingIcon = Icons.Outlined.Search,
                )
            }

            fullWidth {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                ) {
                    FilmatubeFilterChip(
                        label = stringResource(R.string.boards_tab_all),
                        selected = filter == BoardFilter.ALL,
                        onClick = { viewModel.setFilter(BoardFilter.ALL) },
                    )
                    FilmatubeFilterChip(
                        label = stringResource(R.string.boards_tab_movies),
                        selected = filter == BoardFilter.MOVIES,
                        onClick = { viewModel.setFilter(BoardFilter.MOVIES) },
                    )
                    FilmatubeFilterChip(
                        label = stringResource(R.string.boards_tab_general),
                        selected = filter == BoardFilter.GENERAL,
                        onClick = { viewModel.setFilter(BoardFilter.GENERAL) },
                    )
                }
            }

            if (boards.isEmpty()) {
                fullWidth {
                    Text(
                        stringResource(
                            if (searching) R.string.boards_no_results else R.string.boards_empty,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = FilmatubeSpacing.xxl),
                    )
                }
            } else {
                items(boards, key = { it.id }) { board ->
                    BoardCard(board = board, onClick = { onBoardClick(board.id) })
                }
            }
        }
    }
}

/** A row that spans both grid columns — headings, the hero, the search box and the filters. */
private fun LazyGridScope.fullWidth(
    content: @Composable () -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = FilmatubeSpacing.sm),
    )
}
