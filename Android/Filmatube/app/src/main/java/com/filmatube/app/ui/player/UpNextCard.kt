package com.filmatube.app.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.util.LocaleController

/** "Up Next" card shown near the end of a movie: recommended title + countdown to autoplay. */
@Composable
fun UpNextCard(
    movie: Movie,
    secondsLeft: Int,
    onPlayNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocaleController.currentTag()
    Surface(
        modifier = modifier.width(320.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.82f),
    ) {
        Row(
            modifier = Modifier.padding(FilmatubeSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(56.dp)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                Text(
                    stringResource(R.string.player_up_next),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    movie.title.get(language),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.player_playing_in, secondsLeft),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onPlayNow) { Text(stringResource(R.string.player_play_now)) }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.player_dismiss), color = Color.White)
                    }
                }
            }
        }
    }
}
