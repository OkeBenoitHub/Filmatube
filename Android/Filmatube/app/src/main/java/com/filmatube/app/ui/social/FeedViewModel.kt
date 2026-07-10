package com.filmatube.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.filmatube.app.data.social.FeedEvent
import com.filmatube.app.data.social.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FeedFilter { TODAY, WEEK, ALL }

private const val PAGE_SIZE = 25

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val _limit = MutableStateFlow(PAGE_SIZE)
    private val _filter = MutableStateFlow(FeedFilter.ALL)
    val filter = _filter.asStateFlow()

    val feed = combine(
        _limit.flatMapLatest { feedRepository.observeFeed(it) },
        preferences.mutedActors,
        _filter,
    ) { events, muted, filter ->
        val cutoff = filter.cutoffMs()
        events.filter { it.actorId !in muted && it.createdAtMs >= cutoff }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<FeedEvent>())

    fun setFilter(filter: FeedFilter) {
        _filter.value = filter
    }

    fun loadMore() {
        _limit.value += PAGE_SIZE
    }

    fun muteActor(actorId: String) {
        if (actorId.isBlank()) return
        viewModelScope.launch { preferences.toggleMutedActor(actorId) }
    }
}

private fun FeedFilter.cutoffMs(): Long = when (this) {
    FeedFilter.TODAY -> System.currentTimeMillis() - 24L * 60 * 60 * 1000
    FeedFilter.WEEK -> System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    FeedFilter.ALL -> 0L
}
