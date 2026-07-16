package com.filmatube.app.ui.landing

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.filmatube.app.R
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeSecondaryButton
import com.filmatube.app.ui.theme.FilmatubeGold
import com.filmatube.app.ui.theme.FilmatubeGreen
import com.filmatube.app.ui.theme.FilmatubeSpacing

private data class Feature(val icon: ImageVector, val title: Int, val desc: Int)
private data class Faq(val q: Int, val a: Int)
private data class Stat(val value: Int, val label: Int)

/**
 * Landing screen — mirrors the web marketing landing (hero, stats, features, how it works,
 * player, social, FAQ) in the Filmatube green theme.
 *
 * Purely presentational; the CTAs and back arrow are injected because it serves two contexts:
 *  - **Signed out:** the app's entry screen (every open) — Landing → Get started → Login,
 *    with the carousel skipped after the first run. No back arrow.
 *  - **Signed in:** Settings → About, with a back arrow and in-app CTAs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    primaryLabel: Int,
    onPrimary: () -> Unit,
    secondaryLabel: Int,
    onSecondary: () -> Unit,
    /** Null on the entry screen — renders no navigation icon. */
    onBack: (() -> Unit)? = null,
) {
    val features = listOf(
        Feature(Icons.Filled.Movie, R.string.landing_feat_watch_title, R.string.landing_feat_watch_desc),
        Feature(Icons.Filled.Groups, R.string.landing_feat_social_title, R.string.landing_feat_social_desc),
        Feature(Icons.Filled.Theaters, R.string.landing_feat_theater_title, R.string.landing_feat_theater_desc),
        Feature(Icons.AutoMirrored.Filled.Chat, R.string.landing_feat_boards_title, R.string.landing_feat_boards_desc),
        Feature(Icons.Filled.Download, R.string.landing_feat_downloads_title, R.string.landing_feat_downloads_desc),
        Feature(Icons.Filled.AutoAwesome, R.string.landing_feat_discover_title, R.string.landing_feat_discover_desc),
    )
    val stats = listOf(
        Stat(R.string.landing_stat_movies_value, R.string.landing_stat_movies_label),
        Stat(R.string.landing_stat_members_value, R.string.landing_stat_members_label),
        Stat(R.string.landing_stat_reviews_value, R.string.landing_stat_reviews_label),
        Stat(R.string.landing_stat_countries_value, R.string.landing_stat_countries_label),
    )
    val faqs = listOf(
        Faq(R.string.landing_faq_q1, R.string.landing_faq_a1),
        Faq(R.string.landing_faq_q2, R.string.landing_faq_a2),
        Faq(R.string.landing_faq_q3, R.string.landing_faq_a3),
        Faq(R.string.landing_faq_q4, R.string.landing_faq_a4),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.landing_topbar),
                        fontWeight = FontWeight.ExtraBold,
                        color = FilmatubeGreen,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.detail_back),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(bottom = FilmatubeSpacing.xxl),
        ) {
            // ── Hero ──────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(FilmatubeGreen.copy(alpha = 0.12f), Color.Transparent),
                            ),
                        )
                        .padding(horizontal = FilmatubeSpacing.xl, vertical = FilmatubeSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Pill(stringResource(R.string.landing_badge))
                    Text(
                        stringResource(R.string.landing_title),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.displaySmall.lineHeight,
                        modifier = Modifier.padding(top = FilmatubeSpacing.lg),
                    )
                    Text(
                        stringResource(R.string.landing_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = FilmatubeSpacing.md, bottom = FilmatubeSpacing.sm),
                    )

                    // Poster strip — centre tiles raised, like the web hero.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = FilmatubeSpacing.xl),
                    ) {
                        repeat(4) { i ->
                            val centre = i == 1 || i == 2
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .offset(y = if (centre) (-10).dp else 0.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(
                                        width = 1.dp,
                                        color = if (centre) FilmatubeGreen.copy(alpha = 0.55f)
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(14.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Movie,
                                    contentDescription = null,
                                    tint = if (centre) FilmatubeGreen.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }

                    FilmatubePrimaryButton(
                        text = stringResource(primaryLabel),
                        onClick = onPrimary,
                        leadingIcon = Icons.Filled.PlayArrow,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FilmatubeSecondaryButton(
                        text = stringResource(secondaryLabel),
                        onClick = onSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = FilmatubeSpacing.sm),
                    )
                }
            }

            // ── Stats band ────────────────────────────────────────
            item {
                Card {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        stats.take(2).forEach { StatCell(it, Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(FilmatubeSpacing.lg))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        stats.drop(2).forEach { StatCell(it, Modifier.weight(1f)) }
                    }
                }
            }

            // ── Features (cards, 2 per row) ───────────────────────
            item {
                SectionHeader(
                    stringResource(R.string.landing_features_kicker),
                    stringResource(R.string.landing_features_title),
                )
            }
            items(features.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                ) {
                    rowItems.forEach { f -> FeatureCard(f, Modifier.weight(1f)) }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // ── How it works ──────────────────────────────────────
            item {
                SectionHeader(
                    stringResource(R.string.landing_how_kicker),
                    stringResource(R.string.landing_how_title),
                )
                Card {
                    Step(1, R.string.landing_step1_title, R.string.landing_step1_desc)
                    Step(2, R.string.landing_step2_title, R.string.landing_step2_desc)
                    Step(3, R.string.landing_step3_title, R.string.landing_step3_desc, last = true)
                }
            }

            // ── Player features (chips) ───────────────────────────
            item {
                SectionHeader(
                    stringResource(R.string.landing_player_kicker),
                    stringResource(R.string.landing_player_title),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                    contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
                ) {
                    val chips = listOf(
                        Icons.Filled.ClosedCaption to R.string.landing_player_subtitles,
                        Icons.Filled.Language to R.string.landing_player_audio,
                        Icons.Filled.Bolt to R.string.landing_player_speed,
                        Icons.Filled.Replay to R.string.landing_player_resume,
                        Icons.Filled.PictureInPicture to R.string.landing_player_pip,
                        Icons.Filled.Download to R.string.landing_player_offline,
                    )
                    items(chips) { (icon, label) -> FeatureChip(icon, stringResource(label)) }
                }
            }

            // ── Social spotlight ──────────────────────────────────
            item {
                SectionHeader(
                    stringResource(R.string.landing_social_kicker),
                    stringResource(R.string.landing_social_title),
                )
                Card {
                    SocialPoint(Icons.Filled.Favorite, R.string.landing_social_react)
                    SocialPoint(Icons.Filled.Send, R.string.landing_social_recommend)
                    SocialPoint(Icons.Filled.RssFeed, R.string.landing_social_feed)
                }
            }

            // ── FAQ ───────────────────────────────────────────────
            item {
                SectionHeader(
                    stringResource(R.string.landing_faq_kicker),
                    stringResource(R.string.landing_faq_title),
                )
            }
            items(faqs) { faq -> FaqRow(stringResource(faq.q), stringResource(faq.a)) }

            item {
                Text(
                    stringResource(R.string.landing_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FilmatubeSpacing.xl),
                )
            }
        }
    }
}

