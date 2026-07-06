package com.filmatube.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.ui.theme.PosterTileWidth

private val PosterShape = RoundedCornerShape(12.dp)

/**
 * Movie poster tile (2:3) with shimmer while loading and a film-icon fallback on error.
 * Used in home rows (fixed [width]) and browse/search grids (pass `width = null` to fill the cell).
 */
@Composable
fun PosterTile(
    posterUrl: String?,
    title: String?,
    modifier: Modifier = Modifier,
    width: Dp? = PosterTileWidth,
    onClick: () -> Unit = {},
) {
    val sized = if (width != null) modifier.width(width) else modifier.fillMaxWidth()
    Column(modifier = sized.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(PosterShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), PosterShape),
        ) {
            SubcomposeAsyncImage(
                model = posterUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ShimmerBox(Modifier.fillMaxSize(), shape = PosterShape) },
                error = { PosterFallback() },
            )
        }
        if (!title.isNullOrBlank()) {
            Spacer(Modifier.height(FilmatubeSpacing.xs))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PosterFallback() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
