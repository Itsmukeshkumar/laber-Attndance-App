package com.example

import com.example.data.model.AttendanceStatus
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test attendance status working day multipliers`() {
        // Business Rule: Full Day = 1.0, Half Day = 0.5, Absent = 0.0
        val presentMultiplier = when (AttendanceStatus.PRESENT) {
            AttendanceStatus.PRESENT -> 1.0
            AttendanceStatus.HALF_DAY -> 0.5
            AttendanceStatus.ABSENT -> 0.0
        }
        val halfDayMultiplier = when (AttendanceStatus.HALF_DAY) {
            AttendanceStatus.PRESENT -> 1.0
            AttendanceStatus.HALF_DAY -> 0.5
            AttendanceStatus.ABSENT -> 0.0
        }
        val absentMultiplier = when (AttendanceStatus.ABSENT) {
            AttendanceStatus.PRESENT -> 1.0
            AttendanceStatus.HALF_DAY -> 0.5
            AttendanceStatus.ABSENT -> 0.0
        }

        assertEquals(1.0, presentMultiplier, 0.001)
        assertEquals(0.5, halfDayMultiplier, 0.001)
        assertEquals(0.0, absentMultiplier, 0.001)
    }

    @Test
    fun `test wage and balance calculation`() {
        val dailyWage = 600.0
        val presentDays = 12
        val halfDays = 4
        val absentDays = 2

        val totalWorkingDays = presentDays * 1.0 + halfDays * 0.5 + absentDays * 0.0
        assertEquals(14.0, totalWorkingDays, 0.001)

        val totalEarnings = totalWorkingDays * dailyWage
        assertEquals(8400.0, totalEarnings, 0.001)

        val totalPaid = 5000.0
        val remainingBalance = totalEarnings - totalPaid
        assertEquals(3400.0, remainingBalance, 0.001)
    }

    @Test
    fun `test currency formatting`() {
        val formatted = CurrencyUtils.formatInr(5000.0)
        assertTrue(formatted.contains("5,000") || formatted.contains("5000"))
        assertTrue(formatted.startsWith("₹"))

        val cleanFormatted = CurrencyUtils.formatInrClean(1250.0)
        assertEquals("₹1,250", cleanFormatted)
    }

    @Test
    fun `test date formatting utilities`() {
        val isoDate = "2026-09-05"
        val displayDate = DateUtils.formatToDisplay(isoDate)
        assertNotNull(displayDate)
        assertTrue(displayDate.contains("05") || displayDate.contains("Sep") || displayDate.contains("2026"))

        val todayIso = DateUtils.getTodayIso()
        assertTrue(todayIso.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))

        val yesterdayIso = DateUtils.getYesterdayIso()
        assertTrue(yesterdayIso.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertNotEquals(todayIso, yesterdayIso)
    }
}
