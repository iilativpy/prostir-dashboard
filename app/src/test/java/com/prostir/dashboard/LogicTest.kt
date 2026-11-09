package com.prostir.dashboard

import com.prostir.dashboard.data.ArrivalItem
import com.prostir.dashboard.data.BookingPace
import com.prostir.dashboard.data.House
import com.prostir.dashboard.data.PaceYear
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogicTest {
    private val pace = BookingPace(
        month = 11,
        labels = listOf("17.09", "22.09", "27.09", "02.10"),
        offsets = listOf(-45, -40, -35, -30),
        years = listOf(
            PaceYear(2025, listOf(0.0, 3.0, 7.0, 11.0), 11.0),
            PaceYear(2026, listOf(0.0, 4.0, 9.4, null), 9.4),
            PaceYear(2024, listOf(0.0, 2.0, 5.0, 8.0), 8.0),
        ),
    )

    @Test fun percentIsRounded() {
        assertEquals("51%", 50.8.percentText())
        assertEquals("—", (null as Double?).percentText())
    }

    @Test fun paceDeltaUsesLastKnownPointOfCurrentYear() {
        val d = paceDelta(pace, 2026)!!
        assertEquals("27.09", d.label)
        assertEquals(9, d.current)
        assertEquals(7, d.previous)
        assertEquals(2, d.points)
        assertEquals(2025, d.previousYear)
    }

    @Test fun paceDeltaNeedsAPreviousYear() {
        assertNull(paceDelta(pace.copy(years = listOf(PaceYear(2026, listOf(1.0), 1.0))), 2026))
    }

    @Test fun visibleYearsAreTheThreeNewestOldestFirst() {
        val many = pace.copy(years = (2022..2026).map { PaceYear(it, listOf(1.0)) })
        assertEquals(listOf(2024, 2025, 2026), visiblePaceYears(many).map { it.year })
    }

    @Test fun phoneIsFormatted() {
        assertEquals("+380 67 123 45 67", formatPhone("+380671234567"))
        assertEquals("0671234567", formatPhone("0671234567"))
        assertNull(formatPhone(" "))
    }

    @Test fun dayMonthDropsTheYear() {
        assertEquals("09.11", dayMonth("2026-11-09"))
    }

    @Test fun fractionalIndexInterpolatesBetweenPoints() {
        val offsets = listOf(0, 5, 10, 29, 30)
        assertEquals(1.6f, fractionalIndex(offsets, 8)!!, 0.001f)
        assertEquals(3f, fractionalIndex(offsets, 29)!!, 0.001f)
        assertNull(fractionalIndex(offsets, 31))
    }

    @Test fun houseStatusPrefersToday() {
        val today = LocalDate.of(2026, 11, 9)
        fun item(type: String, date: String) = ArrivalItem(
            type = type, date = date, reservationId = 1, house = House(3, "Prostir #3"),
            dateFrom = date, dateTo = date,
        )
        assertEquals("заїзд сьогодні", houseStatus(3, listOf(item("check_in", "2026-11-09")), today))
        assertEquals("виїзд сьогодні", houseStatus(3, listOf(item("check_out", "2026-11-09")), today))
        assertEquals("наступний заїзд 12.11", houseStatus(3, listOf(item("check_in", "2026-11-12")), today))
        assertNull(houseStatus(2, listOf(item("check_in", "2026-11-12")), today))
    }
}
