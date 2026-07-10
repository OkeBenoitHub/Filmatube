package com.filmatube.app.ui.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.filmatube.app.data.analytics.PlaybackAnalytics
import com.filmatube.app.data.download.DownloadRepository
import com.filmatube.app.data.download.DownloadUtil
import com.filmatube.app.data.playback.PlaybackRepository
import com.filmatube.app.data.playback.WatchProgressRepository
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.filmatube.app.data.social.FeedEventTypes
import com.filmatube.app.data.social.FeedRepository
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.model.SubtitleStyle
import com.filmatube.app.domain.repository.MovieRepository
import com.filmatube.app.domain.util.AppError
import com.filmatube.app.domain.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the [ExoPlayer] for one movie. Resolves a token-protected R2 URL via
 * [PlaybackRepository], prepares progressive MP4 playback, and releases the player
 * on [onCleared] so it survives configuration changes but never leaks.
 */
@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val playbackRepository: PlaybackRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val movieRepository: MovieRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val downloadRepository: DownloadRepository,
    private val feedRepository: FeedRepository,
    private val analytics: PlaybackAnalytics,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movieId: String = checkNotNull(savedStateHandle["movieId"])
    private var movieTitle: String = ""

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

    /** Embedded audio tracks (multi-language) + current selection + playback speed. */
    private val _audioTracks = MutableStateFlow<List<AudioTrackOption>>(emptyList())
    val audioTracks: StateFlow<List<AudioTrackOption>> = _audioTracks.asStateFlow()
    private val _selectedAudio = MutableStateFlow<String?>(null)
    val selectedAudio: StateFlow<String?> = _selectedAudio.asStateFlow()
    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    /** Sleep timer: selected option + remaining ms (null when off / end-of-movie). */
    private var sleepJob: Job? = null
    private val _sleepOption = MutableStateFlow<SleepTimerOption?>(null)
    val sleepOption: StateFlow<SleepTimerOption?> = _sleepOption.asStateFlow()
    private val _sleepRemainingMs = MutableStateFlow<Long?>(null)
    val sleepRemainingMs: StateFlow<Long?> = _sleepRemainingMs.asStateFlow()

    /** Recommended next movie (autoplay "Up Next") + admin skip-intro markers (ms). */
    private val _upNext = MutableStateFlow<Movie?>(null)
    val upNext: StateFlow<Movie?> = _upNext.asStateFlow()
    private val _introStartMs = MutableStateFlow(0L)
    val introStartMs: StateFlow<Long> = _introStartMs.asStateFlow()
    private val _introEndMs = MutableStateFlow(0L)
    val introEndMs: StateFlow<Long> = _introEndMs.asStateFlow()

    /** Persisted subtitle appearance, applied by the player's SubtitleView. */
    val subtitleStyle: StateFlow<SubtitleStyle> = preferencesRepository.subtitleStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubtitleStyle())

    fun setSubtitleStyle(style: SubtitleStyle) {
        viewModelScope.launch { preferencesRepository.setSubtitleStyle(style) }
    }

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Cache-backed source: streams online, and plays downloaded movies from the local cache offline.
    private val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(DownloadUtil.getCacheDataSourceFactory(context))

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build().apply {
        playWhenReady = true
        // Request audio focus (duck/pause on interruptions) + pause when headphones unplug.
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true,
        )
        setHandleAudioBecomingNoisy(true)
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
                    maybeAutoDeleteWatched()
                    viewModelScope.launch { feedRepository.publish(FeedEventTypes.WATCHED, movieId, movieTitle) }
                }
                // Recovered after a network drop / retry.
                if (playbackState == Player.STATE_READY && _uiState.value is PlayerUiState.Error) {
                    _uiState.value = PlayerUiState.Ready
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val options = mutableListOf<AudioTrackOption>()
                var selected: String? = null
                for (group in tracks.groups) {
                    if (group.type != C.TRACK_TYPE_AUDIO) continue
                    for (i in 0 until group.length) {
                        val language = group.getTrackFormat(i).language ?: continue
                        if (options.none { it.language == language }) {
                            options.add(AudioTrackOption(language, group.getTrackFormat(i).label ?: language))
                        }
                        if (group.isTrackSelected(i)) selected = language
                    }
                }
                _audioTracks.value = options
                _selectedAudio.value = selected
            }
        })
    }

    init {
        load()
        registerNetworkCallback()
        // Periodically checkpoint the watch position while playing.
        viewModelScope.launch {
            while (true) {
                delay(SAVE_INTERVAL_MS)
                if (player.isPlaying) persistProgress()
            }
        }
    }

    /** Retry playback from the last position when the network comes back after a drop. */
    private fun registerNetworkCallback() {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                viewModelScope.launch {
                    if (player.playerError != null) {
                        player.prepare()
                        player.play()
                    }
                }
            }
        }
        networkCallback = callback
        runCatching { connectivityManager?.registerDefaultNetworkCallback(callback) }
    }

    fun load() {
        _uiState.value = PlayerUiState.Loading
        viewModelScope.launch {
            runCatching {
                val movie = runCatching { movieRepository.getMovie(movieId) }.getOrNull()
                // Expired downloads are removed and fall back to streaming.
                if (downloadRepository.isExpired(movieId)) downloadRepository.cancel(movieId)
                // Play from the local download when available (works offline); else stream.
                val download = downloadRepository.getDownload(movieId)?.takeIf { it.state == Download.STATE_COMPLETED }
                val uri = download?.request?.uri ?: Uri.parse(playbackRepository.streamUrl(movieId))
                val subtitles = if (download != null) {
                    downloadRepository.subtitlesFor(movieId).map { it.lang to it.url }
                } else {
                    movie?.subtitleTracks.orEmpty().map { it.lang to it.url }
                }
                val next = runCatching {
                    movie?.let { m -> movieRepository.getRelated(movieId, m.genres).firstOrNull() }
                        ?: movieRepository.getNewReleases().firstOrNull { it.id != movieId }
                }.getOrNull()
                LoadedMedia(uri, subtitles, movie, next)
            }
                .onSuccess { media ->
                    val mediaItem = MediaItem.Builder()
                        .setUri(media.uri)
                        .setSubtitleConfigurations(
                            media.subtitles.map { (lang, url) ->
                                MediaItem.SubtitleConfiguration.Builder(Uri.parse(url))
                                    .setMimeType(MimeTypes.TEXT_VTT)
                                    .setLanguage(lang)
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
                    _subtitleLanguages.value = media.subtitles.map { it.first }
                    _upNext.value = media.next
                    _introStartMs.value = (media.movie?.introStartSec ?: 0) * 1000L
                    _introEndMs.value = (media.movie?.introEndSec ?: 0) * 1000L
                    movieTitle = media.movie?.title?.get("en") ?: ""
                    viewModelScope.launch { feedRepository.publish(FeedEventTypes.WATCHING, movieId, movieTitle) }
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

    /** Select an embedded audio language. */
    fun selectAudio(language: String) {
        _selectedAudio.value = language
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setPreferredAudioLanguage(language)
            .build()
        analytics.feature(movieId, "audio")
    }

    /** Set playback speed (0.5x–2x). */
    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player.setPlaybackSpeed(speed)
        if (speed != 1f) analytics.feature(movieId, "speed")
    }

    /** Log engagement with a player feature (skip_intro, up_next, …). */
    fun logFeature(name: String) = analytics.feature(movieId, name)

    /** Arm/cancel the sleep timer. Timed options count down then pause; null = off. */
    fun setSleepTimer(option: SleepTimerOption?) {
        sleepJob?.cancel()
        _sleepOption.value = option
        if (option != null) analytics.feature(movieId, "sleep_timer")
        if (option == null || option == SleepTimerOption.END_OF_MOVIE) {
            _sleepRemainingMs.value = null
            return
        }
        val totalMs = option.minutes * 60_000L
        _sleepRemainingMs.value = totalMs
        sleepJob = viewModelScope.launch {
            var remaining = totalMs
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _sleepRemainingMs.value = remaining
            }
            player.pause()
            _sleepOption.value = null
            _sleepRemainingMs.value = null
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
        if (lang != null) analytics.feature(movieId, "subtitle")
    }

    fun startOver() {
        player.seekTo(0L)
        _resumePrompt.value = null
    }

    fun dismissResumePrompt() {
        _resumePrompt.value = null
    }

    private fun maybeAutoDeleteWatched() {
        viewModelScope.launch {
            if (preferencesRepository.downloadAutoDeleteWatched.first() && downloadRepository.isDownloaded(movieId)) {
                downloadRepository.cancel(movieId)
            }
        }
    }

    private fun persistProgress() {
        val position = player.currentPosition
        val duration = player.duration
        if (duration <= 0L) return
        viewModelScope.launch { watchProgressRepository.save(movieId, position, duration) }
    }

    override fun onCleared() {
        sleepJob?.cancel()
        networkCallback?.let { cb -> runCatching { connectivityManager?.unregisterNetworkCallback(cb) } }
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

/** A selectable embedded audio track. */
data class AudioTrackOption(val language: String, val label: String)

/** Resolved media for playback: video uri + (lang,url) subtitles + movie + up-next. */
private data class LoadedMedia(
    val uri: Uri,
    val subtitles: List<Pair<String, String>>,
    val movie: Movie?,
    val next: Movie?,
)

/** Sleep-timer choices. Timed options auto-pause after [minutes]; END_OF_MOVIE has none. */
enum class SleepTimerOption(val minutes: Long) {
    MIN_15(15), MIN_30(30), MIN_45(45), MIN_60(60), END_OF_MOVIE(0)
}
