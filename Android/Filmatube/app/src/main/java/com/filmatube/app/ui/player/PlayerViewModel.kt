package com.filmatube.app.ui.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.filmatube.app.data.analytics.PlaybackAnalytics
import com.filmatube.app.data.playback.PlaybackRepository
import com.filmatube.app.data.playback.WatchProgressRepository
import com.filmatube.app.domain.repository.MovieRepository
import com.filmatube.app.domain.util.AppError
import com.filmatube.app.domain.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the [ExoPlayer] for one movie. Resolves a token-protected R2 URL via
 * [PlaybackRepository], prepares progressive MP4 playback, and releases the player
 * on [onCleared] so it survives configuration changes but never leaks.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val playbackRepository: PlaybackRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val movieRepository: MovieRepository,
    private val analytics: PlaybackAnalytics,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movieId: String = checkNotNull(savedStateHandle["movieId"])

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    /** Non-null (resume position in ms) briefly after re-opening a partially-watched movie. */
    private val _resumePrompt = MutableStateFlow<Long?>(null)
    val resumePrompt: StateFlow<Long?> = _resumePrompt.asStateFlow()

    /** Available subtitle language codes and the current selection (null = off). */
    private val _subtitleLanguages = MutableStateFlow<List<String>>(emptyList())
    val subtitleLanguages: StateFlow<List<String>> = _subtitleLanguages.asStateFlow()
    private val _selectedSubtitle = MutableStateFlow<String?>(null)
    val selectedSubtitle: StateFlow<String?> = _selectedSubtitle.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        playWhenReady = true
        addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                _uiState.value = PlayerUiState.Error(error.toAppError())
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    analytics.play(movieId)
                } else {
                    analytics.pause(movieId)
                    persistProgress() // save on pause/background
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    analytics.complete(movieId)
                    persistProgress()
                }
            }
        })
    }

    init {
        load()
        // Periodically checkpoint the watch position while playing.
        viewModelScope.launch {
            while (true) {
                delay(SAVE_INTERVAL_MS)
                if (player.isPlaying) persistProgress()
            }
        }
    }

    fun load() {
        _uiState.value = PlayerUiState.Loading
        viewModelScope.launch {
            runCatching {
                val url = playbackRepository.streamUrl(movieId)
                val subtitles = runCatching { movieRepository.getMovie(movieId)?.subtitleTracks }
                    .getOrNull().orEmpty()
                url to subtitles
            }
                .onSuccess { (url, subtitles) ->
                    val mediaItem = MediaItem.Builder()
                        .setUri(url)
                        .setSubtitleConfigurations(
                            subtitles.map { track ->
                                MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
                                    .setMimeType(MimeTypes.TEXT_VTT)
                                    .setLanguage(track.lang)
                                    .build()
                            },
                        )
                        .build()
                    player.setMediaItem(mediaItem)
                    // Subtitles off until the viewer picks a language.
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                    player.prepare()
                    _subtitleLanguages.value = subtitles.map { it.lang }
                    val resumeMs = watchProgressRepository.resumePosition(movieId)
                    if (resumeMs > 0L) {
                        player.seekTo(resumeMs)
                        _resumePrompt.value = resumeMs
                    }
                    _uiState.value = PlayerUiState.Ready
                }
                .onFailure { _uiState.value = PlayerUiState.Error(it.toAppError()) }
        }
    }

    /** Select a subtitle language (null = off). */
    fun selectSubtitle(lang: String?) {
        _selectedSubtitle.value = lang
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().apply {
            if (lang == null) {
                setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                setPreferredTextLanguage(lang)
            }
        }.build()
    }

    fun startOver() {
        player.seekTo(0L)
        _resumePrompt.value = null
    }

    fun dismissResumePrompt() {
        _resumePrompt.value = null
    }

    private fun persistProgress() {
        val position = player.currentPosition
        val duration = player.duration
        if (duration <= 0L) return
        viewModelScope.launch { watchProgressRepository.save(movieId, position, duration) }
    }

    override fun onCleared() {
        player.release()
    }

    private companion object {
        const val SAVE_INTERVAL_MS = 10_000L
    }
}

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data object Ready : PlayerUiState
    data class Error(val error: AppError) : PlayerUiState
}
