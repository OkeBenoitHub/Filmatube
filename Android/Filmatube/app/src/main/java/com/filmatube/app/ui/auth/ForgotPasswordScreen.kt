package com.filmatube.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeSecondaryButton
import com.filmatube.app.ui.components.FilmatubeTextField
import com.filmatube.app.ui.theme.FilmatubeGreen
import com.filmatube.app.ui.theme.FilmatubeSpacing

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(FilmatubeSpacing.sm)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.forgot_back_to_login))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = FilmatubeSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
        ) {
            if (state.isSent) {
                SentContent(email = state.email.trim(), onBack = onBack)
            } else {
                FormContent(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun FormContent(state: ForgotPasswordUiState, viewModel: ForgotPasswordViewModel) {
    AuthHeader(
        title = stringResource(R.string.forgot_title),
        subtitle = stringResource(R.string.forgot_subtitle),
    )

    FilmatubeTextField(
        value = state.email,
        onValueChange = viewModel::onEmailChange,
        label = stringResource(R.string.auth_email),
        leadingIcon = Icons.Outlined.Email,
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Done,
        onImeAction = viewModel::sendReset,
        errorText = state.emailError?.let { stringResource(it) },
        enabled = !state.isLoading,
        modifier = Modifier.padding(top = FilmatubeSpacing.sm),
    )

    state.generalError?.let { AuthErrorBanner(message = stringResource(it)) }

    FilmatubePrimaryButton(
        text = stringResource(R.string.forgot_send),
        onClick = viewModel::sendReset,
        loading = state.isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    )
}

@Composable
private fun SentContent(email: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(top = FilmatubeSpacing.xxl)
            .size(96.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.MarkEmailRead,
            contentDescription = null,
            tint = FilmatubeGreen,
            modifier = Modifier.size(44.dp),
        )
    }
    Text(
        text = stringResource(R.string.forgot_sent_title),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
    )
    Text(
        text = stringResource(R.string.forgot_sent_message, email),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    FilmatubeSecondaryButton(
        text = stringResource(R.string.forgot_back_to_login),
        onClick = onBack,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = FilmatubeSpacing.md),
    )
}
