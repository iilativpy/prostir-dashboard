package com.prostir.dashboard.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** Single entry point to the Admin API. Returns [Result] with an [ApiException] on failure. */
class AdminRepository(
    private val api: AdminApi,
    private val tokens: TokenStore,
) {
    private val _houses = MutableStateFlow<List<House>>(emptyList())

    /** Houses seen in the last occupancy response. Their order defines the colour and number of each house. */
    val houses: StateFlow<List<House>> = _houses

    val isLoggedIn: Boolean get() = tokens.token != null

    val sessionExpiresAt: String? get() = tokens.expiresAt

    suspend fun login(login: String, password: String, deviceName: String?): Result<Unit> =
        call(isLogin = true) {
            val data = api.login(LoginRequest(login.trim(), password, deviceName)).requireData()
            tokens.save(data.token, data.expiresAt)
        }

    /** Revokes the token on the server (best effort) and always forgets it locally. */
    suspend fun logout() {
        try {
            if (tokens.token != null) api.logout()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline logout is fine: the token is dropped locally either way.
        } finally {
            tokens.clear()
            _houses.value = emptyList()
        }
    }

    suspend fun occupancy(year: Int? = null): Result<Occupancy> = call {
        api.occupancy(year).requireData().also { _houses.value = it.houses }
    }

    suspend fun bookingPace(month: Int? = null): Result<BookingPace> = call {
        api.bookingPace(month).requireData()
    }

    suspend fun arrivals(from: LocalDate, to: LocalDate): Result<Arrivals> = call {
        api.arrivals(from.toString(), to.toString()).requireData()
    }

    private suspend fun <T> call(isLogin: Boolean = false, block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(ApiException(e.toApiError(isLogin)))
    }

    private fun <T> Envelope<T>.requireData(): T =
        data ?: throw ApiException(ApiError.Unknown(message ?: "Порожня відповідь сервера"))
}

fun Throwable.apiError(): ApiError = (this as? ApiException)?.error ?: toApiError()
