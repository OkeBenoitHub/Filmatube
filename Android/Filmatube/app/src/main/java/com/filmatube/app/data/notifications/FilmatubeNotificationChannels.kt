package com.filmatube.app.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.filmatube.app.R

/**
 * The three notification channels. Push messages carry a `category` data field
 * (`social`/`content`/`system`) that maps to one of these.
 */
object FilmatubeNotificationChannels {
    const val SOCIAL = "filmatube_social"
    const val CONTENT = "filmatube_content"
    const val SYSTEM = "filmatube_system"

    /** Category → channel id (defaults to SOCIAL). Mirrors the `category` push data field. */
    fun channelFor(category: String?): String = when (category) {
        "content" -> CONTENT
        "system" -> SYSTEM
        else -> SOCIAL
    }

    /** Idempotently registers all channels; safe to call on every app start. */
    fun createAll(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(SOCIAL, context.getString(R.string.notif_channel_social), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notif_channel_social_desc)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CONTENT, context.getString(R.string.notif_channel_content), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notif_channel_content_desc)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(SYSTEM, context.getString(R.string.notif_channel_system), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.notif_channel_system_desc)
            },
        )
    }
}
