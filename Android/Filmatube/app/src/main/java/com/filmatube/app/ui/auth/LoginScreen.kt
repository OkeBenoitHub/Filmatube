package com.filmatube.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmatube.app.R
import com.filmatube.app.ui.components.FilmatubePrimaryButton
import com.filmatube.app.ui.components.FilmatubeTextField
import com.filmatube.app.ui.theme.FilmatubeSpacing

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onLoggedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = FilmatubeSpacing.xl, vertical = FilmatubeSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
    ) {
        AuthHeader(
            title = stringResource(R.string.auth_login_title),
            subtitle = stringResource(R.string.auth_login_subtitle),
        )

        FilmatubeTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = stringResource(R.string.auth_email),
            leadingIcon = Icons.Outlined.Email,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            errorText = state.emailError?.let { stringResource(it) },
            enabled = !state.isLoading,
            modifier = Modifier.padding(top = FilmatubeSpacing.sm),
        )

        FilmatubeTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = stringResource(R.string.auth_password),
            leadingIcon = Icons.Outlined.Lock,
            isPassword = true,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = viewModel::signIn,
            errorText = state.passwordError?.let { stringResource(it) },
            enabled = !state.isLoading,
        )

        TextButton(
            onClick = onNavigateToForgot,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.auth_forgot_password))
        }

        state.generalError?.let { AuthErrorBanner(message = stringResource(it)) }

        FilmatubePrimaryButton(
            text = stringResource(R.string.auth_sign_in),
            onClick = viewModel::signIn,
            loading = state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        )

        AuthDivider(modifier = Modifier.padding(vertical = FilmatubeSpacing.sm))

        GoogleButton(
            onClick = { viewModel.signInWithGoogle(context) },
            enabled = !state.isLoading,
        )

        AuthFooterPrompt(
            prompt = stringResource(R.string.auth_no_account),
            actionText = stringResource(R.string.auth_sign_up),
            onAction = onNavigateToRegister,
        )
    }
}
