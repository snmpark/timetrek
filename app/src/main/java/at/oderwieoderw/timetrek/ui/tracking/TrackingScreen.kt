package at.oderwieoderw.timetrek.ui.tracking

import android.app.AlertDialog
import android.os.SystemClock
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import at.oderwieoderw.timetrek.R
import at.oderwieoderw.timetrek.data.local.TimeEntryStore
import at.oderwieoderw.timetrek.domain.GoalMessageStage
import at.oderwieoderw.timetrek.domain.TimeEntries
import at.oderwieoderw.timetrek.domain.TimeEntry
import at.oderwieoderw.timetrek.domain.WeeklyMotivation
import at.oderwieoderw.timetrek.ui.TimeFormatting

/** Owns the timer, weekly goal, and the single start/finish action. */
class TrackingScreen(
    private val activity: ComponentActivity,
    private val store: TimeEntryStore,
    private val onChanged: () -> Unit
) {
    private data class MotivationKey(val weekStart: Long, val goalMinutes: Int, val stage: GoalMessageStage)

    private val sessionButton: Button = activity.findViewById(R.id.session_button)
    private val actionContainer: FrameLayout = activity.findViewById(R.id.session_action_container)
    private val actionTitle: TextView = activity.findViewById(R.id.session_action_title)
    private val actionIcon: ImageView = activity.findViewById(R.id.session_action_icon)
    private val actionHint: TextView = activity.findViewById(R.id.action_hint)
    private val timerLabel: TextView = activity.findViewById(R.id.timer_label)
    private val counter: TextView = activity.findViewById(R.id.counter_text)
    private val todayTotal: TextView = activity.findViewById(R.id.today_total)
    private val goalButton: Button = activity.findViewById(R.id.goal_button)
    private val remainingLabel: TextView = activity.findViewById(R.id.remaining_label)
    private val remainingTime: TextView = activity.findViewById(R.id.remaining_time)
    private val weeklyProgress: TextView = activity.findViewById(R.id.weekly_progress)
    private val weeklyMotivation: TextView = activity.findViewById(R.id.weekly_motivation)

    private var feedbackUntil = 0L
    private var completedDuration = 0L
    private var lastActionAt = 0L
    private var motivationKey: MotivationKey? = null

    init {
        goalButton.setOnClickListener { showGoalDialog() }
        sessionButton.setOnClickListener {
            val actionAt = SystemClock.elapsedRealtime()
            if (actionAt - lastActionAt < 700L) return@setOnClickListener
            lastActionAt = actionAt
            val now = System.currentTimeMillis()
            val active = store.activeStart
            if (active == null) {
                feedbackUntil = 0L
                actionHint.animate().cancel()
                actionHint.alpha = 1f
                store.start(now)
            } else {
                store.stop(now)
                if (store.activeStart == null) {
                    completedDuration = now - active
                    feedbackUntil = now + 6_000L
                }
            }
            onChanged()
            actionContainer.animate().cancel()
            actionContainer.scaleX = 0.98f
            actionContainer.scaleY = 0.98f
            actionContainer.animate().scaleX(1f).scaleY(1f).setDuration(180L).start()
            if (active != null && store.activeStart == null) {
                actionHint.animate().cancel()
                actionHint.alpha = 0f
                actionHint.animate().alpha(1f).setDuration(300L).start()
            }
        }
    }

    fun render(now: Long, entries: List<TimeEntry>, active: Long?) {
        sessionButton.isSelected = active != null
        sessionButton.contentDescription = activity.getString(if (active == null) R.string.start_working else R.string.finish_work)
        actionTitle.setText(if (active == null) R.string.start_working else R.string.finish_work)
        actionIcon.setImageResource(if (active == null) R.drawable.ic_start_work else R.drawable.ic_finish_work)
        actionHint.text = when {
            active != null -> activity.getString(R.string.finish_hint)
            now < feedbackUntil -> activity.getString(R.string.session_complete, TimeFormatting.duration(completedDuration))
            else -> activity.getString(R.string.start_hint)
        }
        timerLabel.setText(if (active == null) R.string.ready_to_track else R.string.tracking_now)
        counter.text = TimeFormatting.elapsed((now - (active ?: now)).coerceAtLeast(0L))
        todayTotal.text = TimeFormatting.duration(TimeEntries.totalForDay(entries, now))
        renderWeeklyGoal(entries, now)
    }

    private fun renderWeeklyGoal(entries: List<TimeEntry>, now: Long) {
        val goalMinutes = store.weeklyGoalMinutes
        val tracked = TimeEntries.totalForWeek(entries, now)
        renderMotivation(now, goalMinutes, tracked)
        goalButton.setText(if (goalMinutes == 0) R.string.set_goal else R.string.edit_goal)
        if (goalMinutes == 0) {
            remainingLabel.setText(R.string.set_goal_prompt)
            remainingTime.text = "—"
            weeklyProgress.setText(R.string.weekly_progress_prompt)
            return
        }

        val goal = goalMinutes * 60_000L
        val remaining = (goal - tracked).coerceAtLeast(0L)
        remainingLabel.setText(when {
            tracked > goal -> R.string.weekly_overtime_label
            remaining > 0L -> R.string.remaining_this_week
            else -> R.string.weekly_goal_reached
        })
        remainingTime.text = if (remaining == 0L) activity.getString(R.string.weekly_goal_done)
            else TimeFormatting.countdown(remaining)
        val progress = activity.getString(R.string.weekly_progress, TimeFormatting.duration(tracked), TimeFormatting.duration(goal))
        weeklyProgress.text = if (tracked > goal) {
            "$progress · ${activity.getString(R.string.weekly_overtime, TimeFormatting.duration(tracked - goal))}"
        } else {
            progress
        }
    }

    private fun renderMotivation(now: Long, goalMinutes: Int, tracked: Long) {
        val stage = WeeklyMotivation.stage(goalMinutes, tracked)
        val key = MotivationKey(TimeEntries.weekBounds(now).first, goalMinutes, stage)
        if (key == motivationKey) return
        motivationKey = key
        val messageArray = when (stage) {
            GoalMessageStage.NO_GOAL -> R.array.motivation_no_goal
            GoalMessageStage.GETTING_STARTED -> R.array.motivation_getting_started
            GoalMessageStage.ON_TRACK -> R.array.motivation_on_track
            GoalMessageStage.NEARLY_THERE -> R.array.motivation_nearly_there
            GoalMessageStage.GOAL_MET -> R.array.motivation_goal_met
            GoalMessageStage.OVERTIME -> R.array.motivation_overtime
        }
        weeklyMotivation.text = activity.resources.getStringArray(messageArray).random()
        weeklyMotivation.setBackgroundResource(
            if (stage == GoalMessageStage.OVERTIME) R.drawable.motivation_warning_background
            else R.drawable.motivation_background
        )
        weeklyMotivation.setTextColor(activity.getColor(if (stage == GoalMessageStage.OVERTIME) R.color.delete_text else R.color.ink))
    }

    private fun showGoalDialog() {
        val view = activity.layoutInflater.inflate(R.layout.dialog_weekly_goal, null)
        val hoursInput = view.findViewById<EditText>(R.id.goal_hours)
        val minutesInput = view.findViewById<EditText>(R.id.goal_minutes)
        val currentGoal = store.weeklyGoalMinutes.takeIf { it > 0 } ?: (40 * 60)
        hoursInput.setText((currentGoal / 60).toString())
        minutesInput.setText((currentGoal % 60).toString())

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.weekly_goal_dialog_title)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val hoursText = hoursInput.text.toString().trim()
            val minutesText = minutesInput.text.toString().trim()
            val hours = if (hoursText.isEmpty()) 0 else hoursText.toIntOrNull() ?: -1
            val minutes = if (minutesText.isEmpty()) 0 else minutesText.toIntOrNull() ?: -1
            if (minutes !in 0..59) {
                minutesInput.error = activity.getString(R.string.invalid_minutes)
                return@setOnClickListener
            }
            val goal = hours.toLong() * 60L + minutes
            if (hours < 0 || goal !in 1..(168 * 60L)) {
                hoursInput.error = activity.getString(R.string.invalid_weekly_goal)
                return@setOnClickListener
            }
            store.weeklyGoalMinutes = goal.toInt()
            dialog.dismiss()
            onChanged()
        }
    }
}
