package com.filmatube.app.ui.collections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.collections.CollectionsRepository
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionDetailUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val movies: List<Movie> = emptyList(),
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collectionsRepository: CollectionsRepository,
    private val movieRepository: MovieRepository,
) : ViewModel() {

    private val collectionId: String = savedStateHandle.get<String>("collectionId").orEmpty()

    private val _state = MutableStateFlow(CollectionDetailUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val collection = collectionsRepository.getCollection(collectionId)
            val ids = collectionsRepository.getMovieIds(collectionId)
            val movies = movieRepository.getMoviesByIds(ids)
            _state.value = CollectionDetailUiState(
                isLoading = false,
                title = collection?.title.orEmpty(),
                movies = movies,
            )
        }
    }
}
