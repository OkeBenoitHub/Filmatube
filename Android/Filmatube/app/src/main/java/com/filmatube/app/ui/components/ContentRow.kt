package com.filmatube.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmatube.app.ui.theme.FilmatubeSpacing

/**
 * A titled horizontally-scrolling row of content (e.g. Trending, New Releases).
 * Generic over item type so it works for movies now and other content types later.
 */
@Composable
fun <T> ContentRow(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit,
) {
    Column(modifier = modifier) {
        SectionHeader(title = title, onAction = onSeeAll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            items(items = items, key = key) { item -> itemContent(item) }
        }
    }
}

/** Loading placeholder for a [ContentRow]. */
@Composable
fun ContentRowShimmer(
    modifier: Modifier = Modifier,
    itemCount: Int = 5,
) {
    Column(modifier = modifier) {
        ShimmerBox(
            modifier = Modifier
                .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.sm)
                .width(160.dp)
                .height(24.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            items(count = itemCount) { PosterTileShimmer() }
        }
    }
}
