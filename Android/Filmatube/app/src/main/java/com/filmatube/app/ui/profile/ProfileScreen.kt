package com.filmatube.app.ui.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.filmatube.app.R
import com.filmatube.app.ui.components.FeaturePreviewScreen

/** Profile: avatar, bio, stats, followers/following, badges, settings (Day 19). */
@Composable
fun ProfileScreen() {
    FeaturePreviewScreen(
        icon = Icons.Outlined.Person,
        title = stringResource(R.string.nav_profile),
        subtitle = stringResource(R.string.profile_subtitle),
    )
}
