package com.filmatube.app.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.domain.util.AppError
import com.filmatube.app.domain.util.DataState
import com.filmatube.app.domain.util.toAppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Base class for all ViewModels.
 *
 * Provides small helpers for the common pattern of "run a suspending call, surface the result as
 * a [DataState]" so individual ViewModels stay focused on their own logic.
 */
abstract class BaseViewModel : ViewModel() {

    /** Convenience accessor for the ViewModel coroutine scope. */
    protected val scope: CoroutineScope get() = viewModelScope

    /**
     * Runs [block], pushing Loading → Success/Error into [target]. Empty results can be signalled
     * by returning `null` from [block] (rendered as [DataState.Empty]).
     */
    protected fun <T> loadInto(
        target: MutableStateFlow<DataState<T>>,
        block: suspend () -> T?,
    ) {
        viewModelScope.launch {
            target.value = DataState.Loading
            runCatching { block() }.fold(
                onSuccess = { result ->
                    target.value = if (result == null) DataState.Empty else DataState.Success(result)
                },
                onFailure = { throwable ->
                    target.value = DataState.Error(throwable.toAppError())
                },
            )
        }
    }

    /** Runs [block], invoking [onError] (default: no-op) if it throws. */
    protected fun launchCatching(
        onError: (AppError) -> Unit = {},
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { onError(it.toAppError()) }
        }
    }
}
