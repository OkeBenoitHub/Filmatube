package com.filmatube.app.ui.theater

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.filmatube.app.R
import com.filmatube.app.ui.components.FeaturePreviewScreen

/** Online Movie Theater: upcoming showtimes & premieres, synced playback (v1.2). */
@Composable
fun TheaterScreen() {
    FeaturePreviewScreen(
        icon = Icons.Outlined.Theaters,
        title = stringResource(R.string.nav_theater),
        subtitle = stringResource(R.string.theater_subtitle),
    )
}
