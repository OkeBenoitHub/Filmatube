package com.filmatube.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.domain.model.WatchProfile
import com.filmatube.app.ui.components.FilmatubeTextField
import com.filmatube.app.ui.theme.FilmatubeGreen
import com.filmatube.app.ui.theme.FilmatubeSpacing

private val EMOJIS = listOf("🍿", "🎬", "🎥", "😎", "🐱", "🦄", "🚀", "🎮", "🌟", "🐸", "👾", "🎧")

@Composable
fun ProfilesScreen(
    onBack: () -> Unit,
    viewModel: ProfilesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<WatchProfile?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FilmatubeSpacing.sm, vertical = FilmatubeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.settings_back))
            }
            Text(stringResource(R.string.profiles_title), style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FilmatubeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
        ) {
            state.profiles.forEach { profile ->
                ProfileRow(
                    profile = profile,
                    isActive = profile.id == state.activeProfileId,
                    onClick = { viewModel.setActive(profile.id) },
                    onEdit = { editing = profile },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { creating = true }
                    .padding(vertical = FilmatubeSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.padding(horizontal = FilmatubeSpacing.sm))
                Text(stringResource(R.string.profiles_add), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (creating) {
        ProfileDialog(
            title = stringResource(R.string.profiles_create),
            initialName = "",
            initialEmoji = EMOJIS.first(),
            canDelete = false,
            onConfirm = { name, emoji -> viewModel.createProfile(name, emoji); creating = false },
            onDelete = {},
            onDismiss = { creating = false },
        )
    }

    editing?.let { profile ->
        ProfileDialog(
            title = stringResource(R.string.profiles_edit),
            initialName = profile.name,
            initialEmoji = profile.avatarEmoji,
            canDelete = !profile.isDefault,
            onConfirm = { name, emoji -> viewModel.updateProfile(profile.id, name, emoji); editing = null },
            onDelete = { viewModel.deleteProfile(profile.id); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ProfileRow(
    profile: WatchProfile,
    isActive: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = FilmatubeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(profile.avatarEmoji, fontSize = 24.sp)
        }
        Spacer(Modifier.padding(horizontal = FilmatubeSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(profile.name, style = MaterialTheme.typography.bodyLarge)
            if (profile.isDefault) {
                Text(
                    stringResource(R.string.profiles_default),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isActive) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = stringResource(R.string.profiles_active), tint = FilmatubeGreen)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.profiles_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileDialog(
    title: String,
    initialName: String,
    initialEmoji: String,
    canDelete: Boolean,
    onConfirm: (name: String, emoji: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md)) {
                FilmatubeTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.profiles_name),
                    imeAction = ImeAction.Done,
                )
                Text(stringResource(R.string.profiles_choose_emoji), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                    EMOJIS.forEach { option ->
                        val selected = option == emoji
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) FilmatubeGreen else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    shape = CircleShape,
                                )
                                .clickable { emoji = option },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(option, fontSize = 22.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, emoji) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.profiles_save)) }
        },
        dismissButton = {
            if (canDelete) {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.profiles_delete), color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.profiles_cancel)) }
            }
        },
    )
}
