package com.filmatube.app.ui.parties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.util.LocaleController
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.theme.FilmatubeShapes
import com.filmatube.app.ui.theme.FilmatubeSpacing

/** Create a watch party for one movie: pick when it starts, then invite from the lobby. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePartyScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreatePartyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val language = LocaleController.currentTag()

    LaunchedEffect(state.createdId) {
        state.createdId?.let(onCreated)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.party_create_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(FilmatubeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.lg),
        ) {
            val movie = state.movie
            if (movie != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
                    AsyncImage(
                        model = movie.posterUrl,
                        contentDescription = movie.title.get(language),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(72.dp)
                            .aspectRatio(2f / 3f)
                            .clip(FilmatubeShapes.medium),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                        Text(movie.title.get(language), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.party_create_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(stringResource(R.string.party_when), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                StartChip(PartyStart.NOW, R.string.party_start_now, state.start, viewModel::setStart)
                StartChip(PartyStart.IN_30_MIN, R.string.party_start_30m, state.start, viewModel::setStart)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                StartChip(PartyStart.IN_1_HOUR, R.string.party_start_1h, state.start, viewModel::setStart)
                StartChip(PartyStart.IN_2_HOURS, R.string.party_start_2h, state.start, viewModel::setStart)
            }

            if (state.error) {
                Text(
                    stringResource(R.string.party_create_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            FilmatubePrimaryButton(
                text = stringResource(R.string.party_create_button),
                onClick = { viewModel.create(language) },
                enabled = state.canCreate,
                loading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StartChip(
    option: PartyStart,
    label: Int,
    selected: PartyStart,
    onSelect: (PartyStart) -> Unit,
) {
    FilterChip(
        selected = selected == option,
        onClick = { onSelect(option) },
        label = { Text(stringResource(label)) },
    )
}
