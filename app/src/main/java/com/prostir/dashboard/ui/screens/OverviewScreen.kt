package com.prostir.dashboard.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostir.dashboard.MONTH_NAMES_UK
import com.prostir.dashboard.data.Arrivals
import com.prostir.dashboard.data.MonthOccupancy
import com.prostir.dashboard.data.Occupancy
import com.prostir.dashboard.houseStatus
import com.prostir.dashboard.paceDelta
import com.prostir.dashboard.percentText
import com.prostir.dashboard.ui.OverviewViewModel
import com.prostir.dashboard.ui.charts.PaceChart
import com.prostir.dashboard.ui.components.Chip
import com.prostir.dashboard.ui.components.ChipKind
import com.prostir.dashboard.ui.components.ErrorBlock
import com.prostir.dashboard.ui.components.Glyph
import com.prostir.dashboard.ui.components.HouseBadge
import com.prostir.dashboard.ui.components.ProCard
import com.prostir.dashboard.ui.components.Ring
import com.prostir.dashboard.ui.components.RoundIconButton
import com.prostir.dashboard.ui.components.ScreenHeader
import com.prostir.dashboard.ui.components.Skeleton
import com.prostir.dashboard.ui.components.SkeletonCard
import com.prostir.dashboard.ui.components.Tab
import com.prostir.dashboard.ui.components.UpdatedFooter
import com.prostir.dashboard.ui.theme.Pro
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun OverviewScreen(
    vm: OverviewViewModel,
    sessionExpiresAt: String?,
    onOpenTab: (Tab) -> Unit,
    onOpenHouse: (houseId: Int) -> Unit,
    onLogout: () -> Unit,
) {
    val c = Pro.c
    val occ by vm.occupancy.state.collectAsStateWithLifecycle()
    val pace by vm.pace.state.collectAsStateWithLifecycle()
    val arrivals by vm.arrivals.state.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    var menuOpen by remember { mutableStateOf(false) }

    PollWhileVisible(onTick = { vm.refresh(silent = true) })

    val refreshing = (occ.loading && occ.data != null) || (pace.loading && pace.data != null) || (arrivals.loading && arrivals.data != null)
    val error = occ.error ?: pace.error ?: arrivals.error
    val updatedAt = listOfNotNull(occ.updatedAt, pace.updatedAt, arrivals.updatedAt).minOrNull()

    Page(refreshing = refreshing, onRefresh = { vm.refresh() }) {
        ScreenHeader(
            title = "${MONTH_NAMES_UK[today.monthValue - 1]} ${today.year}",
            subtitle = "Всі будинки",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                RoundIconButton(Glyph.Refresh, "Оновити", onClick = { vm.refresh() })
                Box {
                    RoundIconButton(Glyph.Dots, "Меню", onClick = { menuOpen = true })
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = c.surface) {
                        DropdownMenuItem(text = { Text("Оновити") }, onClick = { menuOpen = false; vm.refresh() })
                        DropdownMenuItem(
                            text = { Text("Вийти з акаунта", color = c.bad) },
                            onClick = { menuOpen = false; onLogout() },
                        )
                        Text(
                            "Сеанс дійсний до ${sessionExpiresAt?.let(::expiryDate) ?: "—"}",
                            color = c.muted, fontSize = 11.5.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        if (error != null) {
            ErrorBlock(error, updatedAt, hasData = occ.data != null, onRetry = { vm.refresh() })
        }

        val occData = occ.data
        if (occData == null) {
            if (occ.isInitialLoading || error == null) {
                SkeletonCard(contentHeight = 40.dp)
                SkeletonCard(contentHeight = 60.dp)
                SkeletonCard(contentHeight = 80.dp)
            }
        } else {
            val month = occData.months.firstOrNull { it.status == "current" } ?: occData.months.getOrNull(today.monthValue - 1)
            if (month != null) AverageCard(month, pace.data, today)
            HouseTiles(occData, month, arrivals.data, today, onOpenHouse)
        }

        val paceData = pace.data
        ProCard(Modifier.fillMaxWidth(), onClick = { onOpenTab(Tab.Pace) }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Темп бронювання", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(MONTH_NAMES_UK[today.monthValue - 1], color = c.muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            if (paceData == null) Skeleton(Modifier.fillMaxWidth().height(120.dp)) else PaceChart(paceData, height = 120.dp, compact = true)
        }

        UpdatedFooter(updatedAt, failed = error != null)
    }
}

@Composable
private fun AverageCard(month: MonthOccupancy, pace: com.prostir.dashboard.data.BookingPace?, today: LocalDate) {
    val c = Pro.c
    ProCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Ring((month.percent ?: 0.0).roundToInt(), size = 86.dp, stroke = 10.dp, color = c.accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Середня завантаженість", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text("${month.nightsBooked} з ${month.nightsAvailable} ночей", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                val delta = pace?.let { paceDelta(it, today.year) }
                if (delta != null) {
                    val p = delta.points
                    val (text, kind) = when {
                        p > 0 -> "▲ +$p п.п. до ${delta.previousYear} на ${delta.label}" to ChipKind.Good
                        p < 0 -> "▼ −${abs(p)} п.п. до ${delta.previousYear} на ${delta.label}" to ChipKind.Warn
                        else -> "Як у ${delta.previousYear} на ${delta.label}" to ChipKind.Mute
                    }
                    Chip(text, kind)
                }
            }
        }
    }
}

@Composable
private fun HouseTiles(occ: Occupancy, month: MonthOccupancy?, arrivals: Arrivals?, today: LocalDate, onOpenHouse: (Int) -> Unit) {
    val c = Pro.c
    occ.houses.withIndex().chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { (i, house) ->
                val percent = month?.houses?.firstOrNull { it.houseId == house.id }?.percent
                ProCard(Modifier.weight(1f), onClick = { onOpenHouse(house.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HouseBadge(i)
                        Text(house.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(percent.percentText(), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                        Ring((percent ?: 0.0).roundToInt(), size = 40.dp, stroke = 6.dp, color = c.houses[i % c.houses.size], showText = false)
                    }
                    val status = arrivals?.let { houseStatus(house.id, it.items, today) }
                    Text(status ?: " ", color = c.muted, fontSize = 12.5.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

private fun expiryDate(iso: String): String = try {
    val d = java.time.OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
    "%02d.%02d.%d".format(d.dayOfMonth, d.monthValue, d.year)
} catch (e: Exception) {
    runCatching { Instant.parse(iso).toString().take(10) }.getOrDefault("—")
}
