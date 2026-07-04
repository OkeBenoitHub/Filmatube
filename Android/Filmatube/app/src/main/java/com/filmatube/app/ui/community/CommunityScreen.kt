package com.filmatube.app.ui.community

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.filmatube.app.R
import com.filmatube.app.ui.components.FeaturePreviewScreen

/** Community: discussion boards, real-time chat, watch parties (v1.1). */
@Composable
fun CommunityScreen() {
    FeaturePreviewScreen(
        icon = Icons.Outlined.Groups,
        title = stringResource(R.string.nav_community),
        subtitle = stringResource(R.string.community_subtitle),
    )
}
