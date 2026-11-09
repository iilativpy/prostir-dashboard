package com.prostir.dashboard.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prostir.dashboard.MONTH_NAMES_UK
import com.prostir.dashboard.MONTH_SHORT_UK
import com.prostir.dashboard.data.BookingPace
import com.prostir.dashboard.data.MonthOccupancy
import com.prostir.dashboard.data.Occupancy
import com.prostir.dashboard.fractionalIndex
import com.prostir.dashboard.percentText
import com.prostir.dashboard.ui.components.HouseBadge
import com.prostir.dashboard.ui.theme.Onest
import com.prostir.dashboard.ui.theme.Pro
import com.prostir.dashboard.visiblePaceYears
import java.time.LocalDate
import kotlin.math.roundToInt

private fun axisStyle(color: Color) = TextStyle(fontSize = 10.sp, color = color, fontFamily = Onest)
private fun valueStyle(color: Color) =
    TextStyle(fontSize = 11.sp, color = color, fontFamily = Onest, fontWeight = FontWeight.SemiBold)

private fun DrawScope.label(measurer: TextMeasurer, text: String, style: TextStyle, x: Float, y: Float, anchor: Anchor) {
    val r = measurer.measure(text, style)
    val left = when (anchor) {
        Anchor.Start -> x
        Anchor.Center -> x - r.size.width / 2f
        Anchor.End -> x - r.size.width
    }
    drawText(r, topLeft = Offset(left, y - r.size.height / 2f))
}

private enum class Anchor { Start, Center, End }

/**
 * Booking pace: % of the month booked on each date before it, one line per year.
 * Touch or drag shows a crosshair and a tooltip with all years for that date.
 */
