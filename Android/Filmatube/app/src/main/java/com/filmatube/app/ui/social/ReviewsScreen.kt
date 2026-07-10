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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.data.social.Review
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeTextField
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: ReviewsViewModel = hiltViewModel(),
) {
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val myReview by viewModel.myReview.collectAsStateWithLifecycle()
    val revealed = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reviews_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(FilmatubeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            item {
                ReviewEditor(
                    existing = myReview,
                    onSubmit = viewModel::submit,
                    onDelete = viewModel::delete,
                )
            }

            if (reviews.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = FilmatubeSpacing.xl), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.reviews_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(reviews, key = { it.id }) { review ->
                    ReviewItem(
                        review = review,
                        revealed = revealed[review.id] == true,
                        onReveal = { revealed[review.id] = true },
                        onToggleLike = { viewModel.toggleLike(review) },
                        onUserClick = { if (review.userId.isNotBlank()) onUserClick(review.userId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewEditor(
    existing: Review?,
    onSubmit: (String, Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var text by remember(existing?.id) { mutableStateOf(existing?.text ?: "") }
    var spoiler by remember(existing?.id) { mutableStateOf(existing?.hasSpoiler ?: false) }

    Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
        Text(
            stringResource(if (existing != null) R.string.reviews_edit_yours else R.string.reviews_write),
            style = MaterialTheme.typography.titleMedium,
        )
        FilmatubeTextField(
            value = text,
            onValueChange = { text = it },
            label = stringResource(R.string.reviews_hint),
            singleLine = false,
            minLines = 3,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = spoiler, onCheckedChange = { spoiler = it })
            Text(
                stringResource(R.string.reviews_spoiler_toggle),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = FilmatubeSpacing.sm),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
            FilmatubePrimaryButton(
                text = stringResource(if (existing != null) R.string.reviews_update else R.string.reviews_post),
                onClick = { onSubmit(text, spoiler) },
                enabled = text.isNotBlank(),
                modifier = Modifier.weight(1f),
            )
            if (existing != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.reviews_delete))
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(
    review: Review,
    revealed: Boolean,
    onReveal: () -> Unit,
    onToggleLike: () -> Unit,
    onUserClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(FilmatubeSpacing.md), verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                UserAvatar(
                    url = review.userAvatar,
                    name = review.userName,
                    size = 36.dp,
                    modifier = Modifier.clickable { onUserClick() },
                )
                Text(
                    review.userName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (review.isMine) {
                    Text(
                        stringResource(R.string.reviews_you),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (review.hasSpoiler && !revealed) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth().clickable { onReveal() },
                ) {
                    Text(
                        stringResource(R.string.reviews_spoiler_hidden),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(FilmatubeSpacing.md),
                    )
                }
            } else {
                Text(review.text, style = MaterialTheme.typography.bodyMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onToggleLike) {
                    Icon(
                        Icons.Filled.ThumbUp,
                        contentDescription = stringResource(R.string.reviews_like),
                        tint = if (review.likedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(18.dp),
                    )
                    Text(
                        text = if (review.likeCount > 0) "  ${review.likeCount}" else "  ",
                        color = if (review.likedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
