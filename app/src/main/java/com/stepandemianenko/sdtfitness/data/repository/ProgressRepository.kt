package com.stepandemianenko.sdtfitness.data.repository

import com.stepandemianenko.sdtfitness.data.local.SessionSetLogDao
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionDao
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionEntity
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ProgressSummary(
    val completedSessions: Int,
    val workoutDays: Int,
    val streakDays: Int,
    val last7DaySessions: Int,
    val previous7DaySessions: Int,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolumeKg: Double,
    val bestLiftKg: Int,
    val topExerciseName: String?,
    val topExerciseVolumeKg: Double?
)

class ProgressRepository(
    private val database: WorkoutDatabase
) {
    private val sessionDao: WorkoutSessionDao = database.workoutSessionDao()
    private val setLogDao: SessionSetLogDao = database.sessionSetLogDao()

    suspend fun getSummary(now: LocalDate = LocalDate.now()): ProgressSummary {
        val totals = setLogDao.getProgressTotals(completedStatus = WorkoutSessionStatus.COMPLETED)
        val completedSessions = sessionDao.getByStatus(status = WorkoutSessionStatus.COMPLETED)
        val workoutDays = setLogDao.getCompletedWorkoutDays(completedStatus = WorkoutSessionStatus.COMPLETED)
            .mapNotNull { row -> row.workoutDay.takeIf { it.isNotBlank() }?.let(LocalDate::parse) }
            .distinct()

        val streakDays = calculateStreakDays(workoutDays)
        val sessionDates = completedSessions.map { it.toLocalDate() }

        val last7Start = now.minusDays(6)
        val previous7Start = now.minusDays(13)
        val previous7End = now.minusDays(7)

        val last7DaySessions = sessionDates.count { !it.isBefore(last7Start) && !it.isAfter(now) }
        val previous7DaySessions = sessionDates.count { !it.isBefore(previous7Start) && !it.isAfter(previous7End) }

        val topExercise = setLogDao.getTopExerciseByVolume(completedStatus = WorkoutSessionStatus.COMPLETED)

        return ProgressSummary(
            completedSessions = totals.completedSessions,
            workoutDays = workoutDays.size,
            streakDays = streakDays,
            last7DaySessions = last7DaySessions,
            previous7DaySessions = previous7DaySessions,
            totalSets = totals.totalSets,
            totalReps = totals.totalReps,
            totalVolumeKg = totals.totalVolume,
            bestLiftKg = totals.bestLiftKg,
            topExerciseName = topExercise?.exerciseName,
            topExerciseVolumeKg = topExercise?.totalVolume
        )
    }

    private fun calculateStreakDays(days: List<LocalDate>): Int {
        if (days.isEmpty()) return 0
        val descending = days.sortedDescending()
        var streak = 1
        for (index in 1 until descending.size) {
            val previous = descending[index - 1]
            val current = descending[index]
            if (current == previous.minusDays(1)) {
                streak += 1
            } else {
                break
            }
        }
        return streak
    }

    private fun WorkoutSessionEntity.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
        val epochMillis = completedAt ?: startedAt
        return Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
    }
}
