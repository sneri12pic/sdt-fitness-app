package com.stepandemianenko.sdtfitness.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeModelsTest {

    @Test
    fun dailyQuestProgress_returnsFractionOfTarget() {
        val quest = DailyQuestState(
            targetSteps = 10_000,
            currentSteps = 2_500
        )

        assertEquals(0.25f, quest.progress, 0.001f)
    }

    @Test
    fun dailyQuestProgress_capsAtComplete() {
        val quest = DailyQuestState(
            targetSteps = 5_000,
            currentSteps = 7_500
        )

        assertEquals(1f, quest.progress, 0.001f)
    }

    @Test
    fun dailyQuestProgress_returnsZeroWhenTargetIsInvalid() {
        val quest = DailyQuestState(
            targetSteps = 0,
            currentSteps = 2_000
        )

        assertEquals(0f, quest.progress, 0.001f)
    }

    @Test
    fun creatineProgress_returnsFractionOfTargetAndCapsAtComplete() {
        assertEquals(
            0.5f,
            CreatineIntakeQuestState(currentGramsToday = 5, targetGrams = 10).progress,
            0.001f
        )
        assertEquals(
            1f,
            CreatineIntakeQuestState(currentGramsToday = 15, targetGrams = 10).progress,
            0.001f
        )
    }

    @Test
    fun waterProgress_returnsFractionOfTargetAndCapsAtComplete() {
        assertEquals(
            0.5f,
            WaterIntakeQuestState(currentMlToday = 1000, targetMl = 2000).progress,
            0.001f
        )
        assertEquals(
            1f,
            WaterIntakeQuestState(currentMlToday = 2500, targetMl = 2000).progress,
            0.001f
        )
    }

    @Test
    fun dailyGoalSummaryOverallProgress_averagesStepsWorkoutsAndQuests() {
        val summary = DailyGoalSummaryState(
            stepsCurrent = 2_500,
            stepsTarget = 5_000,
            workoutsCompleted = 1,
            workoutsTarget = 1,
            questsCompleted = 1,
            questsTarget = 2
        )

        assertEquals(0.666f, summary.overallProgress, 0.001f)
    }

    @Test
    fun dailyGoalSummaryOverallProgress_capsWorkoutCompletion() {
        val summary = DailyGoalSummaryState(
            stepsCurrent = 5_000,
            stepsTarget = 5_000,
            workoutsCompleted = 3,
            workoutsTarget = 1,
            questsCompleted = 4,
            questsTarget = 2
        )

        assertEquals(1f, summary.workoutsCompletedCapped.toFloat(), 0.001f)
        assertEquals(2f, summary.questsCompletedCapped.toFloat(), 0.001f)
        assertEquals(1f, summary.overallProgress, 0.001f)
    }

    @Test
    fun calculateCurrentStreak_countsConsecutiveDatesEndingToday() {
        val today = LocalDate.of(2026, 5, 25)
        val streakDates = setOf(
            today,
            today.minusDays(1),
            today.minusDays(2),
            today.minusDays(5)
        )

        assertEquals(3, calculateCurrentStreak(streakDates, today))
    }

    @Test
    fun calculateCurrentStreak_returnsZeroWhenTodayIsMissing() {
        val today = LocalDate.of(2026, 5, 25)
        val streakDates = setOf(
            today.minusDays(1),
            today.minusDays(2)
        )

        assertEquals(0, calculateCurrentStreak(streakDates, today))
    }

    @Test
    fun buildRoutineCalendarActivityMap_keepsWorkoutQuickLogAndRestDayDistinct() {
        val workoutDate = LocalDate.of(2026, 7, 20)
        val quickLogDate = LocalDate.of(2026, 7, 21)
        val restDayDate = LocalDate.of(2026, 7, 22)

        val activities = buildRoutineCalendarActivityMap(
            routineDates = setOf(workoutDate, quickLogDate, restDayDate),
            workoutDates = setOf(workoutDate),
            quickLogDates = setOf(quickLogDate),
            restDayDates = setOf(restDayDate)
        )

        assertEquals(setOf(RoutineCalendarActivity.WORKOUT), activities[workoutDate])
        assertEquals(setOf(RoutineCalendarActivity.QUICK_LOG), activities[quickLogDate])
        assertEquals(setOf(RoutineCalendarActivity.REST_DAY), activities[restDayDate])
    }

    @Test
    fun buildRoutineCalendarActivityMap_preservesMultipleActivitiesOnSameDay() {
        val date = LocalDate.of(2026, 7, 23)

        val activities = buildRoutineCalendarActivityMap(
            routineDates = setOf(date),
            workoutDates = setOf(date),
            quickLogDates = setOf(date),
            restDayDates = emptySet()
        )

        assertEquals(
            setOf(RoutineCalendarActivity.WORKOUT, RoutineCalendarActivity.QUICK_LOG),
            activities[date]
        )
        assertFalse(activities[date].orEmpty().contains(RoutineCalendarActivity.ACTIVITY))
    }

    @Test
    fun buildRoutineCalendarActivityMap_marksUnclassifiedLegacyRoutineDates() {
        val legacyDate = LocalDate.of(2026, 7, 19)

        val activities = buildRoutineCalendarActivityMap(
            routineDates = setOf(legacyDate),
            workoutDates = emptySet(),
            quickLogDates = emptySet(),
            restDayDates = emptySet()
        )

        assertTrue(activities[legacyDate].orEmpty().contains(RoutineCalendarActivity.ACTIVITY))
    }
}
