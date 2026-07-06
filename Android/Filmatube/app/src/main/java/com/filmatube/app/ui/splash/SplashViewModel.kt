package com.filmatube.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.filmatube.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Where the splash routes once the app state is resolved. */
enum class SplashDestination { ONBOARDING, LOGIN, MAIN }

@HiltViewModel
class SplashViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    /** Null while loading; resolves once onboarding flag + auth state are known. */
    val destination: StateFlow<SplashDestination?> = combine(
        preferences.onboardingCompleted,
        authRepository.authState,
    ) { onboardingCompleted, user ->
        when {
            !onboardingCompleted -> SplashDestination.ONBOARDING
            user == null -> SplashDestination.LOGIN
            else -> SplashDestination.MAIN
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )
}
