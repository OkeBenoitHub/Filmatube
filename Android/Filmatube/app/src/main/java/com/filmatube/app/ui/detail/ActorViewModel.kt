package com.filmatube.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.repository.MovieRepository
import com.filmatube.app.domain.util.DataState
import com.filmatube.app.domain.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActorViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val actorName: String = savedStateHandle["name"] ?: ""

    private val _state = MutableStateFlow<DataState<List<Movie>>>(DataState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = DataState.Loading
            runCatching { movieRepository.getByActor(actorName) }.fold(
                onSuccess = { list -> _state.value = if (list.isEmpty()) DataState.Empty else DataState.Success(list) },
                onFailure = { _state.value = DataState.Error(it.toAppError()) },
            )
        }
    }
}
