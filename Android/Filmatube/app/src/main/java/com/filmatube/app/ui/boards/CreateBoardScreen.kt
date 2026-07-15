package com.filmatube.app.ui.boards

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmatube.app.R
import com.filmatube.app.data.boards.BoardTypes
import com.filmatube.app.ui.components.FilmatubeFilterChip
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeTextField
import com.filmatube.app.ui.theme.FilmatubeSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBoardScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreateBoardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdId) {
        state.createdId?.let(onCreated)
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.setCover(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_board_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(FilmatubeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            // Cover picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (state.coverUri != null) {
                    AsyncImage(
                        model = state.coverUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.board_add_cover),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = FilmatubeSpacing.xs),
                        )
                    }
                }
            }

            FilmatubeTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = stringResource(R.string.board_field_title),
            )
            FilmatubeTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = stringResource(R.string.board_field_description),
                singleLine = false,
                minLines = 2,
            )

            // Type
            Text(stringResource(R.string.board_type_label), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                FilmatubeFilterChip(
                    label = stringResource(R.string.board_type_general),
                    selected = state.type == BoardTypes.GENERAL,
                    onClick = { viewModel.setType(BoardTypes.GENERAL) },
                )
                FilmatubeFilterChip(
                    label = stringResource(R.string.board_type_movie),
                    selected = state.type == BoardTypes.MOVIE,
                    onClick = { viewModel.setType(BoardTypes.MOVIE) },
                )
            }

            // Visibility
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(if (state.isPublic) R.string.board_visibility_public else R.string.board_visibility_private),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Switch(checked = state.isPublic, onCheckedChange = viewModel::setPublic)
            }

            if (state.error) {
                Text(
                    stringResource(R.string.board_create_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            FilmatubePrimaryButton(
                text = stringResource(R.string.board_create),
                onClick = viewModel::create,
                enabled = state.canCreate,
                loading = state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
