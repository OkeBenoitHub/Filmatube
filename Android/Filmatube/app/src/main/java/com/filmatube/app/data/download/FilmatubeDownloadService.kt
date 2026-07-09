package com.filmatube.app.data.download

import android.app.Notification
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.filmatube.app.R

/** Foreground service that runs offline downloads with a progress notification. */
@UnstableApi
class FilmatubeDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DownloadUtil.DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.downloads_channel_name,
    0,
) {
    override fun getDownloadManager(): DownloadManager = DownloadUtil.getDownloadManager(this)

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification =
        DownloadUtil.getDownloadNotificationHelper(this).buildProgressNotification(
            this,
            R.drawable.ic_filmatube_mark,
            null,
            null,
            downloads,
            notMetRequirements,
        )

    private companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1
    }
}
