package com.filmatube.app.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.data.boards.BoardTypes
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeSecondaryButton
import com.filmatube.app.ui.theme.FilmatubeSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDetailScreen(
    onBack: () -> Unit,
    viewModel: BoardDetailViewModel = hiltViewModel(),
) {
    val board by viewModel.board.collectAsStateWithLifecycle()
    val isMember by viewModel.isMember.collectAsStateWithLifecycle()
    val invitedCount by viewModel.invitedCount.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val invitedManyMsg = invitedCount?.let { stringResource(R.string.board_invited, it) }
    val invitedNoneMsg = stringResource(R.string.board_invited_none)
    LaunchedEffect(invitedCount) {
        val count = invitedCount ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(if (count > 0) invitedManyMsg!! else invitedNoneMsg)
        viewModel.clearInvited()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(board?.title ?: stringResource(R.string.boards_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
            )
        },
    ) { padding ->
        val b = board
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                if (b?.coverUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = b.coverUrl,
                        contentDescription = b.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                ),
                            ),
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Groups, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Column(
                modifier = Modifier.padding(FilmatubeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs)) {
                    Text(b?.title ?: "", style = MaterialTheme.typography.headlineSmall)
                    if (b?.isOfficial == true) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = stringResource(R.string.board_official),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (b != null) {
                    val typeLabel = stringResource(
                        if (b.type == BoardTypes.MOVIE) R.string.board_type_movie else R.string.board_type_general,
                    )
                    Text(
                        "$typeLabel · ${stringResource(R.string.boards_members, b.memberCount)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (b?.description?.isNotBlank() == true) {
                    Text(
                        b.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (b != null) {
                    Row(
                        modifier = Modifier.padding(top = FilmatubeSpacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                    ) {
                        if (viewModel.isOwner) {
                            FilmatubePrimaryButton(
                                text = stringResource(R.string.board_invite),
                                onClick = viewModel::invite,
                                leadingIcon = Icons.Filled.PersonAdd,
                                modifier = Modifier.weight(1f),
                            )
                        } else if (isMember) {
                            FilmatubeSecondaryButton(
                                text = stringResource(R.string.board_leave),
                                onClick = viewModel::toggleMembership,
                                modifier = Modifier.weight(1f),
                            )
                            FilmatubePrimaryButton(
                                text = stringResource(R.string.board_invite),
                                onClick = viewModel::invite,
                                leadingIcon = Icons.Filled.PersonAdd,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            FilmatubePrimaryButton(
                                text = stringResource(R.string.board_join),
                                onClick = viewModel::toggleMembership,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.board_chat_soon),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