/* ── building blocks ──────────────────────────────────────────────────────── */

@Composable
private fun Pill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = FilmatubeGreen.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, FilmatubeGreen.copy(alpha = 0.35f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
            modifier = Modifier.padding(horizontal = FilmatubeSpacing.md, vertical = 6.dp),
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = FilmatubeGreen,
                modifier = Modifier.size(14.dp),
            )
            Text(text, style = MaterialTheme.typography.labelMedium, color = FilmatubeGreen)
        }
    }
}

@Composable
private fun SectionHeader(kicker: String, title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = FilmatubeSpacing.lg,
                end = FilmatubeSpacing.lg,
                top = FilmatubeSpacing.xxl,
                bottom = FilmatubeSpacing.md,
            ),
    ) {
        Text(
            kicker.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = FilmatubeGold,
            fontWeight = FontWeight.Bold,
        )
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** Shared card container — subtle border + container tone, matching the web card language. */
@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FilmatubeSpacing.lg),
    ) {
        Column(modifier = Modifier.padding(FilmatubeSpacing.lg)) { content() }
    }
}

@Composable
private fun StatCell(stat: Stat, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(stat.value),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = FilmatubeGreen,
        )
        Text(
            stringResource(stat.label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FeatureCard(f: Feature, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(FilmatubeSpacing.lg)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FilmatubeGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(f.icon, contentDescription = null, tint = FilmatubeGreen, modifier = Modifier.size(20.dp))
            }
            Text(
                stringResource(f.title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = FilmatubeSpacing.md),
            )
            Text(
                stringResource(f.desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = FilmatubeSpacing.xs),
            )
        }
    }
}

@Composable
private fun Step(n: Int, title: Int, desc: Int, last: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(FilmatubeGreen),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$n",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            if (!last) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(FilmatubeGreen.copy(alpha = 0.25f)),
                )
            }
        }
        Column(modifier = Modifier.padding(bottom = if (last) 0.dp else FilmatubeSpacing.md)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                stringResource(desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SocialPoint(icon: ImageVector, label: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FilmatubeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = FilmatubeGreen, modifier = Modifier.size(20.dp))
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FeatureChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
            modifier = Modifier.padding(horizontal = FilmatubeSpacing.md, vertical = FilmatubeSpacing.sm),
        ) {
            Icon(icon, contentDescription = null, tint = FilmatubeGreen, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FaqRow(q: String, a: String) {
    var open by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.xs),
    ) {
        Column(
            modifier = Modifier
                .clickable { open = !open }
                .padding(FilmatubeSpacing.lg)
                .animateContentSize(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    q,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (open) Icons.Filled.Remove else Icons.Filled.Add,
                    contentDescription = null,
                    tint = FilmatubeGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (open) {
                Text(
                    a,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = FilmatubeSpacing.sm),
                )
            }
        }
    }
}
