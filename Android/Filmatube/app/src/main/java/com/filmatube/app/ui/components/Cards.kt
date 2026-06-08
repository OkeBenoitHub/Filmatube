package com.filmatube.app.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Brand card surface (uses `surfaceContainer`). Pass [onClick] for a clickable card.
 */
@Composable
fun FilmatubeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors, content = content)
    } else {
        Card(modifier = modifier, colors = colors, content = content)
    }
}
