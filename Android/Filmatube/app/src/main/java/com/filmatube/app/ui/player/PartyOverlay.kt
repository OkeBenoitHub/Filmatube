package com.filmatube.app.ui.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.filmatube.app.R
import com.filmatube.app.ui.theme.FilmatubeSpacing
import kotlinx.coroutines.delay

/** How long a floating emoji stays on screen. */
const val REACTION_TTL_MS = 4_000L

/**
 * One chat line as the playback overlay needs it — deliberately not a party or a theater
 * type, so both rooms can share this overlay without either owning it.
 */
data class OverlayMessage(
    val id: String,
    val userName: String,
    val text: String,
    val isSpoiler: Boolean = false,
)

/**
 * Translucent chat overlay drawn over the video: the last few lines, an emoji reaction bar
 * and a compact composer. Used by watch parties (Day 144) and the theater (Day 159).
 *
 * [onSend] receives the text and whether the sender flagged it as a spoiler. Pass
 * [spoilerToggle] = false where spoilers make no sense (a private party watching together
 * is already past the point of spoiling itself).
 */
@Composable
fun PlaybackChatOverlay(
    messages: List<OverlayMessage>,
    reactionEmojis: List<String>,
    onSend: (text: String, isSpoiler: Boolean) -> Unit,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier,
    spoilerToggle: Boolean = false,
) {
    // Reveal state lives with the overlay: a spoiler you opened should stay open while
    // the line is still on screen, but it resets as soon as the line scrolls away.
    val revealed = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FilmatubeSpacing.md),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
    ) {
        // Last few lines only — the overlay must not swallow the picture.
        messages.takeLast(4).forEach { m ->
            val isHidden = m.isSpoiler && revealed[m.id] != true
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .then(if (isHidden) Modifier.clickable { revealed[m.id] = true } else Modifier)
                    .padding(horizontal = FilmatubeSpacing.sm, vertical = FilmatubeSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    m.userName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (isHidden) {
                    Icon(
                        Icons.Filled.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 2.dp),
                    )
                    Text(
                        stringResource(R.string.reviews_spoiler_hidden),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                } else {
                    Text(m.text, style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
            reactionEmojis.forEach { emoji ->
                Text(
                    emoji,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { onReact(emoji) }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }

        OverlayComposer(onSend = onSend, spoilerToggle = spoilerToggle)
    }
}

@Composable
private fun OverlayComposer(onSend: (String, Boolean) -> Unit, spoilerToggle: Boolean) {
    var text by remember { mutableStateOf("") }
    var isSpoiler by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.5f)),
    ) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.party_chat_hint), color = Color.White.copy(alpha = 0.5f)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
        )
        if (spoilerToggle) {
            IconButton(onClick = { isSpoiler = !isSpoiler }) {
                Icon(
                    Icons.Filled.VisibilityOff,
                    contentDescription = stringResource(R.string.reviews_spoiler_toggle),
                    tint = if (isSpoiler) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                )
            }
        }
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text, isSpoiler)
                    text = ""
                    isSpoiler = false
                }
            },
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.party_chat_send),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** One reaction as the overlay needs it, independent of which room it came from. */
data class OverlayReaction(
    val id: String,
    val emoji: String,
    val createdAtMs: Long,
)

/**
 * The rising-emoji column. Only reactions that are still fresh and haven't already
 * animated are drawn, so recomposition never replays the same emoji.
 */
@Composable
fun FloatingReactions(
    reactions: List<OverlayReaction>,
    spent: MutableList<String>,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        reactions
            .filter { it.id !in spent && now - it.createdAtMs < REACTION_TTL_MS }
            .takeLast(6)
            .forEach { r ->
                key(r.id) {
                    FloatingReaction(id = r.id, emoji = r.emoji, onDone = { spent.add(it) })
                }
            }
    }
}

/** One floating emoji: rises and fades over [REACTION_TTL_MS], then reports itself done. */
@Composable
fun FloatingReaction(id: String, emoji: String, onDone: (String) -> Unit) {
    var started by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = REACTION_TTL_MS.toInt()),
        label = "reaction-rise",
    )
    LaunchedEffect(id) {
        started = true
        delay(REACTION_TTL_MS)
        onDone(id)
    }
    Text(
        emoji,
        fontSize = 28.sp,
        modifier = Modifier
            .padding(bottom = (progress * 160).dp)
            .alpha(1f - progress),
    )
}
