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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.filmatube.app.data.parties.PARTY_REACTIONS
import com.filmatube.app.data.parties.PartyMessage
import com.filmatube.app.data.parties.PartyReaction
import com.filmatube.app.ui.theme.FilmatubeSpacing
import kotlinx.coroutines.delay

/** How long a floating emoji stays on screen. */
const val REACTION_TTL_MS = 4_000L

/**
 * Watch-party overlay drawn over the video: the last few chat lines, an emoji reaction bar and
 * a compact composer. Everything is translucent so it never hides the film.
 */
@Composable
fun PartyOverlay(
    messages: List<PartyMessage>,
    onSend: (String) -> Unit,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FilmatubeSpacing.md),
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
    ) {
        // Last few lines only — the overlay must not swallow the picture.
        messages.takeLast(4).forEach { m ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = FilmatubeSpacing.sm, vertical = FilmatubeSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
            ) {
                Text(
                    m.userName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(m.text, style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
            PARTY_REACTIONS.forEach { emoji ->
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

        PartyComposer(onSend = onSend)
    }
}

@Composable
private fun PartyComposer(onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
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
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
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

/** One floating emoji: rises and fades over [REACTION_TTL_MS], then reports itself done. */
@Composable
fun FloatingReaction(reaction: PartyReaction, onDone: (String) -> Unit) {
    var started by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = REACTION_TTL_MS.toInt()),
        label = "reaction-rise",
    )
    LaunchedEffect(reaction.id) {
        started = true
        delay(REACTION_TTL_MS)
        onDone(reaction.id)
    }
    Text(
        reaction.emoji,
        fontSize = 28.sp,
        modifier = Modifier
            .padding(bottom = (progress * 160).dp)
            .alpha(1f - progress),
    )
}
