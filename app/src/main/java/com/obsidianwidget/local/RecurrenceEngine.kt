package com.obsidianwidget.local

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Deterministic local calendar engine, not an alarm scheduler.
 * Weekday bits 0..6 map Monday..Sunday. Monthly day clamps to month end.
 */
object RecurrenceEngine {
    private fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun isScheduled(rule: RecurrenceRule, date: LocalDate): Boolean {
        val start = LocalDate.parse(rule.startDate)
        if (date.isBefore(start) || rule.interval < 1) return false
        val elapsedDays = ChronoUnit.DAYS.between(start, date)
        return when (rule.frequency) {
            "DAILY", "CUSTOM" -> elapsedDays % rule.interval == 0L
            "WEEKLY" -> {
                val weeks = ChronoUnit.WEEKS.between(
                    start.minusDays((start.dayOfWeek.value - 1).toLong()),
                    date.minusDays((date.dayOfWeek.value - 1).toLong())
                )
                weeks % rule.interval == 0L && date.dayOfWeek == start.dayOfWeek
            }
            "WEEKDAYS" -> {
                val weeks = ChronoUnit.WEEKS.between(
                    start.minusDays((start.dayOfWeek.value - 1).toLong()),
                    date.minusDays((date.dayOfWeek.value - 1).toLong())
                )
                weeks % rule.interval == 0L && (rule.weekdaysMask and bit(date.dayOfWeek)) != 0
            }
            "MONTHLY" -> {
                val months = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), date.withDayOfMonth(1))
                val target = (rule.monthDay ?: start.dayOfMonth).coerceIn(1, date.lengthOfMonth())
                months % rule.interval == 0L && date.dayOfMonth == target
            }
            else -> false
        }
    }

    fun nextOnOrAfter(rule: RecurrenceRule, today: LocalDate): LocalDate? {
        val start = LocalDate.parse(rule.startDate)
        val from = if (today.isBefore(start)) start else today
        for (offset in 0L..3700L) {
            val day = from.plusDays(offset)
            if (isScheduled(rule, day)) return day
        }
        return null
    }
}
