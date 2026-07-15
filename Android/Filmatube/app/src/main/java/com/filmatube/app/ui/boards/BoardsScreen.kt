package com.filmatube.app.ui.boards

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.data.boards.Board
import com.filmatube.app.data.boards.BoardTypes
import com.filmatube.app.ui.components.FilmatubeFilterChip
import com.filmatube.app.ui.theme.FilmatubeSpacing

@Composable
fun BoardsScreen(
    onBoardClick: (String) -> Unit,
    viewModel: BoardsViewModel = hiltViewModel(),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val featured by viewModel.featured.collectAsStateWithLifecycle()
    val boards by viewModel.boards.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = FilmatubeSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.sm)) {
                Text(stringResource(R.string.boards_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.boards_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg),
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

        if (filter == BoardFilter.ALL && featured.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.boards_featured),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                ) {
                    items(featured, key = { it.id }) { board ->
                        FeaturedBoardCard(board, onClick = { onBoardClick(board.id) })
                    }
                }
            }
        }

        if (boards.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = FilmatubeSpacing.xxl),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.boards_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = FilmatubeSpacing.xl),
                    )
                }
            }
        } else {
            items(boards, key = { it.id }) { board ->
                BoardRow(board, onClick = { onBoardClick(board.id) })
            }
        }
    }
}

@Composable
private fun FeaturedBoardCard(board: Board, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.width(240.dp).clickable(onClick = onClick),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                BoardCover(board, Modifier.fillMaxSize())
                if (board.isOfficial) OfficialBadge(Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            Column(modifier = Modifier.padding(FilmatubeSpacing.md)) {
                Text(
                    board.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MemberLine(board)
            }
        }
    }
}

@Composable
private fun BoardRow(board: Board, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoardCover(board, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                Text(
                    board.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (board.isOfficial) {
                    Icon(
                        Icons.Filled.Verified,
                        contentDescription = stringResource(R.string.board_official),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (board.description.isNotBlank()) {
                Text(
                    board.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MemberLine(board)
        }
    }
}

@Composable
private fun BoardCover(board: Board, modifier: Modifier) {
    if (board.coverUrl.isNotBlank()) {
        AsyncImage(
            model = board.coverUrl,
            contentDescription = board.title,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun MemberLine(board: Board) {
    val typeLabel = stringResource(
        if (board.type == BoardTypes.MOVIE) R.string.board_type_movie else R.string.board_type_general,
    )
    Text(
        text = "$typeLabel · ${stringResource(R.string.boards_members, board.memberCount)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun OfficialBadge(modifier: Modifier) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Icon(
                Icons.Filled.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp),
            )
            Text(
                stringResource(R.string.board_official),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}
