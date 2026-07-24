package com.filmatube.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.filmatube.app.domain.model.MovieCollection
import com.filmatube.app.ui.theme.FilmatubeSpacing

// On-brand duotones for coverless collections — deterministic by title, so the same collection
// always looks the same and a shelf of them reads as a varied set. Mirrors the web CollectionCover.
private val GRADIENTS = listOf(
    listOf(Color(0xFF22C55E), Color(0xFF166534)),
    listOf(Color(0xFF10B981), Color(0xFF115E59)),
    listOf(Color(0xFF14B8A6), Color(0xFF15803D)),
    listOf(Color(0xFF16A34A), Color(0xFF064E3B)),
    listOf(Color(0xFF65A30D), Color(0xFF166534)),
    listOf(Color(0xFF059669), Color(0xFF14532D)),
)

private fun gradientFor(seed: String): List<Color> {
    var h = 0
    for (c in seed) h = h * 31 + c.code
    return GRADIENTS[(h % GRADIENTS.size + GRADIENTS.size) % GRADIENTS.size]
}

/** A 16:9 collection card: cover image, or a branded gradient placeholder with the title monogram. */
@Composable
fun CollectionCoverCard(
    collection: MovieCollection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp? = 176.dp,
) {
    Column(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (collection.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = collection.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(gradientFor(collection.title))),
                    contentAlignment = Alignment.Center,
                ) {
                    val initial = collection.title.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
                    if (initial.isNotEmpty()) {
                        Text(
                            initial,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.18f),
                        )
                    }
                    Icon(
                        Icons.Outlined.Layers,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.aspectRatio(1f).padding(FilmatubeSpacing.xl),
                    )
                }
            }
        }
        Text(
            collection.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = FilmatubeSpacing.xs),
        )
    }
}
