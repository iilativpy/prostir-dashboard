package com.prostir.dashboard

import com.prostir.dashboard.data.ArrivalItem
import com.prostir.dashboard.data.BookingPace
import com.prostir.dashboard.data.PaceYear
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Presentation rules from the design spec, kept free of Android types so they are unit-testable. */

val MONTH_NAMES_UK = listOf(
    "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
    "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень",
)

val MONTH_SHORT_UK = listOf("Січ", "Лют", "Бер", "Кві", "Тра", "Чер", "Лип", "Сер", "Вер", "Жов", "Лис", "Гру")

private val UK = Locale.forLanguageTag("uk")

/** Percentages are shown as whole numbers; the API sends one decimal. */
fun Double?.percentText(): String = this?.let { "${it.roundToInt()}%" } ?: "—"

/** "2026-11-09" -> "09.11" */
fun dayMonth(iso: String): String {
    val p = iso.take(10).split("-")
    return if (p.size == 3) "${p[2]}.${p[1]}" else iso
}

/** "+380671234567" -> "+380 67 123 45 67". Other formats are shown as they came. */
fun formatPhone(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val digits = raw.trim()
    return if (Regex("""\+380\d{9}""").matches(digits)) {
        "${digits.substring(0, 4)} ${digits.substring(4, 6)} ${digits.substring(6, 9)} ${digits.substring(9, 11)} ${digits.substring(11, 13)}"
    } else {
        digits
    }
}

fun sourceLabel(source: String): String = when (source) {
    "site" -> "Сайт"
    "admin" -> "Адмін"
    "booking" -> "Booking"
    "airbnb" -> "Airbnb"
    else -> source.replaceFirstChar { it.uppercase() }
}

fun ArrivalItem.guestName(): String =
    listOfNotNull(guest?.name, guest?.surname).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Гість" }

/** "Сьогодні · пн 9", "Завтра · вт 10", "ср 11". */
fun dayHeader(date: LocalDate, today: LocalDate): String {
    val base = date.format(DateTimeFormatter.ofPattern("EEE d", UK))
    return when (date) {
        today -> "Сьогодні · $base"
        today.plusDays(1) -> "Завтра · $base"
        else -> base
    }
}

/** At most the three newest years, oldest first. Colour and line style follow the position from the newest. */
fun visiblePaceYears(pace: BookingPace): List<PaceYear> = pace.years.sortedBy { it.year }.takeLast(3)

/** How the newest year compares with the previous one at the newest year's last known point. */
data class PaceDelta(val label: String, val current: Int, val previous: Int, val previousYear: Int) {
    val points: Int get() = current - previous
}

fun paceDelta(pace: BookingPace, currentYear: Int): PaceDelta? {
    val years = pace.years.sortedBy { it.year }
    val current = years.lastOrNull { it.year <= currentYear } ?: return null
    val previous = years.lastOrNull { it.year < current.year } ?: return null
    val index = current.values.indexOfLast { it != null }
    if (index < 0) return null
    val now = current.values[index] ?: return null
    val before = previous.values.getOrNull(index) ?: return null
    return PaceDelta(
        label = pace.labels.getOrNull(index) ?: "",
        current = now.roundToInt(),
        previous = before.roundToInt(),
        previousYear = previous.year,
    )
}

/**
 * Position of [offset] (days from the 1st) on the chart axis, as a fractional point index.
 * Points are evenly spaced on screen. Returns null when the day is outside the plotted range.
 */
fun fractionalIndex(offsets: List<Int>, offset: Int): Float? {
    if (offsets.size < 2 || offset < offsets.first() || offset > offsets.last()) return null
    for (i in 0 until offsets.size - 1) {
        val a = offsets[i]
        val b = offsets[i + 1]
        if (offset in a..b) return i + if (b == a) 0f else (offset - a).toFloat() / (b - a)
    }
    return null
}

/** Texts for the "Заїзд / Виїзд" status line under a house tile on the overview. */
fun houseStatus(houseId: Int, items: List<ArrivalItem>, today: LocalDate): String? {
    val own = items.filter { it.house.id == houseId }
    val todayIso = today.toString()
    val checkIn = own.any { it.isCheckIn && it.date == todayIso }
    val checkOut = own.any { !it.isCheckIn && it.date == todayIso }
    return when {
        checkIn && checkOut -> "заїзд і виїзд сьогодні"
        checkIn -> "заїзд сьогодні"
        checkOut -> "виїзд сьогодні"
        else -> own.filter { it.isCheckIn && it.date > todayIso }.minByOrNull { it.date }
            ?.let { "наступний заїзд ${dayMonth(it.date)}" }
    }
}
