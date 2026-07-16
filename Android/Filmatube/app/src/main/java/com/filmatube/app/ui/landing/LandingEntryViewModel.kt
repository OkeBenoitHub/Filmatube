package com.filmatube.app.ui.landing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Routing state for the landing when it's the signed-out entry screen: "Get started" runs the
 * carousel only on first open; afterwards it goes straight to sign-in.
 */
@HiltViewModel
class LandingEntryViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
) : ViewModel() {

    /** Null while DataStore loads — treat as "not completed" so first-run never skips the tour. */
    val onboardingCompleted: StateFlow<Boolean?> = preferences.onboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
