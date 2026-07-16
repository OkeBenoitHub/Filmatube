package com.filmatube.app.ui.parties

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.data.parties.Party
import com.filmatube.app.data.parties.PartyRepository
import com.filmatube.app.ui.theme.FilmatubeShapes
import com.filmatube.app.ui.theme.FilmatubeSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class MyPartiesViewModel @Inject constructor(partyRepository: PartyRepository) : ViewModel() {
    val parties = partyRepository.observeMyParties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Party>())
}

/** Horizontal strip of my upcoming/live watch parties — shown on the Community feed. */
@Composable
fun MyPartiesRow(
    onOpenParty: (String) -> Unit,
    viewModel: MyPartiesViewModel = hiltViewModel(),
) {
    val parties by viewModel.parties.collectAsStateWithLifecycle()
    if (parties.isEmpty()) return

    Column {
        Text(
            stringResource(R.string.party_my_parties),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = FilmatubeSpacing.lg, vertical = FilmatubeSpacing.sm),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = FilmatubeSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            items(parties, key = { it.id }) { party ->
                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .clickable { onOpenParty(party.id) },
                    verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
                ) {
                    AsyncImage(
                        model = party.moviePoster,
                        contentDescription = party.movieTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(110.dp)
                            .aspectRatio(2f / 3f)
                            .clip(FilmatubeShapes.medium),
                    )
                    Text(
                        party.movieTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (party.isLive) {
                            stringResource(R.string.party_status_live)
                        } else {
                            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(party.scheduledAtMs))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (party.isLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
