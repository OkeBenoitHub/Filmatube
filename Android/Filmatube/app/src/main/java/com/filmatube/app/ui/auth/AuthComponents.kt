package com.filmatube.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.filmatube.app.R
import com.filmatube.app.ui.components.FilmatubeLogo
import com.filmatube.app.ui.theme.FilmatubeSpacing
import com.filmatube.app.util.FilmatubeLinks

/** Logo + title + subtitle header shared by login & register. */
@Composable
fun AuthHeader(title: String, subtitle: String) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FilmatubeSpacing.sm),
    ) {
        FilmatubeLogo(size = 72.dp)
        Spacer(Modifier.height(FilmatubeSpacing.sm))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** "Continue with Google" outlined button with the real Google mark. */
@Composable
fun GoogleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (loading) {
            // Same footprint as the mark, so the row doesn't shift while signing in.
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.ic_google),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(FilmatubeSpacing.md))
        Text(stringResource(R.string.auth_continue_google))
    }
}

/** "──── or ────" divider. */
@Composable
fun AuthDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FilmatubeSpacing.md),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.auth_or),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

/** Inline error banner shown above the primary action. */
@Composable
fun AuthErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = FilmatubeSpacing.md, vertical = FilmatubeSpacing.sm),
        )
    }
}

/**
 * "By creating an account you agree to our Terms of Service and Privacy Policy."
 *
 * Shown on register rather than gated behind a checkbox: the account creation itself is the
 * act of acceptance, and the two documents have to be readable *before* signing up, which is
 * why they open the public site rather than an in-app copy.
 */
@Composable
fun AuthLegalConsent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val terms = stringResource(R.string.landing_terms)
    val privacy = stringResource(R.string.landing_privacy)
    val full = stringResource(R.string.auth_legal_consent, terms, privacy)

    // Annotated so the two document names are tappable inside the sentence, which keeps the
    // copy readable in both languages regardless of where the words land.
    val annotated = buildAnnotatedString {
        append(full)
        listOf(terms to FilmatubeLinks.TERMS, privacy to FilmatubeLinks.PRIVACY).forEach { (label, url) ->
            val start = full.indexOf(label)
            if (start >= 0) {
                addStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    ),
                    start,
                    start + label.length,
                )
                addStringAnnotation("url", url, start, start + label.length)
            }
        }
    }

    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier.fillMaxWidth(),
        onClick = { offset ->
            annotated.getStringAnnotations("url", offset, offset)
                .firstOrNull()
                ?.let { FilmatubeLinks.open(context, it.item) }
        },
    )
}

/** "Don't have an account? Sign up" footer prompt. */
@Composable
fun AuthFooterPrompt(prompt: String, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onAction) { Text(actionText) }
    }
}
