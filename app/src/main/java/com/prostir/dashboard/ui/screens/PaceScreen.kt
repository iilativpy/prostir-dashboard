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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostir.dashboard.MONTH_NAMES_UK
import com.prostir.dashboard.data.BookingPace
import com.prostir.dashboard.percentText
import com.prostir.dashboard.ui.PaceViewModel
import com.prostir.dashboard.ui.charts.PaceChart
import com.prostir.dashboard.ui.charts.PaceTable
import com.prostir.dashboard.ui.components.ErrorBlock
import com.prostir.dashboard.ui.components.Glyph
import com.prostir.dashboard.ui.components.ProCard
import com.prostir.dashboard.ui.components.ProIcon
import com.prostir.dashboard.ui.components.RoundIconButton
import com.prostir.dashboard.ui.components.ScreenHeader
import com.prostir.dashboard.ui.components.Skeleton
import com.prostir.dashboard.ui.components.SkeletonCard
import com.prostir.dashboard.ui.components.UpdatedFooter
import com.prostir.dashboard.ui.theme.Pro
import com.prostir.dashboard.visiblePaceYears
import java.time.LocalDate

@Composable
fun PaceScreen(vm: PaceViewModel) {
    val c = Pro.c
    val month by vm.month.collectAsStateWithLifecycle()
    val state by vm.loader.state.collectAsStateWithLifecycle()
    PollWhileVisible(onTick = { vm.refresh(silent = true) })

    Page(refreshing = state.loading && state.data != null, onRefresh = { vm.refresh() }) {
        ScreenHeader("Темп бронювання", subtitle = "Всі будинки")

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Glyph.ChevronLeft, "Попередній місяць", onClick = { vm.step(-1) })
            Text(MONTH_NAMES_UK[month - 1], fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            RoundIconButton(Glyph.ChevronRight, "Наступний місяць", onClick = { vm.step(1) })
        }

        state.error?.let { ErrorBlock(it, state.updatedAt, hasData = state.data != null, onRetry = { vm.refresh() }) }

        val pace = state.data
        if (pace == null) {
            if (state.error == null) {
                SkeletonCard(contentHeight = 160.dp)
                SkeletonCard(contentHeight = 60.dp)
            }
        } else if (pace.years.isEmpty()) {
            ProCard(Modifier.fillMaxWidth(), padding = 20.dp) {
                Text("Немає даних за цей місяць", fontWeight = FontWeight.SemiBold)
                Text("Темп з’явиться, коли за 45 днів до місяця будуть бронювання.", color = c.muted, fontSize = 12.5.sp)
            }
        } else {
            ProCard(Modifier.fillMaxWidth()) {
                PaceChart(pace, height = 200.dp)
                Spacer(Modifier.height(8.dp))
                Legend(pace)
            }
            SummaryTiles(pace)
            ProCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Таблиця", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("% заброньованих ночей", color = c.muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                PaceTable(pace)
                Text(
                    "Скасовані бронювання враховуються до дати скасування, тому старі точки показують стан на той день.",
                    color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        UpdatedFooter(state.updatedAt, failed = state.error != null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(pace: BookingPace) {
    val c = Pro.c
    val years = visiblePaceYears(pace)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        years.forEachIndexed { i, y ->
            val rank = (years.lastIndex - i).coerceAtMost(c.years.lastIndex)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(c.years[rank]))
                Text("${y.year}", color = c.muted, fontSize = 12.sp)
            }
        }
        if (LocalDate.now().monthValue == pace.month) {
            Text("┆ сьогодні, %02d.%02d".format(LocalDate.now().dayOfMonth, LocalDate.now().monthValue), color = c.muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SummaryTiles(pace: BookingPace) {
    val c = Pro.c
    val years = visiblePaceYears(pace)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        years.forEachIndexed { i, y ->
            val rank = (years.lastIndex - i).coerceAtMost(c.years.lastIndex)
            val finished = y.values.lastOrNull() != null
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(c.surface).padding(horizontal = 10.dp, vertical = 9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(c.years[rank]))
                    Text("${y.year}", color = c.muted, fontSize = 12.sp)
                }
                Text(y.finalPercent.percentText(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(if (finished) "підсумок" else "на сьогодні", color = c.muted, fontSize = 11.sp)
            }
        }
    }
}
