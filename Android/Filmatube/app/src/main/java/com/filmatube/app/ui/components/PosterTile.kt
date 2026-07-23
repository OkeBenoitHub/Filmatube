package com.filmatube.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.filmatube.app.R
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.ui.theme.PosterTileWidth

private val PosterShape = RoundedCornerShape(12.dp)

/**
 * Movie poster tile (2:3) with shimmer while loading and a film-icon fallback on error.
 * Used in home rows (fixed [width]) and browse/search grids (pass `width = null` to fill the cell).
 *
 * When [onMoreClick] is supplied the tile grows a ⋮ button and a long press, both opening the
 * movie's options. Long press is the discoverable-by-habit gesture; the visible button is what
 * makes the gesture guessable at all — neither alone is enough.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PosterTile(
    posterUrl: String?,
    title: String?,
    modifier: Modifier = Modifier,
    width: Dp? = PosterTileWidth,
    onClick: () -> Unit = {},
    onMoreClick: (() -> Unit)? = null,
) {
    val sized = if (width != null) modifier.width(width) else modifier.fillMaxWidth()
    val interactive = if (onMoreClick != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onMoreClick)
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Column(modifier = sized.then(interactive)) {
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

            if (onMoreClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        // Scrimmed circle, not a bare icon: posters are arbitrary artwork and
                        // a plain glyph disappears against a bright one.
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onMoreClick)
                        .padding(3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.movie_options_title),
                        tint = Color.White,
                        // 20dp in a 26dp scrim, nudged up from 18-in-22: the old button read
                        // smaller than the web's and was fiddly on a dense grid. Kept well
                        // under the poster's width so it stays an affordance, not a badge.
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
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
