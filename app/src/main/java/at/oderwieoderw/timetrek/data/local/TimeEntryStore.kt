package at.oderwieoderw.timetrek.data.local

import android.content.Context
import at.oderwieoderw.timetrek.domain.TimeEntries
import at.oderwieoderw.timetrek.domain.TimeEntry
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Small on-device store; the active session is saved as soon as Start is pressed. */
class TimeEntryStore(context: Context) {
    private val preferences = context.getSharedPreferences("time_entries", Context.MODE_PRIVATE)

    var activeStart: Long?
        get() = preferences.getLong("active_start", -1L).takeIf { it >= 0L }
        private set(value) {
            preferences.edit().putLong("active_start", value ?: -1L).commit()
        }

    var weeklyGoalMinutes: Int
        get() = preferences.getInt("weekly_goal_minutes", 0)
        set(value) {
            preferences.edit().putInt("weekly_goal_minutes", value).commit()
        }

    fun entries(): List<TimeEntry> {
        val json = try {
            JSONArray(preferences.getString("entries", "[]"))
        } catch (_: JSONException) {
            return emptyList()
        }
        return buildList {
            for (index in 0 until json.length()) {
                try {
                    val item = json.getJSONObject(index)
                    val start = item.getLong("start")
                    val end = item.getLong("end")
                    if (start >= 0 && end > start) add(TimeEntry(start, end))
                } catch (_: JSONException) {
                    // A damaged entry should not hide other saved sessions.
                }
            }
        }
    }

    fun start(now: Long) {
        if (activeStart == null) activeStart = now
    }

    fun stop(now: Long) {
        val start = activeStart ?: return
        if (now <= start) return
        val json = JSONArray()
        entries().forEach { entry ->
            json.put(JSONObject().put("start", entry.start).put("end", entry.end))
        }
        json.put(JSONObject().put("start", start).put("end", now))
        // Save the completed session and clear the active one together.
        preferences.edit().putString("entries", json.toString()).putLong("active_start", -1L).commit()
    }

    fun updateEntry(index: Int, original: TimeEntry, replacement: TimeEntry): Boolean {
        val saved = entries()
        val active = activeStart
        if (saved.getOrNull(index) != original ||
            !TimeEntries.canReplace(saved, index, replacement) ||
            replacement.end > System.currentTimeMillis() ||
            (active != null && replacement.end > active)
        ) return false
        val updated = saved.toMutableList().apply { this[index] = replacement }
        return saveEntries(updated)
    }

    fun addEntry(entry: TimeEntry): Boolean {
        val saved = entries()
        val active = activeStart
        if (!TimeEntries.canAdd(saved, entry) || entry.end > System.currentTimeMillis() ||
            (active != null && entry.end > active)
        ) return false
        return saveEntries(saved + entry)
    }

    fun deleteEntry(index: Int, original: TimeEntry): Boolean {
        val saved = entries()
        if (saved.getOrNull(index) != original) return false
        val updated = saved.toMutableList().apply { removeAt(index) }
        return saveEntries(updated)
    }

    private fun saveEntries(entries: List<TimeEntry>): Boolean {
        val json = JSONArray()
        entries.forEach { entry ->
            json.put(JSONObject().put("start", entry.start).put("end", entry.end))
        }
        return preferences.edit().putString("entries", json.toString()).commit()
    }
}
