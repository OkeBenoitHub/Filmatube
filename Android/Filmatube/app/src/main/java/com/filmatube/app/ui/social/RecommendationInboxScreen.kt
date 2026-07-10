package com.filmatube.app.ui.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeSpacing

@Composable
fun RecommendationInboxScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    viewModel: RecommendationInboxViewModel = hiltViewModel(),
) {
    val inbox by viewModel.inbox.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.sm, vertical = FilmatubeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.settings_back))
            }
            Text(stringResource(R.string.inbox_title), style = MaterialTheme.typography.titleLarge)
        }

        if (inbox.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.inbox_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(FilmatubeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
            ) {
                items(inbox, key = { it.id }) { rec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (rec.movieId.isNotBlank()) onMovieClick(rec.movieId) },
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UserAvatar(url = rec.fromAvatar, name = rec.fromName, size = 40.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.recommend_from, rec.fromName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                rec.movieTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (rec.message.isNotBlank()) {
                                Text(
                                    rec.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
