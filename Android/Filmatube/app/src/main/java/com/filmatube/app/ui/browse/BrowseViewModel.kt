package com.filmatube.app.ui.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.repository.MovieRepository
import com.filmatube.app.domain.repository.MovieSort
import com.filmatube.app.domain.util.DataState
import com.filmatube.app.domain.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowseUiState(
    val sort: MovieSort = MovieSort.NEWEST,
    val genre: String? = null,
    val year: Int? = null,
    val movies: DataState<List<Movie>> = DataState.Loading,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState(genre = savedStateHandle["genre"]))
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun setSort(sort: MovieSort) {
        _state.update { it.copy(sort = sort) }
        load()
    }

    fun setGenre(genre: String?) {
        _state.update { it.copy(genre = genre) }
        load()
    }

    fun setYear(year: Int?) {
        _state.update { it.copy(year = year) }
        load()
    }

    fun load() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(movies = DataState.Loading) }
            runCatching { movieRepository.browse(current.sort, current.genre, current.year) }
                .fold(
                    onSuccess = { list ->
                        _state.update {
                            it.copy(movies = if (list.isEmpty()) DataState.Empty else DataState.Success(list))
                        }
                    },
                    onFailure = { e -> _state.update { it.copy(movies = DataState.Error(e.toAppError())) } },
                )
        }
    }
}
