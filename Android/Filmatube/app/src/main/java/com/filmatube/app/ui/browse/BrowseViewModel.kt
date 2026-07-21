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
    /** True when opened from Home's "Coming soon" row — narrows to unreleased titles. */
    val comingSoon: Boolean = false,
    val movies: DataState<List<Movie>> = DataState.Loading,
) {
    /** Anything narrowed from the default view, which is what "Clear" undoes. */
    val hasFilters: Boolean
        get() = genre != null || year != null || comingSoon || sort != MovieSort.NEWEST
}

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Home's "See all" buttons each land here with the section they came from, so the grid
    // opens continuing that row rather than on a generic unfiltered catalog.
    private val _state = MutableStateFlow(
        BrowseUiState(
            genre = savedStateHandle["genre"],
            sort = runCatching { MovieSort.valueOf(savedStateHandle["sort"] ?: "") }
                .getOrDefault(MovieSort.NEWEST),
            comingSoon = savedStateHandle["comingSoon"] ?: false,
        ),
    )
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

    fun toggleComingSoon() {
        _state.update { it.copy(comingSoon = !it.comingSoon) }
        load()
    }

    fun setYear(year: Int?) {
        _state.update { it.copy(year = year) }
        load()
    }

    /** Back to the default view — newest first, every genre, every year, released or not. */
    fun clearFilters() {
        _state.update {
            it.copy(sort = MovieSort.NEWEST, genre = null, year = null, comingSoon = false)
        }
        load()
    }

    fun load() {
        val current = _state.value
        viewModelScope.launch {
            _state.update { it.copy(movies = DataState.Loading) }
            runCatching {
                movieRepository.browse(
                    sort = current.sort,
                    genre = current.genre,
                    year = current.year,
                    comingSoon = if (current.comingSoon) true else null,
                )
            }
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
