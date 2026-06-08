package com.filmatube.app.domain.util

/**
 * UI-facing state wrapper for any asynchronously-loaded data.
 *
 * Screens observe a `StateFlow<DataState<T>>` and render loading/empty/content/error accordingly.
 */
sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>

    data object Empty : DataState<Nothing>

    data class Success<out T>(val data: T) : DataState<T>

    data class Error(val error: AppError) : DataState<Nothing>
}

/** Returns the data if this is [DataState.Success], otherwise null. */
fun <T> DataState<T>.dataOrNull(): T? = (this as? DataState.Success)?.data

inline fun <T, R> DataState<T>.map(transform: (T) -> R): DataState<R> = when (this) {
    is DataState.Success -> DataState.Success(transform(data))
    is DataState.Error -> this
    DataState.Loading -> DataState.Loading
    DataState.Empty -> DataState.Empty
}
