package com.filmatube.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.util.DataState
import com.filmatube.app.ui.components.EmptyView
import com.filmatube.app.ui.components.ErrorView
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.LoadingView
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.util.LocaleController

@Composable
fun MovieDetailScreen(
    onBack: () -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = LocaleController.currentTag()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
            DataState.Loading -> LoadingView()
            DataState.Empty -> EmptyView()
            is DataState.Error -> ErrorView(error = s.error, onRetry = viewModel::load)
            is DataState.Success -> DetailContent(movie = s.data, language = language)
        }
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(FilmatubeSpacing.sm)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.detail_back),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun DetailContent(movie: Movie, language: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            AsyncImage(
                model = movie.backdropUrl.ifBlank { movie.posterUrl },
                contentDescription = movie.title.get(language),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier.padding(FilmatubeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            Text(movie.title.get(language), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = buildString {
                    append(movie.year)
                    if (movie.duration > 0) append("  •  ${movie.duration} min")
                    if (movie.ageRating.isNotBlank()) append("  •  ${movie.ageRating}")
                    if (movie.averageRating > 0) append("  •  ★ ${"%.1f".format(movie.averageRating)}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilmatubePrimaryButton(
                text = stringResource(R.string.detail_play),
                onClick = { /* player wired on Day 43 */ },
                leadingIcon = Icons.Filled.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = movie.description.get(language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
