package com.obsidianwidget.local

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RecurrenceEngineTest {
    @Test fun dailyProgression() {
        val today = LocalDate.of(2026, 10, 17)
        assertEquals(today.plusDays(1), RecurrenceEngine.next("DAILY", null, today))
    }
    @Test fun weekdaysSkipWeekend() {
        val friday = LocalDate.of(2026, 10, 16)
        assertEquals(LocalDate.of(2026, 10, 19), RecurrenceEngine.next("WEEKDAYS", friday, friday))
    }
    @Test fun weeklyKeepsFutureSchedule() {
        val today = LocalDate.of(2026, 10, 17)
        val later = today.plusDays(2)
        assertEquals(later.plusWeeks(1), RecurrenceEngine.next("WEEKLY", later, today))
    }
    @Test fun overdueRepeatAdvancesPastToday() {
        val today = LocalDate.of(2026, 10, 17)
        assertEquals(today.plusDays(1), RecurrenceEngine.next("DAILY", today.minusDays(8), today))
    }
    @Test fun nonRecurringIsNull() {
        val today = LocalDate.of(2026, 10, 17)
        assertNull(RecurrenceEngine.next("NONE", today, today))
    }
}
