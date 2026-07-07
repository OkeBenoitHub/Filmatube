package com.filmatube.app.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.filmatube.app.MainActivity

private val PIP_ASPECT = Rational(16, 9)

/** Tracks whether the host activity is currently in Picture-in-Picture. */
@Composable
fun rememberIsInPipMode(): Boolean {
    val activity = LocalContext.current.findComponentActivity()
    return (activity as? MainActivity)?.isInPipMode ?: false
}

/** Enter PiP now (e.g. from the PiP button). No-op below API 26. */
fun Activity.enterPip() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val builder = PictureInPictureParams.Builder().setAspectRatio(PIP_ASPECT)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) builder.setAutoEnterEnabled(true)
    enterPictureInPictureMode(builder.build())
}

/** Auto-enter PiP when the user leaves while playing (API 31+). */
fun Activity.setPipAutoEnter(enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    setPictureInPictureParams(
        PictureInPictureParams.Builder()
            .setAspectRatio(PIP_ASPECT)
            .setAutoEnterEnabled(enabled)
            .build(),
    )
}

fun Context.findComponentActivity(): ComponentActivity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}
