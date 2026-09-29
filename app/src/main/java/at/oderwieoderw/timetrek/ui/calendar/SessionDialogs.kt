package at.oderwieoderw.timetrek.ui.calendar

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import at.oderwieoderw.timetrek.R
import at.oderwieoderw.timetrek.data.local.TimeEntryStore
import at.oderwieoderw.timetrek.domain.TimeEntry
import java.text.DateFormat
import java.util.Calendar

/** Edits only completed sessions; the running session remains controlled by TrackingScreen. */
internal class SessionDialogs(
    private val activity: ComponentActivity,
    private val store: TimeEntryStore,
    private val onChanged: () -> Unit
) {
    fun confirmDelete(index: Int, entry: TimeEntry) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.delete_session_title)
            .setMessage(R.string.delete_session_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_session) { _, _ ->
                if (store.deleteEntry(index, entry)) onChanged()
                else Toast.makeText(activity, R.string.session_change_failed, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    fun showEditor(index: Int, entry: TimeEntry) {
        val view = activity.layoutInflater.inflate(R.layout.dialog_edit_session, null)
        val start = Calendar.getInstance().apply { timeInMillis = entry.start }
        val end = Calendar.getInstance().apply { timeInMillis = entry.end }
        setupDateTimePicker(start, view.findViewById(R.id.start_date), view.findViewById(R.id.start_time),
            R.string.edit_start_date, R.string.edit_start_time)
        setupDateTimePicker(end, view.findViewById(R.id.end_date), view.findViewById(R.id.end_time),
            R.string.edit_end_date, R.string.edit_end_time)
        val error = view.findViewById<TextView>(R.id.edit_error)
        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.edit_session_title)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val updated = TimeEntry(start.timeInMillis, end.timeInMillis)
            val message = when {
                updated.end <= updated.start -> R.string.invalid_session_order
                updated.end > System.currentTimeMillis() -> R.string.invalid_session_future
                !store.updateEntry(index, entry, updated) -> R.string.invalid_session_overlap
                else -> null
            }
            if (message != null) {
                error.setText(message)
                error.visibility = View.VISIBLE
            } else {
                dialog.dismiss()
                onChanged()
            }
        }
    }

    private fun setupDateTimePicker(calendar: Calendar, dateButton: Button, timeButton: Button,
                                    dateDescription: Int, timeDescription: Int) {
        fun refreshLabels() {
            dateButton.text = DateFormat.getDateInstance(DateFormat.SHORT).format(calendar.time)
            timeButton.text = DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.time)
            dateButton.contentDescription = "${activity.getString(dateDescription)}: ${dateButton.text}"
            timeButton.contentDescription = "${activity.getString(timeDescription)}: ${timeButton.text}"
        }
        refreshLabels()
        dateButton.setOnClickListener {
            DatePickerDialog(activity, { _, year, month, day ->
                calendar.set(year, month, day)
                refreshLabels()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        timeButton.setOnClickListener {
            TimePickerDialog(activity, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                refreshLabels()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                android.text.format.DateFormat.is24HourFormat(activity)).show()
        }
    }
}
