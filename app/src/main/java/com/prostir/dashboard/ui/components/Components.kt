package com.prostir.dashboard.ui.components

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostir.dashboard.data.ApiError
import com.prostir.dashboard.data.House
import com.prostir.dashboard.ui.theme.Pro
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CardShape = RoundedCornerShape(24.dp)
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

fun formatClock(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(TimeFormat)

@Composable
fun ProCard(
    modifier: Modifier = Modifier,
    padding: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier.clip(CardShape).background(Pro.c.surface)
    Column(
        modifier = (if (onClick != null) base.clickable(role = Role.Button, onClick = onClick) else base).padding(padding),
        content = content,
    )
}

enum class ChipKind { Accent, Good, Warn, Bad, Mute }

@Composable
fun Chip(text: String, kind: ChipKind = ChipKind.Accent, modifier: Modifier = Modifier) {
    val c = Pro.c
    val (bg, fg) = when (kind) {
        ChipKind.Accent -> c.accentSoft to c.accentInk
        ChipKind.Good -> c.goodSoft to c.good
        ChipKind.Warn -> c.warnSoft to c.warn
        ChipKind.Bad -> c.badSoft to c.bad
        ChipKind.Mute -> c.surface2 to c.muted
    }
    Text(
        text = text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** House colour + number. Colour is never the only carrier of which house it is. */
@Composable
fun HouseBadge(index: Int, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val c = Pro.c
    Box(
        modifier.size(size).clip(CircleShape).background(c.houses[index.coerceAtLeast(0) % c.houses.size]),
        contentAlignment = Alignment.Center,
    ) {
        Text("${index + 1}", color = c.houseText, fontSize = (size.value * 0.55f).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Pro.c
    Row(
        modifier.fillMaxWidth().clip(CircleShape).background(c.surface2).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .then(if (on) Modifier.shadow(1.dp, CircleShape) else Modifier)
                    .clip(CircleShape)
                    .background(if (on) c.surface else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) c.ink else c.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Row of chips: "Усі" and one per house. [houseIndex] maps a house id to its colour/number slot. */
@Composable
fun HouseFilter(
    houses: List<House>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(selected = selectedId == null, dimmed = false, onClick = { onSelect(null) }, label = "Усі", leading = null)
        houses.forEachIndexed { i, h ->
            val on = selectedId == h.id
            FilterChip(
                selected = on,
                dimmed = selectedId != null && !on,
                onClick = { onSelect(if (on) null else h.id) },
                label = if (on) h.name else null,
                leading = { HouseBadge(i) },
                description = h.name,
            )
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    label: String?,
    leading: (@Composable () -> Unit)?,
    description: String? = null,
) {
    val c = Pro.c
    Row(
        Modifier
            .heightIn(min = 48.dp)
            .alpha(if (dimmed) 0.55f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(c.surface)
                .border(BorderStroke(1.5.dp, if (selected) c.ink else Color.Transparent), RoundedCornerShape(17.dp))
                .selectable(selected = selected, role = Role.Button, onClick = onClick)
                .semantics { if (description != null) contentDescription = description }
                .padding(start = if (leading != null) 6.dp else 14.dp, end = if (label != null && leading != null) 12.dp else if (leading != null) 6.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leading?.invoke()
            if (label != null) Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.ink, maxLines = 1)
        }
    }
}

@Composable
fun Ring(percent: Int, size: Dp, stroke: Dp, color: Color, modifier: Modifier = Modifier, showText: Boolean = true) {
    val c = Pro.c
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arc = Size(this.size.width - sw, this.size.height - sw)
            drawArc(c.surface2, 0f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arc, style = Stroke(sw))
            drawArc(
                color, -90f, 360f * percent.coerceIn(0, 100) / 100f, false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arc,
                style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
        if (showText) Text("$percent%", fontSize = (size.value / 4.2f).sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}

@Composable
fun Skeleton(modifier: Modifier = Modifier) {
    val c = Pro.c
    val reduce = rememberReducedMotion()
    val alpha = if (reduce) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val a by transition.animateFloat(
            initialValue = 0.45f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha",
        )
        a
    }
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(c.surface2.copy(alpha = alpha)))
}

@Composable
fun SkeletonCard(contentHeight: Dp = 90.dp, modifier: Modifier = Modifier) {
    ProCard(modifier) {
        Skeleton(Modifier.height(12.dp).fillMaxWidth(0.4f))
        Spacer(Modifier.height(10.dp))
        Skeleton(Modifier.height(36.dp).fillMaxWidth(0.55f))
        Spacer(Modifier.height(10.dp))
        Skeleton(Modifier.height(contentHeight).fillMaxWidth())
    }
}

/** Footer under every data screen: green dot when fresh, grey when stale, plus the Booking delay note. */
@Composable
fun UpdatedFooter(updatedAt: Long?, failed: Boolean, modifier: Modifier = Modifier) {
    val c = Pro.c
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(15_000)
            value = System.currentTimeMillis()
        }
    }
    val fresh = updatedAt != null && !failed && now - updatedAt < 120_000
    val text = when {
        updatedAt == null -> "Завантаження…"
        fresh -> "Оновлено щойно · Booking з’являється до 15 хв"
        else -> "Оновлено о ${formatClock(updatedAt)} · Booking з’являється до 15 хв"
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(if (fresh) c.good else c.muted))
        Spacer(Modifier.width(6.dp))
        Text(text, color = c.muted, fontSize = 12.sp)
    }
}

/** Network / server problem. With old data on screen it is a slim banner, without data a full card. */
@Composable
fun ErrorBlock(error: ApiError, updatedAt: Long?, hasData: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pro.c
    if (error is ApiError.SessionExpired) return // handled by the app root (back to login)
    val title = when (error) {
        ApiError.Server -> "Сервер тимчасово недоступний"
        ApiError.Network -> "Немає з’єднання"
        else -> "Не вдалося завантажити дані"
    }
    val body = when {
        updatedAt != null -> "Показуємо дані станом на ${formatClock(updatedAt)}."
        error is ApiError.Unknown -> error.message ?: "Спробуйте ще раз."
        else -> "Перевірте мережу й спробуйте ще раз."
    }
    if (hasData) {
        Row(
            modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.warnSoft).padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProIcon(Glyph.WifiOff, c.warn, size = 20.dp)
            Column(Modifier.weight(1f)) {
                Text(title, color = c.warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(body, color = c.warn, fontSize = 12.sp)
            }
            Box(Modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                Text("Повторити", color = c.warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        ProCard(modifier.fillMaxWidth(), padding = 20.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(c.surface2), contentAlignment = Alignment.Center) {
                    ProIcon(Glyph.WifiOff, c.ink, size = 24.dp)
                }
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(body, color = c.muted, fontSize = 12.5.sp, textAlign = TextAlign.Center)
                Box(
                    Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(16.dp)).background(c.surface2)
                        .clickable(role = Role.Button, onClick = onRetry).padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Повторити", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** 40 dp round button with a 48 dp touch target. */
@Composable
fun RoundIconButton(glyph: Glyph, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pro.c
    Box(modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(c.surface2), contentAlignment = Alignment.Center) {
            ProIcon(glyph, c.ink, contentDescription = contentDescription)
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) Text(subtitle, color = Pro.c.muted, fontSize = 12.5.sp)
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        actions()
    }
}

enum class Tab(val label: String, val glyph: Glyph) {
    Overview("Огляд", Glyph.Home),
    Pace("Темп", Glyph.Trend),
    Months("Місяці", Glyph.Grid),
    Arrivals("Заїзди", Glyph.Swap),
}

/** Floating pill navigation. The active tab shows icon and label, the others only the icon. */
@Composable
fun BottomNav(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = Pro.c
    val activeBg = if (c.isDark) Color(0xFF0F161D) else Color.White
    val activeFg = if (c.isDark) Color.White else c.accent
    val idleFg = if (c.isDark) Color(0xB30F161D) else Color(0xB8FFFFFF)
    Row(
        modifier
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(c.accent)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Tab.entries.forEach { tab ->
            val on = tab == selected
            Row(
                Modifier
                    .weight(if (on) 2.2f else 1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (on) activeBg else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(tab) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                ProIcon(tab.glyph, if (on) activeFg else idleFg, contentDescription = tab.label)
                if (on) {
                    Spacer(Modifier.width(6.dp))
                    Text(tab.label, color = activeFg, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}
