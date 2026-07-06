package com.filmatube.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.filmatube.app.domain.model.WatchProfile
import com.filmatube.app.domain.repository.AuthRepository
import com.filmatube.app.domain.repository.ProfilesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfilesUiState(
    val profiles: List<WatchProfile> = emptyList(),
    val activeProfileId: String? = null,
)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profilesRepository: ProfilesRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    private val uid: String? = authRepository.currentUser()?.uid

    init {
        val currentUid = uid
        val defaultName = authRepository.currentUser()?.displayName?.takeIf { it.isNotBlank() } ?: "Me"
        if (currentUid != null) {
            viewModelScope.launch {
                runCatching { profilesRepository.ensureDefaultProfile(currentUid, defaultName, "en") }
            }
        }
    }

    val state: StateFlow<ProfilesUiState> =
        if (uid == null) {
            flowOf(ProfilesUiState())
        } else {
            combine(
                profilesRepository.observeProfiles(uid),
                preferences.activeProfileId,
            ) { profiles, activeId ->
                ProfilesUiState(profiles = profiles, activeProfileId = activeId ?: profiles.firstOrNull { it.isDefault }?.id)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUiState())

    fun setActive(id: String) {
        viewModelScope.launch { preferences.setActiveProfileId(id) }
    }

    fun createProfile(name: String, emoji: String) {
        val currentUid = uid ?: return
        viewModelScope.launch { runCatching { profilesRepository.createProfile(currentUid, name.trim(), emoji) } }
    }

    fun updateProfile(id: String, name: String, emoji: String) {
        val currentUid = uid ?: return
        viewModelScope.launch { runCatching { profilesRepository.updateProfile(currentUid, id, name.trim(), emoji) } }
    }

    fun deleteProfile(id: String) {
        val currentUid = uid ?: return
        viewModelScope.launch { runCatching { profilesRepository.deleteProfile(currentUid, id) } }
    }
}
