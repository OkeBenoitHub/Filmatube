package com.filmatube.app.ui.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.filmatube.app.R
import com.filmatube.app.ui.components.FeaturePreviewScreen

/** Search: debounced search with genre/year/rating filters (Day 34). */
@Composable
fun SearchScreen() {
    FeaturePreviewScreen(
        icon = Icons.Outlined.Search,
        title = stringResource(R.string.nav_search),
        subtitle = stringResource(R.string.search_subtitle),
    )
}
