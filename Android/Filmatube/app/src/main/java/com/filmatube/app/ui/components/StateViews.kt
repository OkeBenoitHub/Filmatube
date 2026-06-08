package com.filmatube.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.filmatube.app.R
import com.filmatube.app.domain.util.AppError
import com.filmatube.app.ui.theme.FilmatubeSpacing

/** Centered loading spinner. */
@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Empty-state placeholder with an icon, title and optional message. */
@Composable
fun EmptyView(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.state_empty_default),
    message: String? = null,
    icon: ImageVector = Icons.Outlined.Inbox,
) {
    StateMessage(modifier = modifier, icon = icon, title = title, message = message)
}

/** Error-state placeholder mapped from an [AppError], with a Retry action. */
@Composable
fun ErrorView(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StateMessage(
        modifier = modifier,
        icon = Icons.Outlined.ErrorOutline,
        title = appErrorMessage(error),
        message = null,
        actionLabel = stringResource(R.string.action_retry),
        onAction = onRetry,
    )
}

@Composable
private fun StateMessage(
    icon: ImageVector,
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FilmatubeSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(FilmatubeSpacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Spacer(Modifier.height(FilmatubeSpacing.xs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(FilmatubeSpacing.lg))
            FilmatubePrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Maps an [AppError] to a user-facing message. */
@Composable
fun appErrorMessage(error: AppError): String = when (error) {
    is AppError.Network -> stringResource(R.string.error_network)
    is AppError.NotFound -> stringResource(R.string.error_not_found)
    is AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
    is AppError.Server -> error.message ?: stringResource(R.string.error_generic)
    is AppError.Unknown -> error.message ?: stringResource(R.string.error_generic)
}
