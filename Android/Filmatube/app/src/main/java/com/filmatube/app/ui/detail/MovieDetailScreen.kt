package com.filmatube.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.filmatube.app.ui.social.RecommendDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.domain.model.CastMember
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.util.DataState
import com.filmatube.app.ui.components.ContentRow
import com.filmatube.app.ui.components.EmptyView
import com.filmatube.app.ui.components.ErrorView
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeSecondaryButton
import com.filmatube.app.ui.components.LoadingView
import com.filmatube.app.ui.components.PosterTile
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.taste.genreLabel
import com.filmatube.app.ui.theme.FilmatubeGold
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.util.LocaleController

@Composable
fun MovieDetailScreen(
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onActorClick: (String) -> Unit,
    onOpenReviews: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    viewModel: MovieDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reminderSet by viewModel.reminderSet.collectAsStateWithLifecycle()
    val savedForLater by viewModel.savedForLater.collectAsStateWithLifecycle()
    val myReaction by viewModel.myReaction.collectAsStateWithLifecycle()
    val reactionCounts by viewModel.reactionCounts.collectAsStateWithLifecycle()
    val myRating by viewModel.myRating.collectAsStateWithLifecycle()
    val ratingAggregate by viewModel.ratingAggregate.collectAsStateWithLifecycle()
    val recipients by viewModel.recipients.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val language = LocaleController.currentTag()
    val context = LocalContext.current
    var showRecommend by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val movie = state.movie) {
            DataState.Loading -> LoadingView()
            DataState.Empty -> EmptyView()
            is DataState.Error -> ErrorView(error = movie.error, onRetry = viewModel::load)
            is DataState.Success -> DetailContent(
                movie = movie.data,
                related = state.related,
                language = language,
                reminderSet = reminderSet,
                savedForLater = savedForLater,
                onToggleSaved = viewModel::toggleSaved,
                myReaction = myReaction,
                reactionCounts = reactionCounts,
                onReact = viewModel::setReaction,
                myRating = myRating,
                ratingAverage = ratingAggregate.average,
                ratingCount = ratingAggregate.count,
                onRate = viewModel::setRating,
                onRecommend = { viewModel.loadRecipients(); showRecommend = true },
                onOpenReviews = onOpenReviews,
                onOpenComments = onOpenComments,
                downloadState = downloadState,
                onToggleDownload = viewModel::toggleDownload,
                onPlay = onPlay,
                onToggleReminder = viewModel::toggleReminder,
                onOpenTrailer = { url ->
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                },
                onMovieClick = onMovieClick,
                onActorClick = onActorClick,
            )
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

    if (showRecommend) {
        RecommendDialog(
            recipients = recipients,
            onSend = { uid, message ->
                viewModel.recommend(uid, message)
                showRecommend = false
            },
            onDismiss = { showRecommend = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    movie: Movie,
    related: List<Movie>,
    language: String,
    reminderSet: Boolean,
    savedForLater: Boolean,
    onToggleSaved: () -> Unit,
    myReaction: String?,
    reactionCounts: Map<String, Int>,
    onReact: (String) -> Unit,
    myRating: Int?,
    ratingAverage: Double,
    ratingCount: Int,
    onRate: (Int) -> Unit,
    onRecommend: () -> Unit,
    onOpenReviews: (String) -> Unit,
    onOpenComments: (String) -> Unit,
    downloadState: DownloadUiState,
    onToggleDownload: () -> Unit,
    onPlay: (String) -> Unit,
    onToggleReminder: () -> Unit,
    onOpenTrailer: (String) -> Unit,
    onMovieClick: (String) -> Unit,
    onActorClick: (String) -> Unit,
) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title.get(language),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(96.dp)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(10.dp)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                    Text(movie.title.get(language), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = buildString {
                            append(movie.year)
                            if (movie.duration > 0) append("  •  ${movie.duration} min")
                            if (movie.ageRating.isNotBlank()) append("  •  ${movie.ageRating}")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val displayAverage = if (ratingCount > 0) ratingAverage else movie.averageRating
                    val displayCount = if (ratingCount > 0) ratingCount.toLong() else movie.ratingsCount
                    if (displayAverage > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = FilmatubeGold, modifier = Modifier.width(18.dp))
                            Text(
                                text = "  ${"%.1f".format(displayAverage)}  ($displayCount)",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    if (movie.directors.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.detail_directed_by, movie.directors.joinToString(", ")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (movie.isComingSoon) {
                FilmatubePrimaryButton(
                    text = stringResource(if (reminderSet) R.string.detail_reminder_set else R.string.detail_remind),
                    onClick = onToggleReminder,
                    leadingIcon = if (reminderSet) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                FilmatubePrimaryButton(
                    text = stringResource(R.string.detail_play),
                    onClick = { onPlay(movie.id) },
                    leadingIcon = Icons.Filled.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!movie.trailerUrl.isNullOrBlank()) {
                FilmatubeSecondaryButton(
                    text = stringResource(R.string.detail_trailer),
                    onClick = { onOpenTrailer(movie.trailerUrl) },
                    leadingIcon = Icons.Outlined.PlayCircle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!movie.isComingSoon) {
                FilmatubeSecondaryButton(
                    text = stringResource(
                        when (downloadState) {
                            DownloadUiState.DOWNLOADED -> R.string.detail_downloaded
                            DownloadUiState.DOWNLOADING -> R.string.detail_downloading
                            DownloadUiState.NONE -> R.string.detail_download
                        },
                    ),
                    onClick = onToggleDownload,
                    leadingIcon = when (downloadState) {
                        DownloadUiState.DOWNLOADED -> Icons.Filled.DownloadDone
                        DownloadUiState.DOWNLOADING -> Icons.Filled.Downloading
                        DownloadUiState.NONE -> Icons.Filled.Download
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FilmatubeSecondaryButton(
                text = stringResource(if (savedForLater) R.string.detail_saved else R.string.detail_watch_later),
                onClick = onToggleSaved,
                leadingIcon = if (savedForLater) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!movie.isComingSoon) {
                RatingBar(myRating = myRating, onRate = onRate)
            }

            ReactionBar(myReaction = myReaction, counts = reactionCounts, onReact = onReact)

            FilmatubeSecondaryButton(
                text = stringResource(R.string.detail_recommend),
                onClick = onRecommend,
                leadingIcon = Icons.Filled.Send,
                modifier = Modifier.fillMaxWidth(),
            )

            FilmatubeSecondaryButton(
                text = stringResource(R.string.detail_reviews),
                onClick = { onOpenReviews(movie.id) },
                leadingIcon = Icons.AutoMirrored.Filled.Comment,
                modifier = Modifier.fillMaxWidth(),
            )

            FilmatubeSecondaryButton(
                text = stringResource(R.string.detail_comments),
                onClick = { onOpenComments(movie.id) },
                leadingIcon = Icons.AutoMirrored.Filled.Chat,
                modifier = Modifier.fillMaxWidth(),
            )

            if (movie.genres.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                    movie.genres.forEach { genre ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Text(
                                text = genreLabel(genre),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            Text(
                text = movie.description.get(language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (movie.cast.isNotEmpty()) {
            CastSection(cast = movie.cast, onActorClick = onActorClick)
        }
        if (related.isNotEmpty()) {
            ContentRow(
                title = stringResource(R.string.detail_more_like_this),
                items = related,
                key = { it.id },
            ) { related ->
                PosterTile(
                    posterUrl = related.posterUrl,
                    title = related.title.get(language),
                    onClick = { onMovieClick(related.id) },
                )
            }
        }
        Spacer(Modifier.height(FilmatubeSpacing.xxl))
    }
}

@Composable
private fun RatingBar(myRating: Int?, onRate: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
        Text(
            text = stringResource(if (myRating != null) R.string.detail_your_rating else R.string.detail_rate_this),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
            (1..5).forEach { star ->
                val filled = myRating != null && star <= myRating
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = stringResource(R.string.detail_rate_star, star),
                    tint = if (filled) FilmatubeGold else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .width(32.dp)
                        .clickable { onRate(star) },
                )
            }
        }
    }
}

@Composable
private fun ReactionBar(myReaction: String?, counts: Map<String, Int>, onReact: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
        com.filmatube.app.data.social.ReactionType.entries.forEach { reaction ->
            val selected = myReaction == reaction.value
            val count = counts[reaction.value] ?: 0
            Surface(
                shape = RoundedCornerShape(50),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.clickable { onReact(reaction.value) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(reaction.emoji)
                    if (count > 0) {
                        Text(
                            "  $count",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CastSection(cast: List<CastMember>, onActorClick: (String) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.detail_cast),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.sm),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            items(cast, key = { it.name + it.character }) { member ->
                Column(
                    modifier = Modifier
                        .width(84.dp)
                        .clickable { onActorClick(member.name) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
                ) {
                    UserAvatar(url = member.photoUrl, name = member.name, size = 64.dp)
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                    )
                    if (member.character.isNotBlank()) {
                        Text(
                            text = member.character,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
