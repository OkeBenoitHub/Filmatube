package com.filmatube.app.ui.collections

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.collections.CollectionsRepository
import com.filmatube.app.data.upload.AvatarUploader
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
    val coverUrl: String = "",
    val isPublic: Boolean = false,
    val isOwner: Boolean = false,
    val movies: List<Movie> = emptyList(),
    val coverUploading: Boolean = false,
    // Add-movies picker.
    val searchQuery: String = "",
    val searchResults: List<Movie> = emptyList(),
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collectionsRepository: CollectionsRepository,
    private val movieRepository: MovieRepository,
    private val uploader: AvatarUploader,
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
            _state.update {
                it.copy(
                    isLoading = false,
                    title = collection?.title.orEmpty(),
                    coverUrl = collection?.coverUrl.orEmpty(),
                    isPublic = collection?.isPublic ?: false,
                    isOwner = collection != null && collection.userId == collectionsRepository.currentUid,
                    movies = movies,
                )
            }
        }
    }

    fun rename(title: String) {
        val clean = title.trim().ifBlank { return }
        _state.update { it.copy(title = clean) }
        viewModelScope.launch { collectionsRepository.update(collectionId, title = clean) }
    }

    fun setPublic(isPublic: Boolean) {
        _state.update { it.copy(isPublic = isPublic) }
        viewModelScope.launch { collectionsRepository.update(collectionId, isPublic = isPublic) }
    }

    fun setCover(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(coverUploading = true) }
            val url = runCatching { uploader.uploadCollectionCover(uri) }.getOrNull()
            if (url != null) {
                collectionsRepository.update(collectionId, coverUrl = url)
                _state.update { it.copy(coverUrl = url) }
            }
            _state.update { it.copy(coverUploading = false) }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            collectionsRepository.delete(collectionId)
            onDeleted()
        }
    }

    fun addMovie(movie: Movie) {
        if (_state.value.movies.any { it.id == movie.id }) return
        _state.update { it.copy(movies = it.movies + movie) }
        viewModelScope.launch { collectionsRepository.addMovie(collectionId, movie.id) }
    }

    fun removeMovie(movieId: String) {
        _state.update { it.copy(movies = it.movies.filterNot { m -> m.id == movieId }) }
        viewModelScope.launch { collectionsRepository.removeMovie(collectionId, movieId) }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        val term = query.trim()
        if (term.isEmpty()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val results = runCatching { movieRepository.search(term) }.getOrDefault(emptyList())
            // Only reflect the latest query.
            if (_state.value.searchQuery.trim() == term) {
                _state.update { it.copy(searchResults = results) }
            }
        }
    }

    fun clearSearch() {
        _state.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }
}
