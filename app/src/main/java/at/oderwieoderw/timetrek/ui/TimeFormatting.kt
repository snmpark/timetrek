package at.oderwieoderw.timetrek.ui

import java.util.Locale

object TimeFormatting {
    fun elapsed(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60)
    }

    fun duration(milliseconds: Long): String {
        if (milliseconds in 1..59_999) return "< 1m"
        val minutes = milliseconds / 60_000
        return String.format(Locale.getDefault(), "%dh %02dm", minutes / 60, minutes % 60)
    }

    fun countdown(milliseconds: Long): String {
        val seconds = milliseconds / 1000 + if (milliseconds % 1000 > 0) 1 else 0
        return String.format(Locale.getDefault(), "%dh %02dm %02ds", seconds / 3600, (seconds / 60) % 60, seconds % 60)
    }
}
