package com.filmatube.app.ui.landing

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

/**
 * In-app landing / "about" screen — mirrors the web marketing landing (hero, features, how it
 * works, social spotlight, player, stats, FAQ) in the Filmatube green theme. Only reachable
 * from inside the signed-in app (Profile → About), so it's authenticated by construction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    onBack: () -> Unit,
    onBrowse: () -> Unit,
    onCommunity: () -> Unit,
) {
    val features = listOf(
        Feature(Icons.Filled.Movie, R.string.landing_feat_watch_title, R.string.landing_feat_watch_desc),
        Feature(Icons.Filled.Groups, R.string.landing_feat_social_title, R.string.landing_feat_social_desc),
        Feature(Icons.Filled.Theaters, R.string.landing_feat_theater_title, R.string.landing_feat_theater_desc),
        Feature(Icons.AutoMirrored.Filled.Chat, R.string.landing_feat_boards_title, R.string.landing_feat_boards_desc),
        Feature(Icons.Filled.Download, R.string.landing_feat_downloads_title, R.string.landing_feat_downloads_desc),
        Feature(Icons.Filled.AutoAwesome, R.string.landing_feat_discover_title, R.string.landing_feat_discover_desc),
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
                title = { Text(stringResource(R.string.landing_topbar)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
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
                                listOf(FilmatubeGreen.copy(alpha = 0.14f), MaterialTheme.colorScheme.background),
                            ),
                        )
                        .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Pill(stringResource(R.string.landing_badge), Icons.Filled.AutoAwesome)
                    Text(
                        stringResource(R.string.landing_title),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = FilmatubeSpacing.lg),
                    )
                    Text(
                        stringResource(R.string.landing_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = FilmatubeSpacing.md),
                    )
                    FilmatubePrimaryButton(
                        text = stringResource(R.string.landing_cta_primary),
                        onClick = onBrowse,
                        leadingIcon = Icons.Filled.PlayArrow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = FilmatubeSpacing.xl),
                    )
                    FilmatubeSecondaryButton(
                        text = stringResource(R.string.landing_cta_secondary),
                        onClick = onCommunity,
                        leadingIcon = Icons.Filled.Groups,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = FilmatubeSpacing.sm),
                    )
                    // Poster strip
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                        contentPadding = PaddingValues(top = FilmatubeSpacing.xl),
                    ) {
                        items(6) { i ->
                            Box(
                                modifier = Modifier
                                    .width(80.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Movie,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ── Features ──────────────────────────────────────────
            item {
                SectionHeader(stringResource(R.string.landing_features_kicker), stringResource(R.string.landing_features_title))
            }
            items(features) { f ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FilmatubeGreen.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(f.icon, contentDescription = null, tint = FilmatubeGreen, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(stringResource(f.title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(f.desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── How it works ──────────────────────────────────────
            item {
                SectionHeader(stringResource(R.string.landing_how_kicker), stringResource(R.string.landing_how_title))
                Column(modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg)) {
                    Step(1, R.string.landing_step1_title, R.string.landing_step1_desc)
                    Step(2, R.string.landing_step2_title, R.string.landing_step2_desc)
                    Step(3, R.string.landing_step3_title, R.string.landing_step3_desc)
                }
            }

            // ── Player features (chips) ───────────────────────────
            item {
                SectionHeader(stringResource(R.string.landing_player_kicker), stringResource(R.string.landing_player_title))
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
                SectionHeader(stringResource(R.string.landing_social_kicker), stringResource(R.string.landing_social_title))
                Column(modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg)) {
                    SocialPoint(Icons.Filled.Favorite, R.string.landing_social_react)
                    SocialPoint(Icons.Filled.Send, R.string.landing_social_recommend)
                    SocialPoint(Icons.Filled.RssFeed, R.string.landing_social_feed)
                }
            }

            // ── FAQ ───────────────────────────────────────────────
            item {
                SectionHeader(stringResource(R.string.landing_faq_kicker), stringResource(R.string.landing_faq_title))
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

@Composable
private fun Pill(text: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(50),
        color = FilmatubeGreen.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, FilmatubeGreen.copy(alpha = 0.4f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
            modifier = Modifier.padding(horizontal = FilmatubeSpacing.md, vertical = FilmatubeSpacing.xs),
        ) {
            Icon(icon, contentDescription = null, tint = FilmatubeGreen, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = FilmatubeGreen)
        }
    }
}

@Composable
private fun SectionHeader(kicker: String, title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = FilmatubeSpacing.lg, end = FilmatubeSpacing.lg, top = FilmatubeSpacing.xxl, bottom = FilmatubeSpacing.md),
    ) {
        Text(kicker.uppercase(), style = MaterialTheme.typography.labelMedium, color = FilmatubeGold)
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Step(n: Int, title: Int, desc: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FilmatubeSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(50))
                .background(FilmatubeGreen),
            contentAlignment = Alignment.Center,
        ) {
            Text("$n", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
        }
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Icon(icon, contentDescription = null, tint = FilmatubeGreen, modifier = Modifier.size(22.dp))
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FeatureChip(icon: ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = !open }
            .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.md)
            .animateContentSize(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(q, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
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
