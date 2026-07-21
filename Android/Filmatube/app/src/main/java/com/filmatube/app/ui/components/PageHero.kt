package com.filmatube.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmatube.app.ui.theme.FilmatubeBrandGreen
import com.filmatube.app.ui.theme.FilmatubeBrandGreenDeep
import com.filmatube.app.ui.theme.FilmatubeSpacing

/**
 * Kept modest on purpose: the header should introduce the screen, not consume it. At 92dp
 * with a 28sp title the hero ate roughly a third of a phone viewport before any content.
 */
private val TileSize = 68.dp
private val TileShape = RoundedCornerShape(18.dp)

/**
 * The section header used at the top of Search, Social and Profile — the Android counterpart
 * of the web's `PageHero`, so the two clients read as one product.
 *
 * Structure mirrors the web: a green gradient tile, a small uppercase eyebrow, an oversized
 * title, and a muted subtitle. It's laid out as a row rather than the web's stacked-and-
 * centred mobile variant, because a phone's width is the scarce dimension — a centred column
 * would push the actual content of the screen below the fold.
 *
 * Pass [tile] instead of [icon] when the header should show real content rather than a glyph
 * (Profile uses the user's avatar there, matching the web account page).
 */
@Composable
fun PageHero(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tile: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    /**
     * Set to zero when the hero sits inside a container that already insets it — a grid's
     * `contentPadding`, say — otherwise the two stack and the header drifts inward.
     */
    horizontalPadding: Dp = FilmatubeSpacing.lg,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = FilmatubeSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tile != null) {
            tile()
        } else if (icon != null) {
            Box(
                modifier = Modifier
                    .size(TileSize)
                    .clip(TileShape)
                    .background(Brush.linearGradient(listOf(FilmatubeBrandGreen, FilmatubeBrandGreenDeep))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                // Wide tracking is what makes the eyebrow read as a kicker rather than a
                // second line of body copy — the same trick the web header uses.
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        trailing?.invoke()
    }
}
