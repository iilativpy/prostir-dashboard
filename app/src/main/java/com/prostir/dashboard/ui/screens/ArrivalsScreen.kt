package com.prostir.dashboard.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostir.dashboard.data.ArrivalItem
import com.prostir.dashboard.data.House
import com.prostir.dashboard.dayHeader
import com.prostir.dashboard.dayMonth
import com.prostir.dashboard.formatPhone
import com.prostir.dashboard.guestName
import com.prostir.dashboard.sourceLabel
import com.prostir.dashboard.ui.ArrivalsViewModel
import com.prostir.dashboard.ui.Period
import com.prostir.dashboard.ui.components.Chip
import com.prostir.dashboard.ui.components.ChipKind
import com.prostir.dashboard.ui.components.ErrorBlock
import com.prostir.dashboard.ui.components.Glyph
import com.prostir.dashboard.ui.components.HouseBadge
import com.prostir.dashboard.ui.components.HouseFilter
import com.prostir.dashboard.ui.components.ProCard
import com.prostir.dashboard.ui.components.ProIcon
import com.prostir.dashboard.ui.components.ScreenHeader
import com.prostir.dashboard.ui.components.Segmented
import com.prostir.dashboard.ui.components.SkeletonCard
import com.prostir.dashboard.ui.components.UpdatedFooter
import com.prostir.dashboard.ui.theme.Pro
import java.time.LocalDate

@Composable
fun ArrivalsScreen(vm: ArrivalsViewModel) {
    val c = Pro.c
    val period by vm.period.collectAsStateWithLifecycle()
    val state by vm.loader.state.collectAsStateWithLifecycle()
    val filter by vm.houseFilter.collectAsStateWithLifecycle()
    val houses by vm.houses.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf(setOf<Int>()) }
    PollWhileVisible(onTick = { vm.refresh(silent = true) })

    val today = LocalDate.now()
    val items = state.data?.items.orEmpty().filter { filter == null || it.house.id == filter }

    Page(refreshing = state.loading && state.data != null, onRefresh = { vm.refresh() }) {
        ScreenHeader("Заїзди й виїзди", subtitle = "Хто приїжджає й виїжджає")
        Segmented(Period.entries.map { it.label }, selected = period.ordinal, onSelect = { vm.selectPeriod(Period.entries[it]) })
        if (houses.isNotEmpty()) HouseFilter(houses, selectedId = filter, onSelect = { vm.houseFilter.value = it })

        state.error?.let { ErrorBlock(it, state.updatedAt, hasData = state.data != null, onRetry = { vm.refresh() }) }

        if (state.data == null) {
            if (state.error == null) {
                SkeletonCard(contentHeight = 80.dp)
                SkeletonCard(contentHeight = 80.dp)
            }
        } else if (items.isEmpty()) {
            EmptyDay(period, houses.firstOrNull { it.id == filter }, onShowWeek = { vm.selectPeriod(Period.Week) })
        } else if (period == Period.Week) {
            items.groupBy { it.date }.toSortedMap().forEach { (date, dayItems) ->
                Text(
                    dayHeader(LocalDate.parse(date), today).uppercase(),
                    color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                dayItems.forEach { item ->
                    val open = item.reservationId * 2 + (if (item.isCheckIn) 1 else 0) in expanded
                    val key = item.reservationId * 2 + (if (item.isCheckIn) 1 else 0)
                    if (open) {
                        RecordCard(item, houseIndex(houses, item.house.id), onCollapse = { expanded = expanded - key })
                    } else {
                        DenseRow(item, houseIndex(houses, item.house.id), onClick = { expanded = expanded + key })
                    }
                }
            }
        } else {
            items.forEach { RecordCard(it, houseIndex(houses, it.house.id), onCollapse = null) }
        }
        UpdatedFooter(state.updatedAt, failed = state.error != null)
    }
}

