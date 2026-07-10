package com.filmatube.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.social.Recommendation
import com.filmatube.app.data.social.RecommendationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecommendationInboxViewModel @Inject constructor(
    recommendationRepository: RecommendationRepository,
) : ViewModel() {
    val inbox = recommendationRepository.observeInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Recommendation>())
}
