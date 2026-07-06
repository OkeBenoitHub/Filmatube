package com.filmatube.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmatube.app.R
import com.filmatube.app.ui.theme.FilmatubeBrandGreen
import com.filmatube.app.ui.theme.FilmatubeBrandGreenDeep

/**
 * The Filmatube app-icon logo: a green gradient rounded tile with the white
 * film-strip + play mark. Mirrors the launcher icon so in-app branding matches home screen.
 */
@Composable
fun FilmatubeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(percent = 24))
            .background(
                Brush.linearGradient(
                    colors = listOf(FilmatubeBrandGreen, FilmatubeBrandGreenDeep),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_filmatube_mark),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.fillMaxSize(0.62f),
        )
    }
}
