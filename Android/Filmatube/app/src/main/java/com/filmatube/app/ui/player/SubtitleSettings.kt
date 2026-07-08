package com.filmatube.app.ui.player

import android.graphics.Color
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.filmatube.app.R
import com.filmatube.app.domain.model.SubtitleBackground
import com.filmatube.app.domain.model.SubtitleEdge
import com.filmatube.app.domain.model.SubtitlePosition
import com.filmatube.app.domain.model.SubtitleSize
import com.filmatube.app.domain.model.SubtitleStyle
import com.filmatube.app.domain.model.SubtitleTextColor
import com.filmatube.app.ui.theme.FilmatubeSpacing

@StringRes
private fun SubtitleSize.labelRes() = when (this) {
    SubtitleSize.SMALL -> R.string.sub_small
    SubtitleSize.MEDIUM -> R.string.sub_medium
    SubtitleSize.LARGE -> R.string.sub_large
}

@StringRes
private fun SubtitleTextColor.labelRes() = when (this) {
    SubtitleTextColor.WHITE -> R.string.sub_white
    SubtitleTextColor.YELLOW -> R.string.sub_yellow
    SubtitleTextColor.CYAN -> R.string.sub_cyan
}

@StringRes
private fun SubtitleBackground.labelRes() = when (this) {
    SubtitleBackground.NONE -> R.string.sub_bg_none
    SubtitleBackground.DIM -> R.string.sub_bg_dim
    SubtitleBackground.SOLID -> R.string.sub_bg_solid
}

@StringRes
private fun SubtitleEdge.labelRes() = when (this) {
    SubtitleEdge.NONE -> R.string.sub_edge_none
    SubtitleEdge.SHADOW -> R.string.sub_edge_shadow
    SubtitleEdge.OUTLINE -> R.string.sub_edge_outline
}

@StringRes
private fun SubtitlePosition.labelRes() = when (this) {
    SubtitlePosition.LOW -> R.string.sub_pos_low
    SubtitlePosition.NORMAL -> R.string.sub_pos_normal
    SubtitlePosition.HIGH -> R.string.sub_pos_high
}

/** Applies the user's subtitle style to the player's SubtitleView. */
@OptIn(UnstableApi::class)
fun applySubtitleStyle(playerView: PlayerView, style: SubtitleStyle) {
    val subtitleView = playerView.subtitleView ?: return
    val edgeType = when (style.edge) {
        SubtitleEdge.NONE -> CaptionStyleCompat.EDGE_TYPE_NONE
        SubtitleEdge.SHADOW -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
        SubtitleEdge.OUTLINE -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
    }
    subtitleView.setStyle(
        CaptionStyleCompat(
            style.textColor.color,
            style.background.color,
            Color.TRANSPARENT,
            edgeType,
            Color.BLACK,
            null,
        ),
    )
    subtitleView.setFractionalTextSize(style.size.fraction)
    subtitleView.setBottomPaddingFraction(style.position.bottomPaddingFraction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsSheet(
    style: SubtitleStyle,
    onStyleChange: (SubtitleStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            Text(stringResource(R.string.player_subtitle_style), style = MaterialTheme.typography.titleLarge)

            OptionRow(R.string.sub_size, SubtitleSize.entries, style.size, { it.labelRes() }) {
                onStyleChange(style.copy(size = it))
            }
            OptionRow(R.string.sub_color, SubtitleTextColor.entries, style.textColor, { it.labelRes() }) {
                onStyleChange(style.copy(textColor = it))
            }
            OptionRow(R.string.sub_background, SubtitleBackground.entries, style.background, { it.labelRes() }) {
                onStyleChange(style.copy(background = it))
            }
            OptionRow(R.string.sub_edge, SubtitleEdge.entries, style.edge, { it.labelRes() }) {
                onStyleChange(style.copy(edge = it))
            }
            OptionRow(R.string.sub_position, SubtitlePosition.entries, style.position, { it.labelRes() }) {
                onStyleChange(style.copy(position = it))
            }
            Spacer(Modifier.height(FilmatubeSpacing.xl))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionRow(
    @StringRes labelRes: Int,
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(stringResource(label(option))) },
                )
            }
        }
    }
}
