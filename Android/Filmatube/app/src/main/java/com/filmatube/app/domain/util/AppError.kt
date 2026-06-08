package com.filmatube.app.domain.util

import java.io.IOException

/**
 * Domain-level error type. Repositories/use cases translate raw exceptions into one of these
 * so the UI can react without knowing about HTTP/Firebase/IO specifics.
 *
 * HTTP-status mapping is expanded once Retrofit lands (Day 5); for now we map the common cases.
 */
sealed class AppError(open val message: String? = null) {
    /** No connectivity / timeout / socket failure. */
    data class Network(override val message: String? = null) : AppError(message)

    /** Backend returned an error (optionally with an HTTP status code). */
    data class Server(val code: Int? = null, override val message: String? = null) : AppError(message)

    /** Requested resource does not exist. */
    data class NotFound(override val message: String? = null) : AppError(message)

    /** Auth/permission failure. */
    data class Unauthorized(override val message: String? = null) : AppError(message)

    /** Anything not otherwise classified. */
    data class Unknown(override val message: String? = null) : AppError(message)
}

/** Best-effort mapping from a raw throwable to an [AppError]. */
fun Throwable.toAppError(): AppError = when (this) {
    is AppErrorException -> error
    is IOException -> AppError.Network(message)
    else -> AppError.Unknown(message)
}

/** Wraps an [AppError] so it can be thrown across suspend boundaries and re-mapped later. */
class AppErrorException(val error: AppError) : Exception(error.message)
