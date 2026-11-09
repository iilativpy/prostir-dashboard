package com.prostir.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.prostir.dashboard.AppContainer
import com.prostir.dashboard.data.AdminRepository
import com.prostir.dashboard.data.ApiError
import com.prostir.dashboard.data.Arrivals
import com.prostir.dashboard.data.BookingPace
import com.prostir.dashboard.data.Occupancy
import com.prostir.dashboard.data.apiError
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LoginUiState(
    val login: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val loading: Boolean = false,
    val error: ApiError? = null,
    /** Seconds left until another attempt is allowed after a 429. */
    val lockSeconds: Int = 0,
) {
    val canSubmit: Boolean get() = login.isNotBlank() && password.isNotEmpty() && !loading && lockSeconds == 0
}

class LoginViewModel(private val repo: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()
    private var countdown: Job? = null

    // Editing a field clears the previous error, except while the attempt limit is counting down.
    fun onLogin(value: String) = _state.update { it.copy(login = value, error = if (it.lockSeconds > 0) it.error else null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = if (it.lockSeconds > 0) it.error else null) }
    fun toggleShowPassword() = _state.update { it.copy(showPassword = !it.showPassword) }

    fun submit(deviceName: String, onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.login(s.login, s.password, deviceName).fold(
                onSuccess = {
                    _state.update { it.copy(loading = false, password = "") }
                    onSuccess()
                },
                onFailure = { e ->
                    val error = e.apiError()
                    _state.update { it.copy(loading = false, error = error) }
                    if (error is ApiError.TooManyAttempts) startCountdown(error.retryAfterSeconds)
                },
            )
        }
    }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        _state.update { it.copy(lockSeconds = seconds) }
        countdown = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(1_000)
                left--
                _state.update { it.copy(lockSeconds = left) }
            }
            _state.update { it.copy(error = null) }
        }
    }
}

class OverviewViewModel(private val repo: AdminRepository) : ViewModel() {
    val occupancy = Loader<Occupancy>(viewModelScope)
    val pace = Loader<BookingPace>(viewModelScope)
    val arrivals = Loader<Arrivals>(viewModelScope)

    init {
        refresh()
    }

    fun refresh(silent: Boolean = false) {
        val today = LocalDate.now()
        occupancy.load(silent = silent) { repo.occupancy(today.year) }
        pace.load(silent = silent) { repo.bookingPace(today.monthValue) }
        // 62 days is the API maximum: also gives each house its next check-in.
        arrivals.load(silent = silent) { repo.arrivals(today, today.plusDays(62)) }
    }

    suspend fun logout() = repo.logout()
}

class PaceViewModel(private val repo: AdminRepository) : ViewModel() {
    private val _month = MutableStateFlow(LocalDate.now().monthValue)
    val month: StateFlow<Int> = _month.asStateFlow()
    val loader = Loader<BookingPace>(viewModelScope)

    init {
        refresh()
    }

    fun refresh(silent: Boolean = false) {
        val m = _month.value
        loader.load(silent = silent) { repo.bookingPace(m) }
    }

    fun step(delta: Int) {
        _month.value = ((_month.value - 1 + delta).mod(12)) + 1
        val m = _month.value
        loader.load(clear = true) { repo.bookingPace(m) }
    }
}

class MonthsViewModel(private val repo: AdminRepository, val houseFilter: MutableStateFlow<Int?>) : ViewModel() {
    private val _year = MutableStateFlow(LocalDate.now().year)
    val year: StateFlow<Int> = _year.asStateFlow()
    val loader = Loader<Occupancy>(viewModelScope)

    init {
        refresh()
    }

    fun refresh(silent: Boolean = false) {
        val y = _year.value
        loader.load(silent = silent) { repo.occupancy(y) }
    }

    fun selectYear(y: Int) {
        if (y == _year.value) return
        _year.value = y
        loader.load(clear = true) { repo.occupancy(y) }
    }
}

enum class Period(val label: String) { Today("Сьогодні"), Tomorrow("Завтра"), Week("Цей тиждень") }

class ArrivalsViewModel(private val repo: AdminRepository, val houseFilter: MutableStateFlow<Int?>) : ViewModel() {
    private val _period = MutableStateFlow(Period.Today)
    val period: StateFlow<Period> = _period.asStateFlow()
    val loader = Loader<Arrivals>(viewModelScope)
    val houses = repo.houses

    init {
        refresh()
    }

    fun refresh(silent: Boolean = false) {
        val p = _period.value
        loader.load(silent = silent) { fetch(p) }
        // Colours and numbers of houses come from the occupancy response.
        if (repo.houses.value.isEmpty()) viewModelScope.launch { repo.occupancy(LocalDate.now().year) }
    }

    fun selectPeriod(p: Period) {
        if (p == _period.value) return
        _period.value = p
        loader.load(clear = true) { fetch(p) }
    }

    private suspend fun fetch(p: Period): Result<Arrivals> {
        val today = LocalDate.now()
        return when (p) {
            Period.Today -> repo.arrivals(today, today)
            Period.Tomorrow -> repo.arrivals(today.plusDays(1), today.plusDays(1))
            Period.Week -> repo.arrivals(today, today.plusDays(6))
        }
    }
}

class VmFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = container.repository
        return when (modelClass) {
            LoginViewModel::class.java -> LoginViewModel(repo)
            OverviewViewModel::class.java -> OverviewViewModel(repo)
            PaceViewModel::class.java -> PaceViewModel(repo)
            MonthsViewModel::class.java -> MonthsViewModel(repo, container.houseFilter)
            ArrivalsViewModel::class.java -> ArrivalsViewModel(repo, container.houseFilter)
            else -> error("Unknown ViewModel ${modelClass.name}")
        } as T
    }
}
