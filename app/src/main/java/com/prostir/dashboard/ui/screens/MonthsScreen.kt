package com.prostir.dashboard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostir.dashboard.data.Occupancy
import com.prostir.dashboard.percentText
import com.prostir.dashboard.ui.MonthsViewModel
import com.prostir.dashboard.ui.charts.MonthBars
import com.prostir.dashboard.ui.charts.OccupancyMatrix
import com.prostir.dashboard.ui.components.Chip
import com.prostir.dashboard.ui.components.ChipKind
import com.prostir.dashboard.ui.components.ErrorBlock
import com.prostir.dashboard.ui.components.HouseFilter
import com.prostir.dashboard.ui.components.ProCard
import com.prostir.dashboard.ui.components.ScreenHeader
import com.prostir.dashboard.ui.components.Segmented
import com.prostir.dashboard.ui.components.SkeletonCard
import com.prostir.dashboard.ui.components.UpdatedFooter
import com.prostir.dashboard.ui.theme.Pro
import java.time.LocalDate

@Composable
fun MonthsScreen(vm: MonthsViewModel) {
    val c = Pro.c
    val year by vm.year.collectAsStateWithLifecycle()
    val state by vm.loader.state.collectAsStateWithLifecycle()
    val filter by vm.houseFilter.collectAsStateWithLifecycle()
    PollWhileVisible(onTick = { vm.refresh(silent = true) })

    val occ = state.data
    // Years come from the API: every year with data. Until the first answer only the current year is known.
    val years = remember(occ) { occ?.comparison?.map { it.year }?.sorted()?.ifEmpty { null } ?: listOf(LocalDate.now().year) }

    Page(refreshing = state.loading && occ != null, onRefresh = { vm.refresh() }) {
        ScreenHeader("Завантаженість", subtitle = "Роки й будинки")
        Segmented(years.map { "$it" }, selected = years.indexOf(year).coerceAtLeast(0), onSelect = { vm.selectYear(years[it]) })
        if (occ != null) {
            HouseFilter(occ.houses, selectedId = filter, onSelect = { vm.houseFilter.value = it })
        }

        state.error?.let { ErrorBlock(it, state.updatedAt, hasData = occ != null, onRetry = { vm.refresh() }) }

        if (occ == null) {
            if (state.error == null) {
                SkeletonCard(contentHeight = 40.dp)
                SkeletonCard(contentHeight = 200.dp)
            }
        } else {
            AverageCard(occ, year)
            ProCard(Modifier.fillMaxWidth()) {
                Text("Будинок × місяць", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                OccupancyMatrix(occ, filter)
            }
            BarsCard(occ, year, filter)
        }
        UpdatedFooter(state.updatedAt, failed = state.error != null)
    }
}

@Composable
private fun AverageCard(occ: Occupancy, year: Int) {
    val c = Pro.c
    val previous = occ.comparison.firstOrNull { it.year == year - 1 }
    val beforeThat = occ.comparison.firstOrNull { it.year == year - 2 }
    ProCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Середній за $year, завершені місяці", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(occ.averagePercent.percentText(), fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                previous?.let { Chip("${it.year} за рік: ${it.averagePercent.percentText()}", ChipKind.Mute) }
                beforeThat?.let { Text("${it.year} за рік: ${it.averagePercent.percentText()}", color = c.muted, fontSize = 12.sp) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BarsCard(occ: Occupancy, year: Int, filter: Int?) {
    val c = Pro.c
    val houseIndex = occ.houses.indexOfFirst { it.id == filter }
    val house = occ.houses.getOrNull(houseIndex)
    val values = occ.months.map { m ->
        if (house == null) m.percent else m.houses.firstOrNull { it.houseId == house.id }?.percent
    }
    // The API compares with other years only in total, so the ticks disappear when one house is selected.
    val previous = if (house == null) occ.comparison.firstOrNull { it.year == year - 1 }?.months else null
    val color = if (house != null) c.houses[houseIndex % c.houses.size] else c.years[0]
    ProCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("По місяцях", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("% зайнятих ночей", color = c.muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        MonthBars(
            months = occ.months, values = values, previous = previous, year = year, color = color,
            subject = house?.name ?: "усі будинки",
        )
        FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
                Text(if (house != null) "$year, ${house.name}" else "$year", color = c.muted, fontSize = 12.sp)
            }
            if (previous != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.width(12.dp).height(2.dp).background(c.ink))
                    Text("${year - 1}, той самий місяць", color = c.muted, fontSize = 12.sp)
                }
            } else if (house != null) {
                Text("порівняння з минулими роками лише для всіх будинків", color = c.muted, fontSize = 12.sp)
            }
            Text("поточний і майбутні місяці напівпрозорі", color = c.muted, fontSize = 12.sp)
        }
    }
}
