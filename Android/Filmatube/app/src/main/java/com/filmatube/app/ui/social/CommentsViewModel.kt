package com.filmatube.app.ui.social

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.filmatube.app.data.social.Comment
import com.filmatube.app.data.social.CommentRepository
import com.filmatube.app.data.social.ReportRepository
import com.filmatube.app.data.social.ReportTargetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val commentRepository: CommentRepository,
    private val reportRepository: ReportRepository,
    preferences: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movieId: String = savedStateHandle["movieId"] ?: ""

    val comments = commentRepository.observeComments(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val spoilerFree = preferences.spoilerFree
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun post(text: String, hasSpoiler: Boolean, parentId: String?) {
        if (text.isBlank()) return
        viewModelScope.launch { commentRepository.addComment(movieId, text, hasSpoiler, parentId) }
    }

    fun delete(comment: Comment) {
        viewModelScope.launch { commentRepository.deleteComment(movieId, comment.id) }
    }

    fun toggleLike(comment: Comment) {
        viewModelScope.launch { commentRepository.toggleLike(movieId, comment.id, comment.likedByMe) }
    }

    fun report(comment: Comment) {
        viewModelScope.launch {
            reportRepository.report(ReportTargetType.COMMENT, movieId, comment.id, comment.userId)
        }
    }
}
