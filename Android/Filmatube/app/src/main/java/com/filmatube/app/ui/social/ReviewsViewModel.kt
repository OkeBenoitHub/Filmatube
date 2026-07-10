package com.filmatube.app.ui.social

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.social.Review
import com.filmatube.app.data.social.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movieId: String = savedStateHandle["movieId"] ?: ""

    val reviews = reviewRepository.observeReviews(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The current user's own review, if any (used to prefill the editor). */
    val myReview = reviews
        .map { list -> list.firstOrNull { it.isMine } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun submit(text: String, hasSpoiler: Boolean) {
        if (text.isBlank()) return
        viewModelScope.launch { reviewRepository.submitReview(movieId, text, hasSpoiler) }
    }

    fun delete() {
        viewModelScope.launch { reviewRepository.deleteReview(movieId) }
    }

    fun toggleLike(review: Review) {
        viewModelScope.launch { reviewRepository.toggleLike(movieId, review.id, review.likedByMe) }
    }
}
