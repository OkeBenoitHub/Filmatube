package com.filmatube.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.domain.model.DownloadQuality
import com.filmatube.app.ui.components.FilmatubeFilterChip
import com.filmatube.app.ui.components.FilmatubeSecondaryButton
import com.filmatube.app.ui.theme.FilmatubeSpacing

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageProfiles: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenNotificationPrefs: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val downloadQuality by viewModel.downloadQuality.collectAsStateWithLifecycle()
    val downloadWifiOnly by viewModel.downloadWifiOnly.collectAsStateWithLifecycle()
    val spoilerFree by viewModel.spoilerFree.collectAsStateWithLifecycle()
    val autoDeleteWatched by viewModel.downloadAutoDeleteWatched.collectAsStateWithLifecycle()

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
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FilmatubeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                FilmatubeFilterChip(
                    label = stringResource(R.string.lang_english),
                    selected = language == "en",
                    onClick = { viewModel.setLanguage("en") },
                )
                FilmatubeFilterChip(
                    label = stringResource(R.string.lang_french),
                    selected = language == "fr",
                    onClick = { viewModel.setLanguage("fr") },
                )
            }

            Text(stringResource(R.string.download_quality), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm)) {
                FilmatubeFilterChip(
                    label = stringResource(R.string.download_quality_low),
                    selected = downloadQuality == DownloadQuality.LOW,
                    onClick = { viewModel.setDownloadQuality(DownloadQuality.LOW) },
                )
                FilmatubeFilterChip(
                    label = stringResource(R.string.download_quality_standard),
                    selected = downloadQuality == DownloadQuality.STANDARD,
                    onClick = { viewModel.setDownloadQuality(DownloadQuality.STANDARD) },
                )
                FilmatubeFilterChip(
                    label = stringResource(R.string.download_quality_high),
                    selected = downloadQuality == DownloadQuality.HIGH,
                    onClick = { viewModel.setDownloadQuality(DownloadQuality.HIGH) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.download_wifi_only), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Switch(checked = downloadWifiOnly, onCheckedChange = viewModel::setDownloadWifiOnly)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.download_auto_delete_watched), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Switch(checked = autoDeleteWatched, onCheckedChange = viewModel::setDownloadAutoDeleteWatched)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_spoiler_free), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.settings_spoiler_free_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = spoilerFree, onCheckedChange = viewModel::setSpoilerFree)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenNotificationPrefs)
                    .padding(vertical = FilmatubeSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(horizontal = FilmatubeSpacing.sm))
                Text(stringResource(R.string.settings_notifications), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenLibrary)
                    .padding(vertical = FilmatubeSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(horizontal = FilmatubeSpacing.sm))
                Text(stringResource(R.string.my_library), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenDownloads)
                    .padding(vertical = FilmatubeSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(horizontal = FilmatubeSpacing.sm))
                Text(stringResource(R.string.my_downloads), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onManageProfiles)
                    .padding(vertical = FilmatubeSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Group, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(horizontal = FilmatubeSpacing.sm))
                Text(stringResource(R.string.settings_manage_profiles), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.weight(1f))

            FilmatubeSecondaryButton(
                text = stringResource(R.string.profile_sign_out),
                onClick = {
                    viewModel.signOut()
                    onSignedOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = FilmatubeSpacing.xl),
            )
        }
    }
}
