package com.prostir.dashboard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.prostir.dashboard.data.House
import com.prostir.dashboard.ui.Loadable
import kotlinx.coroutines.delay

/** Scrollable page with pull-to-refresh. Leaves room at the bottom for the floating navigation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Page(refreshing: Boolean, onRefresh: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp + navBar),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/**
 * "Real time" without push: refresh when the screen comes back from the background and then every minute
 * while it is visible. The first start is skipped because the ViewModel already loaded.
 */
@Composable
fun PollWhileVisible(onTick: () -> Unit, intervalMillis: Long = 60_000) {
    val owner = LocalLifecycleOwner.current
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!first) onTick()
            first = false
            while (true) {
                delay(intervalMillis)
                onTick()
            }
        }
    }
}

/** Slot (colour and number) of a house. Falls back to the id order when the house list is not loaded yet. */
fun houseIndex(houses: List<House>, id: Int): Int =
    houses.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: (id - 1).coerceAtLeast(0)

val Loadable<*>.hasError: Boolean get() = error != null
