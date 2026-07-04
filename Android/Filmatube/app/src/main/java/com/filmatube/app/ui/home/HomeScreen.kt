package com.filmatube.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.filmatube.app.R
import com.filmatube.app.ui.components.HeroBanner
import com.filmatube.app.ui.components.PosterPlaceholderRow
import com.filmatube.app.ui.components.SectionHeader
import com.filmatube.app.ui.theme.FilmatubeSpacing

/**
 * Home — branded preview layout: wordmark top bar, gradient hero, and ranked/plain
 * poster rows. Real featured/trending/new-release data replaces the placeholders on Day 30.
 */
@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        HomeTopBar()

        HeroBanner(
            kicker = stringResource(R.string.home_hero_kicker),
            title = stringResource(R.string.home_hero_title),
            tagline = stringResource(R.string.home_hero_tagline),
            badge = stringResource(R.string.coming_soon),
            modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg),
        )

        Spacer(Modifier.height(FilmatubeSpacing.lg))

        SectionHeader(title = stringResource(R.string.row_trending))
        PosterPlaceholderRow(count = 6, ranked = true)

        Spacer(Modifier.height(FilmatubeSpacing.lg))

        SectionHeader(title = stringResource(R.string.row_new_releases))
        PosterPlaceholderRow(count = 6)

        Spacer(Modifier.height(FilmatubeSpacing.lg))

        SectionHeader(title = stringResource(R.string.row_top_rated))
        PosterPlaceholderRow(count = 6)

        Spacer(Modifier.height(FilmatubeSpacing.xxl))
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
            TopBarIcon(
                icon = Icons.Outlined.Notifications,
                contentDescription = stringResource(R.string.notifications),
            )
            TopBarIcon(
                icon = Icons.Outlined.Person,
                contentDescription = stringResource(R.string.nav_profile),
            )
        }
    }
}

@Composable
private fun TopBarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
