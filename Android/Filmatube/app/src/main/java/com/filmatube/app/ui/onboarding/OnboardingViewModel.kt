package com.filmatube.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    /** Persists that onboarding is done, then invokes [onDone] once written. */
    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
            onDone()
        }
    }
}
