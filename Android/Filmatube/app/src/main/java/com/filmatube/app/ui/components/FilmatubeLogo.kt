package com.filmatube.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.filmatube.app.R

/**
 * The Filmatube app-icon logo (the exact brand logo). Mirrors the launcher icon so
 * in-app branding matches the home screen.
 */
@Composable
fun FilmatubeLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    Image(
        painter = painterResource(R.drawable.ic_filmatube_logo),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
