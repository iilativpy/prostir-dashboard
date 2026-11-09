package com.prostir.dashboard.data

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/** Everything the UI needs to tell the user what went wrong. */
sealed interface ApiError {
    /** Login only: wrong login or password (401). */
    data object InvalidCredentials : ApiError

    /** Any other request: the token is missing, expired or revoked (401). */
    data object SessionExpired : ApiError

    /** Login: more than 5 attempts per minute (429). */
    data class TooManyAttempts(val retryAfterSeconds: Int) : ApiError

    /** Missing or malformed fields (422). */
    data class Validation(val message: String?) : ApiError

    /** No connection, timeout, DNS. */
    data object Network : ApiError

    /** 5xx from the backend. */
    data object Server : ApiError

    data class Unknown(val message: String?) : ApiError
}

class ApiException(val error: ApiError) : Exception(error.toString())

private const val DEFAULT_RETRY_AFTER_SECONDS = 60

fun Throwable.toApiError(isLogin: Boolean = false): ApiError = when (this) {
    is ApiException -> error
    is HttpException -> when (code()) {
        401 -> if (isLogin) ApiError.InvalidCredentials else ApiError.SessionExpired
        422 -> ApiError.Validation(message())
        429 -> ApiError.TooManyAttempts(
            response()?.headers()?.get("Retry-After")?.toIntOrNull() ?: DEFAULT_RETRY_AFTER_SECONDS,
        )
        in 500..599 -> ApiError.Server
        else -> ApiError.Unknown("HTTP ${code()}")
    }
    is IOException -> ApiError.Network
    is SerializationException -> ApiError.Unknown("Неочікувана відповідь сервера")
    else -> ApiError.Unknown(message)
}
