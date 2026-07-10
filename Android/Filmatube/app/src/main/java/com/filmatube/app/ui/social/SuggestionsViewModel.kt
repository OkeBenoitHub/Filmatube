package com.filmatube.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.social.FollowRepository
import com.filmatube.app.data.social.SocialRepository
import com.filmatube.app.data.social.SuggestedUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val followRepository: FollowRepository,
) : ViewModel() {

    private val _suggestions = MutableStateFlow<List<SuggestedUser>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            _suggestions.value = runCatching { socialRepository.suggestedUsers() }.getOrDefault(emptyList())
        }
    }

    /** Follow + remove from the list. */
    fun follow(targetUid: String) {
        _suggestions.value = _suggestions.value.filterNot { it.uid == targetUid }
        viewModelScope.launch { followRepository.setFollowing(targetUid, true) }
    }
}
