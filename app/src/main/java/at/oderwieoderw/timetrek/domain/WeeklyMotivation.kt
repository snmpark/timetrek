package at.oderwieoderw.timetrek.domain

enum class GoalMessageStage {
    NO_GOAL, GETTING_STARTED, ON_TRACK, NEARLY_THERE, GOAL_MET, OVERTIME
}

object WeeklyMotivation {
    fun stage(goalMinutes: Int, trackedMillis: Long): GoalMessageStage {
        if (goalMinutes <= 0) return GoalMessageStage.NO_GOAL
        val goal = goalMinutes * 60_000L
        val remaining = goal - trackedMillis
        return when {
            remaining < 0L -> GoalMessageStage.OVERTIME
            remaining == 0L -> GoalMessageStage.GOAL_MET
            remaining <= goal / 4 -> GoalMessageStage.NEARLY_THERE
            remaining <= goal * 3 / 4 -> GoalMessageStage.ON_TRACK
            else -> GoalMessageStage.GETTING_STARTED
        }
    }
}