@Composable
fun PaceChart(pace: BookingPace, modifier: Modifier = Modifier, height: Dp = 200.dp, compact: Boolean = false) {
    val c = Pro.c
    val years = remember(pace) { visiblePaceYears(pace) }
    val n = pace.labels.size
    if (n < 2 || years.isEmpty()) return

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var cursor by remember(pace) { mutableStateOf<Int?>(null) }
    var tipWidth by remember { mutableStateOf(0.dp) }

    val left = 28.dp
    val right = if (compact) 10.dp else 34.dp
    val top = if (compact) 8.dp else 18.dp
    val bottom = if (compact) 18.dp else 24.dp
    val newest = years.last()
    val todayFraction = remember(pace) {
        val today = LocalDate.now()
        if (today.monthValue == pace.month) fractionalIndex(pace.offsets, today.dayOfMonth - 1) else null
    }
    val description = remember(pace) {
        val idx = newest.values.indexOfLast { it != null }
        val prev = years.getOrNull(years.lastIndex - 1)
        buildString {
            append("Темп бронювання, ${pace.monthName.lowercase()}. ")
            if (idx >= 0) append("${newest.year}: ${newest.values[idx].percentText()} станом на ${pace.labels[idx]}. ")
            if (prev != null && idx >= 0) append("${prev.year}: ${prev.values.getOrNull(idx).percentText()} на ту саму дату.")
        }
    }

    BoxWithConstraints(
        modifier.fillMaxWidth().height(height).semantics { contentDescription = description },
    ) {
        val plotWidth = maxWidth - left - right
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(pace) {
                    detectTapGestures { p ->
                        val iw = size.width - left.toPx() - right.toPx()
                        val idx = (((p.x - left.toPx()) / iw) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                        cursor = if (cursor == idx) null else idx
                    }
                }
                .pointerInput(pace) {
                    detectHorizontalDragGestures { change, _ ->
                        val iw = size.width - left.toPx() - right.toPx()
                        cursor = (((change.position.x - left.toPx()) / iw) * (n - 1)).roundToInt().coerceIn(0, n - 1)
                    }
                },
        ) {
            val l = left.toPx()
            val t = top.toPx()
            val iw = size.width - l - right.toPx()
            val ih = size.height - t - bottom.toPx()
            fun x(i: Float) = l + iw * i / (n - 1)
            fun y(v: Double) = t + ih * (1f - (v / 100.0).toFloat())

            // grid and Y axis
            val ticks = if (compact) listOf(0, 50, 100) else listOf(0, 25, 50, 75, 100)
            ticks.forEach { tick ->
                drawLine(c.line, Offset(l, y(tick.toDouble())), Offset(size.width - right.toPx(), y(tick.toDouble())), 1.dp.toPx())
                label(measurer, "$tick", axisStyle(c.muted), l - 6.dp.toPx(), y(tick.toDouble()), Anchor.End)
            }
            // X axis: every 3rd (5th when compact) date and the last one
            val step = if (compact) 5 else 3
            for (i in 0 until n) {
                if (i == n - 1 || (i % step == 0 && n - 1 - i >= 3)) {
                    label(measurer, pace.labels[i], axisStyle(c.muted), x(i.toFloat()), size.height - 6.dp.toPx(), Anchor.Center)
                }
            }
            // "today"
            if (todayFraction != null) {
                val tx = x(todayFraction)
                drawLine(
                    c.muted, Offset(tx, t), Offset(tx, t + ih), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                )
                if (!compact) label(measurer, "сьогодні", axisStyle(c.muted), tx + 4.dp.toPx(), t - 8.dp.toPx(), Anchor.Start)
            }
            // series, oldest first so the newest is on top
            years.forEachIndexed { i, year ->
                val rank = (years.lastIndex - i).coerceAtMost(c.years.lastIndex)
                val path = Path()
                var started = false
                year.values.forEachIndexed { idx, v ->
                    if (v != null) {
                        if (!started) {
                            path.moveTo(x(idx.toFloat()), y(v))
                            started = true
                        } else {
                            path.lineTo(x(idx.toFloat()), y(v))
                        }
                    }
                }
                drawPath(
                    path, c.years[rank],
                    style = Stroke(
                        width = (if (rank == 0) 3.dp else 2.dp).toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                        pathEffect = if (rank == 2) PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())) else null,
                    ),
                )
            }
            val cur = cursor
            if (cur != null) {
                drawLine(c.ink, Offset(x(cur.toFloat()), t), Offset(x(cur.toFloat()), t + ih), 1.dp.toPx())
                years.forEachIndexed { i, year ->
                    val v = year.values.getOrNull(cur) ?: return@forEachIndexed
                    val rank = (years.lastIndex - i).coerceAtMost(c.years.lastIndex)
                    drawCircle(c.surface, 7.dp.toPx(), Offset(x(cur.toFloat()), y(v)))
                    drawCircle(c.years[rank], 5.dp.toPx(), Offset(x(cur.toFloat()), y(v)))
                }
            } else {
                val last = newest.values.indexOfLast { it != null }
                if (last >= 0) {
                    val v = newest.values[last]!!
                    drawCircle(c.surface, 7.dp.toPx(), Offset(x(last.toFloat()), y(v)))
                    drawCircle(c.years[0], 5.dp.toPx(), Offset(x(last.toFloat()), y(v)))
                    if (!compact) label(measurer, v.percentText(), valueStyle(c.ink), x(last.toFloat()) - 8.dp.toPx(), y(v) - 12.dp.toPx(), Anchor.End)
                }
            }
            // final values of finished years on the right edge
            if (!compact) {
                // Nudge labels apart when two years end at almost the same value.
                val minGap = 12.dp.toPx()
                var previousY = Float.NEGATIVE_INFINITY
                years.mapNotNull { year -> year.values.lastOrNull()?.let { it to y(it) } }
                    .sortedBy { it.second }
                    .forEach { (v, ly) ->
                        val placed = maxOf(ly, previousY + minGap)
                        previousY = placed
                        label(measurer, v.percentText(), valueStyle(c.ink), x((n - 1).toFloat()) + 6.dp.toPx(), placed, Anchor.Start)
                    }
            }
        }

        val cur = cursor
        if (cur != null) {
            val cx = left + plotWidth * (cur.toFloat() / (n - 1))
            val placeLeft = cx - tipWidth - 12.dp >= 0.dp
            Column(
                Modifier
                    .offset(x = if (placeLeft) cx - tipWidth - 12.dp else cx + 12.dp, y = 4.dp)
                    .onSizeChanged { tipWidth = with(density) { it.width.toDp() } }
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.ink)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("${pace.labels[cur]} · дата бронювання", color = c.bg.copy(alpha = 0.7f), fontSize = 11.sp)
                years.reversed().forEachIndexed { i, year ->
                    val rank = i.coerceAtMost(c.years.lastIndex)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(c.years[rank]))
                        Text(
                            "${year.year}  ${year.values.getOrNull(cur).percentText()}",
                            color = c.bg, fontSize = 12.sp, fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
                val prev = years.getOrNull(years.lastIndex - 1)
                val a = newest.values.getOrNull(cur)
                val b = prev?.values?.getOrNull(cur)
                if (a != null && b != null) {
                    val d = a.roundToInt() - b.roundToInt()
                    Text(
                        "${if (d >= 0) "+" else "−"}${kotlin.math.abs(d)} п.п. до ${prev.year}",
                        color = c.bg.copy(alpha = 0.7f), fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

/** 12 monthly bars. A tick on each bar marks the same month of the previous year when [previous] is given. */
@Composable
fun MonthBars(
    months: List<MonthOccupancy>,
    values: List<Double?>,
    previous: List<Double?>?,
    year: Int,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
    subject: String = "усі будинки",
) {
    val c = Pro.c
    val measurer = rememberTextMeasurer()
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val left = 28.dp
    val right = 4.dp
    val top = 18.dp
    val bottom = 22.dp

    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
            val sel = selected
            if (sel != null && sel in values.indices) {
                val m = months.getOrNull(sel)
                val v = values[sel]
                val prev = previous?.getOrNull(sel)
                Column(Modifier.clip(RoundedCornerShape(12.dp)).background(c.ink).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("${MONTH_NAMES_UK[sel]} $year · $subject", color = c.bg.copy(alpha = 0.7f), fontSize = 11.sp)
                    val extra = buildString {
                        if (m != null && !m.isFinished) append(" · поки що")
                        if (prev != null && v != null) {
                            val d = v.roundToInt() - prev.roundToInt()
                            append(" · ${year - 1}: ${prev.percentText()} (${if (d >= 0) "+" else "−"}${kotlin.math.abs(d)} п.п.)")
                        }
                    }
                    Text("${v.percentText()}$extra", color = c.bg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = "Завантаженість по місяцях $year, $subject" }
                .pointerInput(values) {
                    detectTapGestures { p ->
                        val slot = (size.width - left.toPx() - right.toPx()) / 12f
                        val idx = ((p.x - left.toPx()) / slot).toInt().coerceIn(0, 11)
                        selected = if (selected == idx) null else idx
                    }
                },
        ) {
            val l = left.toPx()
            val t = top.toPx()
            val iw = size.width - l - right.toPx()
            val ih = size.height - t - bottom.toPx()
            val slot = iw / 12f
            val barW = 16.dp.toPx()
            fun y(v: Double) = t + ih * (1f - (v / 100.0).toFloat())

            listOf(0, 50, 100).forEach { tick ->
                drawLine(c.line, Offset(l, y(tick.toDouble())), Offset(size.width - right.toPx(), y(tick.toDouble())), 1.dp.toPx())
                label(measurer, "$tick", axisStyle(c.muted), l - 6.dp.toPx(), y(tick.toDouble()), Anchor.End)
            }
            values.forEachIndexed { i, v ->
                val x = l + slot * i + (slot - barW) / 2f
                val m = months.getOrNull(i)
                if (v != null) {
                    val topY = y(v)
                    val base = y(0.0)
                    val radius = CornerRadius(4.dp.toPx())
                    val path = Path().apply {
                        addRoundRect(RoundRect(x, topY, x + barW, base, radius, radius, CornerRadius.Zero, CornerRadius.Zero))
                    }
                    drawPath(path, color.copy(alpha = if (m != null && !m.isFinished) 0.35f else 1f))
                    if (m?.status == "current") {
                        label(measurer, v.percentText(), valueStyle(c.ink), x + barW / 2f, topY - 8.dp.toPx(), Anchor.Center)
                    }
                }
                val p = previous?.getOrNull(i)
                if (p != null) {
                    drawLine(c.ink, Offset(x - 2.dp.toPx(), y(p)), Offset(x + barW + 2.dp.toPx(), y(p)), 2.dp.toPx(), cap = StrokeCap.Round)
                }
                label(measurer, MONTH_SHORT_UK[i], axisStyle(c.muted), x + barW / 2f, size.height - 6.dp.toPx(), Anchor.Center)
            }
        }
    }
}

/** One heatmap cell: one-colour scale, text flips to the light colour on dark cells. */
@Composable
fun HeatCell(value: Double?, modifier: Modifier = Modifier, dim: Boolean = false, bold: Boolean = false, text: String? = null) {
    val c = Pro.c
    val shape = RoundedCornerShape(6.dp)
    val alpha = if (dim) 0.28f else 1f
    if (value == null && text == null) {
        Box(modifier.height(27.dp).clip(shape).background(c.surface2.copy(alpha = 0.7f * alpha)), contentAlignment = Alignment.Center) {
            Text("—", color = c.muted, fontSize = 12.5.sp)
        }
        return
    }
    if (value == null) {
        Box(modifier.height(27.dp).clip(shape).background(c.surface2.copy(alpha = alpha)), contentAlignment = Alignment.Center) {
            Text(text!!, color = c.ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
        return
    }
    val t = (0.06f + 0.94f * (value / 100.0).toFloat()).coerceIn(0f, 1f)
    val bg = lerp(c.heat0, c.heat1, t)
    val fg = if (value >= c.hotThreshold) c.onHeat1 else c.ink
    Box(modifier.height(27.dp).clip(shape).background(bg.copy(alpha = alpha)), contentAlignment = Alignment.Center) {
        Text(
            text ?: "${value.roundToInt()}", color = fg.copy(alpha = if (dim) 0.5f else 1f), fontSize = 12.5.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Dates as rows, years as columns. The newest year's last known row is bold. */
@Composable
fun PaceTable(pace: BookingPace, modifier: Modifier = Modifier) {
    val c = Pro.c
    val years = remember(pace) { visiblePaceYears(pace) }
    val newest = years.lastOrNull() ?: return
    val lastKnown = newest.values.indexOfLast { it != null }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Дата", color = c.muted, fontSize = 11.sp, modifier = Modifier.width(72.dp))
            years.forEach { Text("${it.year}", color = c.muted, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
        pace.labels.forEachIndexed { i, label ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label, color = if (i == lastKnown) c.ink else c.muted, fontSize = 12.sp,
                    fontWeight = if (i == lastKnown) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.width(72.dp),
                )
                years.forEach { HeatCell(it.values.getOrNull(i), Modifier.weight(1f), bold = i == lastKnown && it === newest) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text("Підсумок", color = c.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(72.dp))
            years.forEach { y ->
                val finished = y.values.lastOrNull() != null
                if (finished) HeatCell(y.finalPercent, Modifier.weight(1f), bold = true)
                else HeatCell(null, Modifier.weight(1f), text = "${y.finalPercent?.roundToInt() ?: "—"}*")
            }
        }
        Text("* на сьогодні. «—» означає майбутню дату.", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Months as rows; columns are "all houses" and each house. [selectedHouseId] dims the other columns. */
@Composable
fun OccupancyMatrix(occupancy: Occupancy, selectedHouseId: Int?, modifier: Modifier = Modifier) {
    val c = Pro.c
    val dimAll = selectedHouseId != null
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Місяць", color = c.muted, fontSize = 11.sp, modifier = Modifier.width(72.dp))
            Text("Усі", color = c.muted, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            occupancy.houses.forEachIndexed { i, _ ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { HouseBadge(i, size = 18.dp) }
            }
        }
        occupancy.months.forEach { m ->
            val current = m.status == "current"
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    MONTH_SHORT_UK.getOrElse(m.month - 1) { "" } + if (m.isFinished) "" else "*",
                    color = if (current) c.ink else c.muted, fontSize = 12.sp,
                    fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.width(72.dp),
                )
                HeatCell(m.percent, Modifier.weight(1f), dim = dimAll, bold = current)
                occupancy.houses.forEach { h ->
                    HeatCell(
                        m.houses.firstOrNull { it.houseId == h.id }?.percent, Modifier.weight(1f),
                        dim = selectedHouseId != null && selectedHouseId != h.id, bold = current,
                    )
                }
            }
        }
        Text(
            "Колонки 1–${occupancy.houses.size}: ${occupancy.houses.joinToString(", ") { it.name }}.\n* місяць ще триває: заброньовано на сьогодні.",
            color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
        )
    }
}
