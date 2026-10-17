package com.obsidianwidget.local

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RecurrenceEngineTest {
    private fun rule(frequency: String, start: String = "2026-10-17", every: Int = 1, mask: Int = 0, monthDay: Int? = null) =
        RecurrenceRule(frequency = frequency, startDate = start, interval = every,
            weekdaysMask = mask, monthDay = monthDay, zoneId = "Asia/Qatar")

    @Test fun everySecondDay() {
        val r = rule("DAILY", every = 2)
        assertTrue(RecurrenceEngine.isScheduled(r, LocalDate.parse("2026-10-17")))
        assertFalse(RecurrenceEngine.isScheduled(r, LocalDate.parse("2026-10-18")))
        assertTrue(RecurrenceEngine.isScheduled(r, LocalDate.parse("2026-10-19")))
    }

    @Test fun weekdaysAreNotWeekends() {
        val r = rule("WEEKDAYS", mask = 0b0011111)
        assertFalse(RecurrenceEngine.isScheduled(r, LocalDate.parse("2026-10-17")))
        assertTrue(RecurrenceEngine.isScheduled(r, LocalDate.parse("2026-10-19")))
        assertFalse(RecurrenceEngine.isScheduled(r, LocalDate.parse("2026-10-24")))
    }

    @Test fun monthlyClampsToLastDay() {
        val r = rule("MONTHLY", start = "2026-01-31", monthDay = 31)
        assertTrue(RecurrenceEngine.isScheduled(r, LocalDate.parse("2026-02-28")))
        assertFalse(RecurrenceEngine.isScheduled(r, LocalDate.parse("2026-02-27")))
    }

    @Test fun weeklyScheduleAnchorsToStart() {
        val r = rule("WEEKLY", start = "2026-10-17", every = 2)
        assertTrue(RecurrenceEngine.isScheduled(r, LocalDate.parse("2026-10-17")))
        assertFalse(RecurrenceEngine.isScheduled(r, LocalDate.parse("2026-10-24")))
        assertTrue(RecurrenceEngine.isScheduled(r, LocalDate.parse("2026-10-31")))
    }

    @Test fun nextOnOrAfterNeverReturnsEarlierDate() {
        val r = rule("WEEKDAYS", mask = 0b0011111)
        assertEquals(LocalDate.parse("2026-10-19"), RecurrenceEngine.nextOnOrAfter(r, LocalDate.parse("2026-10-17")))
    }
}