@Composable
private fun EmptyDay(period: Period, house: House?, onShowWeek: () -> Unit) {
    val c = Pro.c
    val what = when (period) {
        Period.Today -> "Сьогодні заїздів і виїздів немає"
        Period.Tomorrow -> "Завтра заїздів і виїздів немає"
        Period.Week -> "На цьому тижні заїздів і виїздів немає"
    }
    ProCard(Modifier.fillMaxWidth(), padding = 20.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(c.surface2), contentAlignment = Alignment.Center) {
                ProIcon(Glyph.Calendar, c.ink, size = 24.dp)
            }
            Text(what, fontWeight = FontWeight.SemiBold)
            if (house != null) Text("Фільтр: ${house.name}", color = c.muted, fontSize = 12.5.sp)
            if (period != Period.Week) {
                Box(
                    Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(c.surface2)
                        .clickable(role = Role.Button, onClick = onShowWeek).padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Показати тиждень", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecordCard(item: ArrivalItem, houseIndex: Int, onCollapse: (() -> Unit)?) {
    val c = Pro.c
    val context = LocalContext.current
    val phone = formatPhone(item.guest?.phone)
    ProCard(Modifier.fillMaxWidth(), onClick = onCollapse) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeTag(item.isCheckIn)
                HouseBadge(houseIndex)
                Text(item.house.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Chip(sourceLabel(item.source), ChipKind.Mute)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Fact("Заїзд", dayMonth(item.dateFrom))
                Fact("Виїзд", dayMonth(item.dateTo))
                Row {
                    Text("${item.nights}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(" ноч. · ", color = c.muted, fontSize = 13.sp)
                    Text(item.guests?.toString() ?: "—", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(" гост.", color = c.muted, fontSize = 13.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(item.guestName(), fontWeight = FontWeight.SemiBold)
                    if (phone != null) Text(phone, color = c.muted, fontSize = 13.sp)
                }
                if (phone != null) {
                    Row(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(c.accent)
                            .clickable(role = Role.Button, onClickLabel = "Подзвонити") { dial(context, item.guest?.phone) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ProIcon(Glyph.Phone, c.onAccent, size = 16.dp)
                        Text("Подзвонити", color = c.onAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (item.awaitingOfflinePayment) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.warnSoft).padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProIcon(Glyph.Alert, c.warn, size = 16.dp)
                    Text("Не доплачено офлайн", color = c.warn, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            if (!item.comment.isNullOrBlank()) Note("Коментар гостя", item.comment, c.surface2, c.muted)
            if (!item.adminComment.isNullOrBlank()) Note("Коментар адміна", item.adminComment, c.accentSoft, c.accentInk)
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    val c = Pro.c
    Row {
        Text("$label ", color = c.muted, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun Note(title: String, text: String, background: androidx.compose.ui.graphics.Color, titleColor: androidx.compose.ui.graphics.Color) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(background).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(title, color = titleColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(text, fontSize = 13.sp)
    }
}

@Composable
private fun TypeTag(checkIn: Boolean) {
    val c = Pro.c
    Row(
        Modifier.clip(CircleShape).background(if (checkIn) c.accentSoft else c.surface2).padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        val tint = if (checkIn) c.accentInk else c.ink
        ProIcon(if (checkIn) Glyph.CheckIn else Glyph.CheckOut, tint, size = 14.dp)
        Text(if (checkIn) "Заїзд" else "Виїзд", color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DenseRow(item: ArrivalItem, houseIndex: Int, onClick: () -> Unit) {
    val c = Pro.c
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface)
            .clickable(role = Role.Button, onClickLabel = "Показати деталі", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HouseBadge(houseIndex)
        Column(Modifier.weight(1f)) {
            Text("${item.house.name} · ${item.guestName()}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${if (item.isCheckIn) "заїзд" else "виїзд"} · ${dayMonth(item.dateFrom)} → ${dayMonth(item.dateTo)} · ${item.nights} ноч. · ${item.guests ?: "—"} гост. · ${sourceLabel(item.source)}",
                color = c.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        ProIcon(Glyph.ChevronRight, c.muted, size = 18.dp)
    }
}

private fun dial(context: android.content.Context, phone: String?) {
    if (phone.isNullOrBlank()) return
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")))
    } catch (_: ActivityNotFoundException) {
        // No dialer app (tablet, emulator without one): nothing to do.
    }
}
