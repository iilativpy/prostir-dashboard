package com.prostir.dashboard.ui

import com.prostir.dashboard.data.ApiError
import com.prostir.dashboard.data.apiError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State of one request. Old [data] stays visible while a refresh runs or after it fails,
 * so the screen can show "data as of 09:12" instead of going blank.
 */
data class Loadable<T>(
    val data: T? = null,
    val loading: Boolean = false,
    val error: ApiError? = null,
    /** Epoch millis of the last successful response. */
    val updatedAt: Long? = null,
) {
    val isInitialLoading: Boolean get() = data == null && loading && error == null
}

class Loader<T>(private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(Loadable<T>(loading = true))
    val state: StateFlow<Loadable<T>> = _state
    private var job: Job? = null

    /**
     * @param clear drop the old data first (the request is for something else, e.g. another month)
     * @param silent background poll: no loading indicator
     */
    fun load(clear: Boolean = false, silent: Boolean = false, fetch: suspend () -> Result<T>) {
        job?.cancel()
        _state.update {
            when {
                clear -> Loadable(loading = true)
                silent -> it
                else -> it.copy(loading = true)
            }
        }
        job = scope.launch {
            fetch().fold(
                onSuccess = { value ->
                    _state.value = Loadable(data = value, loading = false, error = null, updatedAt = System.currentTimeMillis())
                },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.apiError()) } },
            )
        }
    }
}
