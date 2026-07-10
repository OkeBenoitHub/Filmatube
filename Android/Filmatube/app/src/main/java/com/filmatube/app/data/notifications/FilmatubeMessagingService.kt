package com.filmatube.app.data.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.filmatube.app.R
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Receives FCM pushes: persists new tokens and posts a system notification routed to the
 * social/content/system channel by the message's `category` data field. A `movieId` (or
 * `route`) data field becomes a deep link so tapping the notification opens the right screen.
 */
class FilmatubeMessagingService : FirebaseMessagingService() {

    /** Hilt entry point — access the singleton preferences without making the Service @AndroidEntryPoint. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PrefsEntryPoint {
        fun userPreferences(): UserPreferencesRepository
    }

    private val preferences: UserPreferencesRepository by lazy {
        EntryPointAccessors.fromApplication(applicationContext, PrefsEntryPoint::class.java).userPreferences()
    }

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("fcmTokens").document(token)
            .set(
                mapOf(
                    "token" to token,
                    "platform" to "android",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val category = data["category"]
        // respect the user's per-channel opt-out
        if (!runBlocking { preferences.notifEnabledFor(category).first() }) return

        val title = message.notification?.title ?: data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: data["body"] ?: ""
        val channelId = FilmatubeNotificationChannels.channelFor(category)

        val deepLink: Uri? = when {
            !data["route"].isNullOrBlank() -> Uri.parse(data["route"])
            !data["movieId"].isNullOrBlank() -> Uri.parse("filmatube://movie/${data["movieId"]}")
            else -> null
        }
        val intent = if (deepLink != null) {
            Intent(Intent.ACTION_VIEW, deepLink).setPackage(packageName)
        } else {
            packageManager.getLaunchIntentForPackage(packageName)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_filmatube_mark)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
