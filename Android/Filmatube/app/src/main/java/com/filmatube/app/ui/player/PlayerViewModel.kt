package com.filmatube.app.ui.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.filmatube.app.data.playback.PlaybackRepository
import com.filmatube.app.domain.util.AppError
import com.filmatube.app.domain.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movieId: String = checkNotNull(savedStateHandle["movieId"])

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        playWhenReady = true
        addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                _uiState.value = PlayerUiState.Error(error.toAppError())
            }
        })
    }

    init {
        load()
    }

    fun load() {
        _uiState.value = PlayerUiState.Loading
        viewModelScope.launch {
            runCatching { playbackRepository.streamUrl(movieId) }
                .onSuccess { url ->
                    player.setMediaItem(MediaItem.fromUri(url))
                    player.prepare()
                    _uiState.value = PlayerUiState.Ready
                }
                .onFailure { _uiState.value = PlayerUiState.Error(it.toAppError()) }
        }
    }

    override fun onCleared() {
        player.release()
    }
}

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data object Ready : PlayerUiState
    data class Error(val error: AppError) : PlayerUiState
}
