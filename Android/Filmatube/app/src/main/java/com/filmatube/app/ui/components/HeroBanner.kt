package com.filmatube.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filmatube.app.ui.theme.FilmatubeGold
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.ui.theme.PosterTileWidth

private val HeroShape = RoundedCornerShape(20.dp)

/**
 * Branded hero banner — deep-green gradient with a decorative film mark and a
 * kicker / headline / tagline stack. Used as the Home header until real featured
 * content arrives (Day 30), then reused as the featured-movie backdrop card.
 */
@Composable
fun HeroBanner(
    kicker: String,
    title: String,
    tagline: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(HeroShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E7A46),
                        Color(0xFF11492B),
                        Color(0xFF0B2818),
                    ),
                ),
            ),
    ) {
        // Decorative oversized film mark bleeding off the right edge.
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 48.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(FilmatubeSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
        ) {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.35f),
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = FilmatubeGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Text(
                text = kicker.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = FilmatubeGold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Text(
                text = tagline,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD2E8D8),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Poster-shaped placeholder card used in preview rows before real catalog data
 * exists — a soft surface gradient with a centered film mark, so empty rows read
 * as intentional design rather than a loading failure.
 */
@Composable
fun PosterPlaceholder(
    modifier: Modifier = Modifier,
    rank: Int? = null,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .width(PosterTileWidth)
            .aspectRatio(2f / 3f)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ),
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), shape),
    ) {
        Icon(
            imageVector = Icons.Filled.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.Center),
        )
        if (rank != null) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 6.dp),
            )
        }
    }
}

/** Fills the placeholder box behind [PosterPlaceholder] previews if needed. */
@Composable
fun PosterPlaceholderRow(
    count: Int,
    ranked: Boolean = false,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = FilmatubeSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
    ) {
        items(count) { index ->
            PosterPlaceholder(rank = if (ranked) index + 1 else null)
        }
    }
}
