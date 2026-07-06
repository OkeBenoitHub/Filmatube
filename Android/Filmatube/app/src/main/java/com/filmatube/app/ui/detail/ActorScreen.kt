package com.filmatube.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.domain.util.DataState
import com.filmatube.app.ui.components.EmptyView
import com.filmatube.app.ui.components.ErrorView
import com.filmatube.app.ui.components.LoadingView
import com.filmatube.app.ui.components.PosterTile
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.util.LocaleController

@Composable
fun ActorScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    viewModel: ActorViewModel = hiltViewModel(),
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
            Text(viewModel.actorName, style = MaterialTheme.typography.titleLarge)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (val s = state) {
                DataState.Loading -> LoadingView()
                DataState.Empty -> EmptyView()
                is DataState.Error -> ErrorView(error = s.error, onRetry = viewModel::load)
                is DataState.Success -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(FilmatubeSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                ) {
                    items(s.data, key = { it.id }) { movie ->
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
