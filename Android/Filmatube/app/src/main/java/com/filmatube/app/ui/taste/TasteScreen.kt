package com.filmatube.app.ui.taste

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.ui.components.FilmatubeFilterChip
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.theme.FilmatubeSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasteScreen(
    onFinished: () -> Unit,
    viewModel: TasteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onFinished()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = FilmatubeSpacing.xl)
                .padding(top = FilmatubeSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            Text(stringResource(R.string.taste_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.taste_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // App language
            SectionLabel(stringResource(R.string.taste_section_app_language))
            Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                FilmatubeFilterChip(
                    label = stringResource(R.string.lang_english),
                    selected = state.appLanguage == "en",
                    onClick = { viewModel.setAppLanguage("en") },
                )
                FilmatubeFilterChip(
                    label = stringResource(R.string.lang_french),
                    selected = state.appLanguage == "fr",
                    onClick = { viewModel.setAppLanguage("fr") },
                )
            }

            // Preferred movie language
            SectionLabel(stringResource(R.string.taste_section_content_language))
            Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                FilmatubeFilterChip(
                    label = stringResource(R.string.content_lang_en),
                    selected = state.contentLanguage == "en",
                    onClick = { viewModel.setContentLanguage("en") },
                )
                FilmatubeFilterChip(
                    label = stringResource(R.string.content_lang_fr),
                    selected = state.contentLanguage == "fr",
                    onClick = { viewModel.setContentLanguage("fr") },
                )
                FilmatubeFilterChip(
                    label = stringResource(R.string.content_lang_both),
                    selected = state.contentLanguage == "both",
                    onClick = { viewModel.setContentLanguage("both") },
                )
            }

            // Genres
            SectionLabel(stringResource(R.string.taste_section_genres))
            Text(
                stringResource(R.string.taste_genre_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                Genre.entries.forEach { genre ->
                    FilmatubeFilterChip(
                        label = stringResource(genre.labelRes),
                        selected = genre.key in state.selectedGenres,
                        onClick = { viewModel.toggleGenre(genre.key) },
                    )
                }
            }

            Spacer(Modifier.height(FilmatubeSpacing.md))
        }

        FilmatubePrimaryButton(
            text = stringResource(R.string.taste_continue),
            onClick = viewModel::save,
            enabled = state.canContinue,
            loading = state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = FilmatubeSpacing.xl)
                .padding(bottom = FilmatubeSpacing.lg)
                .height(52.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = FilmatubeSpacing.sm),
    )
}
