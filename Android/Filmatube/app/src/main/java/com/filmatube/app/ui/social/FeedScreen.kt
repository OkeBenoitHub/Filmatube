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
import com.filmatube.app.data.social.FeedEventTypes
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeSpacing

@Composable
fun FeedScreen(
    onMovieClick: (String) -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.feed_title), style = MaterialTheme.typography.titleLarge)
        }

        if (feed.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.feed_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = FilmatubeSpacing.xl),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(FilmatubeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
            ) {
                items(feed, key = { it.id }) { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (event.movieId.isNotBlank()) onMovieClick(event.movieId) },
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        UserAvatar(url = event.actorAvatar, name = event.actorName, size = 40.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${event.actorName} ${actionText(event.type)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                event.movieTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun actionText(type: String): String = stringResource(
    when (type) {
        FeedEventTypes.WATCHING -> R.string.feed_watching
        FeedEventTypes.WATCHED -> R.string.feed_watched
        FeedEventTypes.ADDED_WATCHLIST -> R.string.feed_added_watchlist
        FeedEventTypes.LIKED -> R.string.feed_liked
        FeedEventTypes.REACTED -> R.string.feed_reacted
        FeedEventTypes.ADDED_COLLECTION -> R.string.feed_added_collection
        else -> R.string.feed_watched
    },
)
