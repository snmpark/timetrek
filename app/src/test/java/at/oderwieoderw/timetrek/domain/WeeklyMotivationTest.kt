package at.oderwieoderw.timetrek.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyMotivationTest {
    private val hour = 60 * 60_000L

    @Test
    fun messagesFollowRemainingWeeklyTime() {
        assertEquals(GoalMessageStage.NO_GOAL, WeeklyMotivation.stage(0, 0))
        assertEquals(GoalMessageStage.GETTING_STARTED, WeeklyMotivation.stage(40 * 60, 0))
        assertEquals(GoalMessageStage.ON_TRACK, WeeklyMotivation.stage(40 * 60, 10 * hour))
        assertEquals(GoalMessageStage.NEARLY_THERE, WeeklyMotivation.stage(40 * 60, 30 * hour))
        assertEquals(GoalMessageStage.GOAL_MET, WeeklyMotivation.stage(40 * 60, 40 * hour))
        assertEquals(GoalMessageStage.OVERTIME, WeeklyMotivation.stage(40 * 60, 40 * hour + 1))
    }
}
