package com.stepandemianenko.sdtfitness.data.repository

import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import com.stepandemianenko.sdtfitness.data.local.SessionSetLogDao
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionDao
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionEntity
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionStatus
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

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
    val topExerciseVolumeKg: Double?,
    val latestSessionCompletedToday: Boolean,
    val latestSessionVolumeKg: Double,
    val latestSessionSets: Int,
    val latestSessionReps: Int
)

data class CompletedSessionHistoryItem(
    val sessionId: Long,
    val templateId: String?,
    val completedAtMillis: Long,
    val exerciseCount: Int,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolumeKg: Double
)

data class CompletedSessionsHistory(
    val sessions: List<CompletedSessionHistoryItem>,
    val sessionsThisWeek: Int,
    val sessionsLastWeek: Int
)

data class SessionSetReview(
    val setNumber: Int,
    val actualWeightKg: Int,
    val actualReps: Int
)

data class SessionExerciseReview(
    val exerciseId: String,
    val exerciseName: String,
    val sets: List<SessionSetReview>
)

data class CompletedSessionReview(
    val sessionId: Long,
    val templateId: String?,
    val completedAtMillis: Long,
    val exercises: List<SessionExerciseReview>
)

class ProgressRepository(
    private val database: WorkoutDatabase,
    private val accountSessionManager: AccountSessionManager
) {
    private val sessionDao: WorkoutSessionDao = database.workoutSessionDao()
    private val exerciseDao = database.sessionExerciseDao()
    private val setLogDao: SessionSetLogDao = database.sessionSetLogDao()

    suspend fun getSummary(now: LocalDate = LocalDate.now()): ProgressSummary {
        val accountId = accountSessionManager.requireActiveAccountId()
        val totals = setLogDao.getProgressTotals(
            accountId = accountId,
            completedStatus = WorkoutSessionStatus.COMPLETED
        )
        val completedSessions = sessionDao.getByStatus(
            accountId = accountId,
            status = WorkoutSessionStatus.COMPLETED
        )
        val workoutDays = setLogDao.getCompletedWorkoutDays(
            accountId = accountId,
            completedStatus = WorkoutSessionStatus.COMPLETED
        )
            .mapNotNull { row -> row.workoutDay.takeIf { it.isNotBlank() }?.let(LocalDate::parse) }
            .distinct()

        val streakDays = calculateStreakDays(workoutDays)
        val sessionDates = completedSessions.map { it.toLocalDate() }

        val last7Start = now.minusDays(6)
        val previous7Start = now.minusDays(13)
        val previous7End = now.minusDays(7)

        val last7DaySessions = sessionDates.count { !it.isBefore(last7Start) && !it.isAfter(now) }
        val previous7DaySessions = sessionDates.count { !it.isBefore(previous7Start) && !it.isAfter(previous7End) }

        val topExercise = setLogDao.getTopExerciseByVolume(
            accountId = accountId,
            completedStatus = WorkoutSessionStatus.COMPLETED
        )
        val latestCompletedSession = completedSessions.maxByOrNull { it.completedAt ?: it.startedAt }
        val latestSessionCompletedToday = latestCompletedSession?.toLocalDate() == now

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
            topExerciseVolumeKg = topExercise?.totalVolume,
            latestSessionCompletedToday = latestSessionCompletedToday,
            latestSessionVolumeKg = latestCompletedSession?.totalVolumeCompleted ?: 0.0,
            latestSessionSets = latestCompletedSession?.totalSetsCompleted ?: 0,
            latestSessionReps = latestCompletedSession?.totalRepsCompleted ?: 0
        )
    }

    suspend fun getCompletedSessionsHistory(now: LocalDate = LocalDate.now()): CompletedSessionsHistory {
        val accountId = accountSessionManager.requireActiveAccountId()
        val sessions = sessionDao.getByStatus(
            accountId = accountId,
            status = WorkoutSessionStatus.COMPLETED
        )
            .sortedByDescending { it.completedAt ?: it.startedAt }

        val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val nextWeekStart = weekStart.plusDays(7)
        val lastWeekStart = weekStart.minusDays(7)

        val sessionsThisWeek = sessions.count { session ->
            val date = session.toLocalDate()
            !date.isBefore(weekStart) && date.isBefore(nextWeekStart)
        }

        val sessionsLastWeek = sessions.count { session ->
            val date = session.toLocalDate()
            !date.isBefore(lastWeekStart) && date.isBefore(weekStart)
        }

        val historyItems = sessions.map { session ->
            val exerciseCount = exerciseDao.getForSession(accountId = accountId, sessionId = session.id).size
            CompletedSessionHistoryItem(
                sessionId = session.id,
                templateId = session.templateId,
                completedAtMillis = session.completedAt ?: session.startedAt,
                exerciseCount = exerciseCount,
                totalSets = session.totalSetsCompleted,
                totalReps = session.totalRepsCompleted,
                totalVolumeKg = session.totalVolumeCompleted
            )
        }

        return CompletedSessionsHistory(
            sessions = historyItems,
            sessionsThisWeek = sessionsThisWeek,
            sessionsLastWeek = sessionsLastWeek
        )
    }

    suspend fun getCompletedSessionReview(sessionId: Long): CompletedSessionReview? {
        val accountId = accountSessionManager.requireActiveAccountId()
        val session = sessionDao.getById(accountId = accountId, sessionId = sessionId) ?: return null
        if (session.status != WorkoutSessionStatus.COMPLETED) return null

        val exercises = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId).sortedBy { it.exerciseOrder }
        val setLogsByExercise = setLogDao.getForSession(accountId = accountId, sessionId = sessionId)
            .groupBy { it.sessionExerciseId }

        val exerciseReviews = exercises.map { exercise ->
            val sets = setLogsByExercise[exercise.id]
                .orEmpty()
                .sortedBy { it.setNumber }
                .map { set ->
                    SessionSetReview(
                        setNumber = set.setNumber,
                        actualWeightKg = set.actualWeightKg,
                        actualReps = set.actualReps
                    )
                }
            SessionExerciseReview(
                exerciseId = exercise.exerciseId,
                exerciseName = exercise.exerciseName,
                sets = sets
            )
        }

        return CompletedSessionReview(
            sessionId = session.id,
            templateId = session.templateId,
            completedAtMillis = session.completedAt ?: session.startedAt,
            exercises = exerciseReviews
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
