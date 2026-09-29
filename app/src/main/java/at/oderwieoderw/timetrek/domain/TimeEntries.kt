package at.oderwieoderw.timetrek.domain

import java.util.Calendar

data class TimeEntry(val start: Long, val end: Long)

object TimeEntries {
    fun dayBounds(date: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return start to calendar.timeInMillis
    }

    fun overlap(entry: TimeEntry, dayStart: Long, nextDayStart: Long): Long =
        (minOf(entry.end, nextDayStart) - maxOf(entry.start, dayStart)).coerceAtLeast(0L)

    fun totalForDay(entries: List<TimeEntry>, date: Long): Long {
        val (start, end) = dayBounds(date)
        return entries.sumOf { overlap(it, start, end) }
    }

    fun weekBounds(date: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply { timeInMillis = dayBounds(date).first }
        val daysSinceWeekStart = (calendar.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek + 7) % 7
        calendar.add(Calendar.DAY_OF_MONTH, -daysSinceWeekStart)
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 7)
        return start to calendar.timeInMillis
    }

    fun totalForWeek(entries: List<TimeEntry>, date: Long): Long {
        val (start, end) = weekBounds(date)
        return entries.sumOf { overlap(it, start, end) }
    }

    fun canReplace(entries: List<TimeEntry>, index: Int, replacement: TimeEntry): Boolean {
        if (index !in entries.indices || replacement.start < 0 || replacement.end <= replacement.start) return false
        return entries.withIndex().none { (otherIndex, entry) ->
            otherIndex != index && overlap(replacement, entry.start, entry.end) > 0L
        }
    }
}
