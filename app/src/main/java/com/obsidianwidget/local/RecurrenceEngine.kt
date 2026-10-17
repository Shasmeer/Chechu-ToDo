package com.obsidianwidget.local

import java.time.DayOfWeek
import java.time.LocalDate

/** Pure recurrence rule calculation; never depends on wall clock or Android APIs. */
object RecurrenceEngine {
    fun next(rule: String, scheduled: LocalDate?, today: LocalDate): LocalDate? {
        if (rule == "NONE") return null
        val from = if (scheduled != null && scheduled.isAfter(today)) scheduled else today
        return when (rule) {
            "DAILY" -> from.plusDays(1)
            "WEEKLY" -> from.plusWeeks(1)
            "WEEKDAYS" -> {
                var candidate = from.plusDays(1)
                while (candidate.dayOfWeek == DayOfWeek.SATURDAY ||
                    candidate.dayOfWeek == DayOfWeek.SUNDAY) candidate = candidate.plusDays(1)
                candidate
            }
            else -> null
        }
    }
}
