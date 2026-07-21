package com.filmatube.app.ui.theater

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.data.theater.Showtime
import com.filmatube.app.data.theater.TheaterAttendee
import com.filmatube.app.ui.components.EmptyView
import com.filmatube.app.ui.components.PageHero
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeGold
import com.filmatube.app.ui.theme.FilmatubeSpacing
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Online Movie Theater (v1.2): the public lineup of scheduled showtimes and premieres.
 *
 * The tab leads with whatever is on right now — or the next thing up — then lists the
 * rest of the schedule. Every card carries the three things that make a screening feel
 * real: the poster, a live countdown, and who else is coming.
 */
@Composable
fun TheaterScreen(
    onShowtimeClick: (String) -> Unit = {},
    viewModel: TheaterViewModel = hiltViewModel(),
) {
    val lineup by viewModel.lineup.collectAsStateWithLifecycle()
    val attendees by viewModel.featuredAttendees.collectAsStateWithLifecycle()
    val nowMs by rememberTickingClock()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = FilmatubeSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
    ) {
        item {
            PageHero(
                eyebrow = stringResource(R.string.theater_eyebrow),
                title = stringResource(R.string.nav_theater),
                subtitle = stringResource(R.string.theater_subtitle),
                icon = Icons.Outlined.Theaters,
            )
        }

        val featured = lineup.featured
        if (featured != null) {
            item(key = "featured-${featured.id}") {
                FeaturedShowtimeCard(
                    showtime = featured,
                    attendees = attendees,
                    nowMs = nowMs,
                    onClick = { onShowtimeClick(featured.id) },
                    modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg),
                )
            }
        }

        // Anything else already running gets its own section — "on now" is a different
        // decision from "worth planning for", so the two shouldn't share a list.
        val liveRest = lineup.nowShowing.filter { it.id != featured?.id }
        if (liveRest.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.theater_now_showing)) }
            items(liveRest, key = { it.id }) { showtime ->
                ShowtimeRow(showtime, nowMs, onClick = { onShowtimeClick(showtime.id) })
            }
        }

        val upcoming = lineup.upcoming.filter { it.id != featured?.id }
        if (upcoming.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.theater_upcoming)) }
            items(upcoming, key = { it.id }) { showtime ->
                ShowtimeRow(showtime, nowMs, onClick = { onShowtimeClick(showtime.id) })
            }
        }

        // Only claim the lineup is empty once a snapshot has actually arrived.
        if (lineup.loaded && lineup.isEmpty) {
            item {
                EmptyView(
                    modifier = Modifier.fillMaxWidth().padding(top = FilmatubeSpacing.xxl),
                    icon = Icons.Outlined.Theaters,
                    title = stringResource(R.string.theater_empty_title),
                    message = stringResource(R.string.theater_empty_message),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(
            start = FilmatubeSpacing.lg,
            end = FilmatubeSpacing.lg,
            top = FilmatubeSpacing.sm,
        ),
    )
}

// ── cards ────────────────────────────────────────────────────────────────────

@Composable
private fun FeaturedShowtimeCard(
    showtime: Showtime,
    attendees: List<TheaterAttendee>,
    nowMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                AsyncImage(
                    model = showtime.backdropUrl.ifBlank { showtime.posterUrl },
                    contentDescription = showtime.movieTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Scrim so the badges and title stay legible over any artwork.
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        ),
                    ),
                )
                Row(
                    modifier = Modifier.align(Alignment.TopStart).padding(FilmatubeSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                ) {
                    if (showtime.isPremiere) PremiereBadge()
                    if (showtime.isLive) LiveBadge()
                }
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(FilmatubeSpacing.md),
                ) {
                    Text(
                        showtime.movieTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        countdownLabel(showtime, nowMs),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (showtime.isLive) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(FilmatubeSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
            ) {
                if (attendees.isNotEmpty()) AvatarStack(attendees)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        attendeeLabel(showtime),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        startTimeLabel(showtime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showtime.isFull) {
                    Text(
                        stringResource(R.string.theater_full),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowtimeRow(showtime: Showtime, nowMs: Long, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = showtime.posterUrl,
            contentDescription = showtime.movieTitle,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(56.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
            ) {
                Text(
                    showtime.movieTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (showtime.isPremiere) PremiereBadge()
            }
            Text(
                startTimeLabel(showtime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
            ) {
                Icon(
                    Icons.Filled.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    attendeeLabel(showtime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            countdownLabel(showtime, nowMs),
            style = MaterialTheme.typography.labelLarge,
            color = if (showtime.isLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Overlapping faces, capped — the rest are implied by the attendee count next to it. */
@Composable
private fun AvatarStack(attendees: List<TheaterAttendee>, max: Int = 4) {
    // Negative spacing overlaps the circles; the ring keeps each face separable.
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        attendees.take(max).forEach { attendee ->
            UserAvatar(
                url = attendee.avatar.ifBlank { null },
                name = attendee.name,
                size = 28.dp,
                modifier = Modifier
                    .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
internal fun PremiereBadge() {
    // Gold, not the brand green: a premiere is the one showing on the lineup that's genuinely
    // rare — a film's first public screening — and it can't say so while wearing the same
    // colour as every ordinary showtime, the live dot and the RSVP button.
    Surface(shape = RoundedCornerShape(50), color = FilmatubeGold) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier.size(11.dp),
            )
            Text(
                stringResource(R.string.theater_premiere),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.85f),
            )
        }
    }
}

/** A softly pulsing dot — the same "we're on air" language the party room uses. */
@Composable
private fun LiveBadge() {
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.55f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(pulse)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                stringResource(R.string.theater_live_now),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

// ── time ─────────────────────────────────────────────────────────────────────

/**
 * A clock that ticks once a second while the tab is composed, so every countdown on
 * screen advances off a single timer rather than one per card.
 */
@Composable
internal fun rememberTickingClock(): State<Long> = produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(1_000)
    }
}

/** "Live now" / "in 2d 4h" / "in 12m 30s" — coarse far out, precise up close. */
@Composable
internal fun countdownLabel(showtime: Showtime, nowMs: Long): String {
    if (showtime.isLive) return stringResource(R.string.theater_live_now)
    val remaining = showtime.startsInMs(nowMs)
    if (remaining <= 0L) return stringResource(R.string.theater_starting_now)

    val days = TimeUnit.MILLISECONDS.toDays(remaining)
    val hours = TimeUnit.MILLISECONDS.toHours(remaining) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
    return when {
        days > 0 -> stringResource(R.string.theater_countdown_days, days, hours)
        hours > 0 -> stringResource(R.string.theater_countdown_hours, hours, minutes)
        else -> stringResource(R.string.theater_countdown_minutes, minutes, seconds)
    }
}

internal fun startTimeLabel(showtime: Showtime): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(showtime.startAtMs))

@Composable
private fun attendeeLabel(showtime: Showtime): String = stringResource(
    if (showtime.isLive) R.string.theater_watching else R.string.theater_going,
    showtime.attendeesCount,
)
