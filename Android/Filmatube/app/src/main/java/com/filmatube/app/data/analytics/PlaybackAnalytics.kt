package com.filmatube.app.data.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/** Logs playback lifecycle events (play / pause / complete) to Firebase Analytics. */
@Singleton
class PlaybackAnalytics @Inject constructor(
    private val analytics: FirebaseAnalytics,
) {
    fun play(movieId: String) = log(EVENT_PLAY, movieId)
    fun pause(movieId: String) = log(EVENT_PAUSE, movieId)
    fun complete(movieId: String) = log(EVENT_COMPLETE, movieId)

    private fun log(event: String, movieId: String) {
        analytics.logEvent(event, Bundle().apply { putString(PARAM_MOVIE_ID, movieId) })
    }

    private companion object {
        const val EVENT_PLAY = "video_play"
        const val EVENT_PAUSE = "video_pause"
        const val EVENT_COMPLETE = "video_complete"
        const val PARAM_MOVIE_ID = "movie_id"
    }
}
