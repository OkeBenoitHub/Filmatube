package com.filmatube.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.repository.MovieRepository
import com.filmatube.app.domain.util.DataState
import com.filmatube.app.domain.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchFilters(
    val genre: String? = null,
    val year: Int? = null,
    val minRating: Double = 0.0,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _filters = MutableStateFlow(SearchFilters())
    val filters = _filters.asStateFlow()

    private val _results = MutableStateFlow<DataState<List<Movie>>>(DataState.Empty)
    val results = _results.asStateFlow()

    private val _trending = MutableStateFlow<List<Movie>>(emptyList())
    val trending = _trending.asStateFlow()

    val recentSearches = preferences.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _trending.value = runCatching { movieRepository.getTrending(8) }.getOrDefault(emptyList())
        }
        viewModelScope.launch {
            combine(_query.debounce(300), _filters) { q, f -> q to f }
                .collect { (q, f) -> runSearch(q, f) }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onSubmit() {
        val term = _query.value.trim()
        if (term.isNotBlank()) {
            viewModelScope.launch { preferences.addRecentSearch(term) }
        }
    }

    fun useTerm(term: String) {
        _query.value = term
    }

    fun clearRecent() {
        viewModelScope.launch { preferences.clearRecentSearches() }
    }

    fun setGenre(genre: String?) = _filters.update { it.copy(genre = genre) }
    fun setYear(year: Int?) = _filters.update { it.copy(year = year) }
    fun setMinRating(minRating: Double) = _filters.update { it.copy(minRating = minRating) }

    private suspend fun runSearch(query: String, filters: SearchFilters) {
        if (query.isBlank()) {
            _results.value = DataState.Empty
            return
        }
        _results.value = DataState.Loading
        runCatching { movieRepository.search(query.trim()) }.fold(
            onSuccess = { list ->
                var filtered = list
                filters.genre?.let { g -> filtered = filtered.filter { g in it.genres } }
                filters.year?.let { y -> filtered = filtered.filter { it.year == y } }
                if (filters.minRating > 0) filtered = filtered.filter { it.averageRating >= filters.minRating }
                _results.value = if (filtered.isEmpty()) DataState.Empty else DataState.Success(filtered)
            },
            onFailure = { _results.value = DataState.Error(it.toAppError()) },
        )
    }
}
