package com.filmatube.app.ui.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.filmatube.app.R
import com.filmatube.app.data.social.RecipientUser
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeTextField
import com.filmatube.app.ui.components.UserAvatar
import com.filmatube.app.ui.theme.FilmatubeSpacing

@Composable
fun RecommendDialog(
    recipients: List<RecipientUser>,
    onSend: (uid: String, message: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedUid by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .padding(FilmatubeSpacing.lg)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
            ) {
                Text(stringResource(R.string.recommend_title), style = MaterialTheme.typography.titleLarge)

                if (recipients.isEmpty()) {
                    Text(
                        stringResource(R.string.recommend_no_followers),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.xs),
                    ) {
                        recipients.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedUid = user.uid }
                                    .padding(vertical = FilmatubeSpacing.xs),
                                horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selectedUid == user.uid, onClick = { selectedUid = user.uid })
                                UserAvatar(url = user.avatarUrl, name = user.displayName, size = 32.dp)
                                Text(user.displayName, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    FilmatubeTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = stringResource(R.string.recommend_message_hint),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    FilmatubePrimaryButton(
                        text = stringResource(R.string.recommend_send),
                        onClick = { selectedUid?.let { onSend(it, message) } },
                        enabled = selectedUid != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
