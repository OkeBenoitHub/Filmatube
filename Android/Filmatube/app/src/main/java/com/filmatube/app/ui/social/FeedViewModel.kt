package com.filmatube.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.social.FeedEvent
import com.filmatube.app.data.social.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    feedRepository: FeedRepository,
) : ViewModel() {
    val feed = feedRepository.observeFeed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<FeedEvent>())
}
