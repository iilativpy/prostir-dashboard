package com.prostir.dashboard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prostir.dashboard.AppContainer
import com.prostir.dashboard.ui.components.BottomNav
import com.prostir.dashboard.ui.components.Tab
import com.prostir.dashboard.ui.screens.ArrivalsScreen
import com.prostir.dashboard.ui.screens.LoginScreen
import com.prostir.dashboard.ui.screens.MonthsScreen
import com.prostir.dashboard.ui.screens.OverviewScreen
import com.prostir.dashboard.ui.screens.PaceScreen
import com.prostir.dashboard.ui.theme.Pro
import kotlinx.coroutines.launch

/** Login or the four tabs. ViewModels are keyed by [session] so a new login never shows the previous user's data. */
@Composable
fun AppRoot(container: AppContainer) {
    val factory = remember { VmFactory(container) }
    val scope = rememberCoroutineScope()
    var loggedIn by remember { mutableStateOf(container.repository.isLoggedIn) }
    var expiredNotice by remember { mutableStateOf(false) }
    var session by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        container.sessionExpired.collect {
            container.houseFilter.value = null
            expiredNotice = true
            loggedIn = false
        }
    }

    Box(Modifier.fillMaxSize().background(Pro.c.bg)) {
        if (!loggedIn) {
            LoginScreen(
                vm = viewModel(key = "login-$session", factory = factory),
                sessionExpired = expiredNotice,
                onLoggedIn = {
                    session++
                    expiredNotice = false
                    loggedIn = true
                },
            )
        } else {
            MainTabs(
                container = container,
                factory = factory,
                session = session,
                onLogout = {
                    scope.launch {
                        container.repository.logout()
                        container.houseFilter.value = null
                        expiredNotice = false
                        loggedIn = false
                    }
                },
            )
        }
    }
}

@Composable
private fun MainTabs(container: AppContainer, factory: VmFactory, session: Int, onLogout: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.Overview) }
    BackHandler(enabled = tab != Tab.Overview) { tab = Tab.Overview }

    Box(Modifier.fillMaxSize()) {
        when (tab) {
            Tab.Overview -> OverviewScreen(
                vm = viewModel(key = "overview-$session", factory = factory),
                sessionExpiresAt = container.repository.sessionExpiresAt,
                onOpenTab = { tab = it },
                onOpenHouse = { houseId ->
                    container.houseFilter.value = houseId
                    tab = Tab.Months
                },
                onLogout = onLogout,
            )
            Tab.Pace -> PaceScreen(viewModel(key = "pace-$session", factory = factory))
            Tab.Months -> MonthsScreen(viewModel(key = "months-$session", factory = factory))
            Tab.Arrivals -> ArrivalsScreen(viewModel(key = "arrivals-$session", factory = factory))
        }
        BottomNav(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}
