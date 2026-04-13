package com.stepandemianenko.sdtfitness.data.repository

import androidx.room.withTransaction
import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import com.stepandemianenko.sdtfitness.data.local.SessionExerciseDao
import com.stepandemianenko.sdtfitness.data.local.SessionExerciseEntity
import com.stepandemianenko.sdtfitness.data.local.SessionExerciseStatus
import com.stepandemianenko.sdtfitness.data.local.SessionSetLogDao
import com.stepandemianenko.sdtfitness.data.local.SessionSetLogEntity
import com.stepandemianenko.sdtfitness.data.local.SyncState
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionDao
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionEntity
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi

private val ACTIVE_SESSION_STATUSES = listOf(
    WorkoutSessionStatus.PLANNED,
    WorkoutSessionStatus.ACTIVE
)

data class SessionExerciseDraft(
    val exerciseId: String,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Int
)

data class SessionStartResult(
    val sessionId: Long,
    val resumedExisting: Boolean
)

data class PreviousExerciseResult(
    val weightKg: Int,
    val reps: Int,
    val completedAt: Long
)

data class ActiveExerciseSnapshot(
    val id: Long,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseOrder: Int,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Int
)

data class ActiveWorkoutSnapshot(
    val sessionId: Long,
    val status: String,
    val currentExerciseIndex: Int,
    val totalExercises: Int,
    val currentExercise: ActiveExerciseSnapshot?,
    val currentSetNumber: Int,
    val completedSets: Int,
    val totalSetsTarget: Int,
    val remainingSets: Int,
    val estimatedMinutesRemaining: Int,
    val previousResult: PreviousExerciseResult?,
    val personalBest: PreviousExerciseResult?
)

sealed interface LogSetOutcome {
    data class AdvancedSet(
        val nextSetNumber: Int,
        val totalSetsForExercise: Int
    ) : LogSetOutcome

    data class AdvancedExercise(
        val completedExerciseName: String,
        val nextExerciseName: String
    ) : LogSetOutcome

    data class SessionCompleted(
        val completedExerciseName: String,
        val sessionId: Long
    ) : LogSetOutcome

    data object NoActiveSession : LogSetOutcome
}

class WorkoutSessionRepository(
    private val database: WorkoutDatabase,
    private val accountSessionManager: AccountSessionManager
) {
    private val sessionDao: WorkoutSessionDao = database.workoutSessionDao()
    private val exerciseDao: SessionExerciseDao = database.sessionExerciseDao()
    private val setLogDao: SessionSetLogDao = database.sessionSetLogDao()

    suspend fun startOrResumeSession(
        templateId: String?,
        orderedExercises: List<SessionExerciseDraft>
    ): SessionStartResult {
        if (orderedExercises.isEmpty()) {
            error("Cannot create a workout session with no exercises.")
        }

        val now = System.currentTimeMillis()
        val accountId = accountSessionManager.requireActiveAccountId()
        val totalSetsTarget = orderedExercises.sumOf { it.targetSets.coerceAtLeast(1) }

        return database.withTransaction {
            sessionDao.updateStatuses(
                accountId = accountId,
                fromStatuses = ACTIVE_SESSION_STATUSES,
                newStatus = WorkoutSessionStatus.ABANDONED,
                endedAt = now
            )

            val sessionId = sessionDao.insert(
                WorkoutSessionEntity(
                    accountId = accountId,
                    templateId = templateId,
                    startedAt = now,
                    status = WorkoutSessionStatus.ACTIVE,
                    currentExerciseIndex = 0,
                    currentSetIndex = 0,
                    totalSetsTarget = totalSetsTarget,
                    createdAt = now,
                    updatedAt = now,
                    syncState = SyncState.LOCAL_ONLY
                )
            )

            val sessionExercises = orderedExercises.mapIndexed { index, draft ->
                SessionExerciseEntity(
                    accountId = accountId,
                    sessionId = sessionId,
                    exerciseId = draft.exerciseId,
                    exerciseName = draft.exerciseName,
                    exerciseOrder = index,
                    targetSets = draft.targetSets.coerceAtLeast(1),
                    targetReps = draft.targetReps.coerceAtLeast(1),
                    targetWeightKg = draft.targetWeightKg.coerceAtLeast(0),
                    status = if (index == 0) {
                        SessionExerciseStatus.ACTIVE
                    } else {
                        SessionExerciseStatus.PENDING
                    },
                    createdAt = now,
                    updatedAt = now,
                    syncState = SyncState.LOCAL_ONLY
                )
            }
            exerciseDao.insertAll(sessionExercises)

            SessionStartResult(
                sessionId = sessionId,
                resumedExisting = false
            )
        }
    }

    suspend fun getActiveSessionId(): Long? {
        val accountId = accountSessionManager.requireActiveAccountId()
        return sessionDao.getMostRecentByStatuses(
            accountId = accountId,
            activeStatuses = ACTIVE_SESSION_STATUSES
        )?.id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeSession(sessionId: Long): Flow<ActiveWorkoutSnapshot?> {
        return accountSessionManager.accountScope.flatMapLatest { scopeKey ->
            val accountId = scopeKey.accountId
            val sessionFlow = sessionDao.observeById(accountId = accountId, sessionId = sessionId)
            val exercisesFlow = exerciseDao.observeForSession(accountId = accountId, sessionId = sessionId)
            val setLogsFlow = setLogDao.observeForSession(accountId = accountId, sessionId = sessionId)

            combine(sessionFlow, exercisesFlow, setLogsFlow) { session, exercises, setLogs ->
                if (session == null) return@combine null

                val orderedExercises = exercises.sortedBy { it.exerciseOrder }
                val loggedSetCountByExercise = setLogs
                    .groupBy { it.sessionExerciseId }
                    .mapValues { (_, logs) -> logs.size }

                val currentExercise = orderedExercises.firstOrNull { exercise ->
                    val completedForExercise = loggedSetCountByExercise[exercise.id] ?: 0
                    completedForExercise < exercise.targetSets
                }
                val currentExerciseIndex = if (currentExercise == null) {
                    orderedExercises.lastIndex.coerceAtLeast(0)
                } else {
                    orderedExercises.indexOfFirst { it.id == currentExercise.id }.coerceAtLeast(0)
                }
                val currentSetNumber = if (currentExercise == null) {
                    0
                } else {
                    val completedForExercise = loggedSetCountByExercise[currentExercise.id] ?: 0
                    (completedForExercise + 1).coerceIn(1, currentExercise.targetSets)
                }

                val previousResult = currentExercise?.let { exercise ->
                    setLogDao.getLatestCompletedResultForExercise(
                        accountId = accountId,
                        exerciseId = exercise.exerciseId,
                        completedStatus = WorkoutSessionStatus.COMPLETED,
                        excludeSessionId = session.id
                    )?.let {
                        PreviousExerciseResult(
                            weightKg = it.actualWeightKg,
                            reps = it.actualReps,
                            completedAt = it.completedAt
                        )
                    }
                }

                val personalBest = currentExercise?.let { exercise ->
                    setLogDao.getPersonalBestForExercise(
                        accountId = accountId,
                        exerciseId = exercise.exerciseId,
                        completedStatus = WorkoutSessionStatus.COMPLETED,
                        excludeSessionId = session.id
                    )?.let {
                        PreviousExerciseResult(
                            weightKg = it.actualWeightKg,
                            reps = it.actualReps,
                            completedAt = it.completedAt
                        )
                    }
                }

                val totalSetsTarget = orderedExercises.sumOf { it.targetSets.coerceAtLeast(1) }
                val completedSets = setLogs.size
                val remainingSets = (totalSetsTarget - completedSets).coerceAtLeast(0)
                val effectiveStatus = if (currentExercise == null && remainingSets == 0) {
                    WorkoutSessionStatus.COMPLETED
                } else {
                    session.status
                }

                ActiveWorkoutSnapshot(
                    sessionId = session.id,
                    status = effectiveStatus,
                    currentExerciseIndex = currentExerciseIndex,
                    totalExercises = orderedExercises.size,
                    currentExercise = currentExercise?.toSnapshot(),
                    currentSetNumber = currentSetNumber,
                    completedSets = completedSets,
                    totalSetsTarget = totalSetsTarget,
                    remainingSets = remainingSets,
                    estimatedMinutesRemaining = remainingSets * 2,
                    previousResult = previousResult,
                    personalBest = personalBest
                )
            }
        }
    }

    suspend fun logCurrentSet(
        sessionId: Long,
        actualWeightKg: Int,
        actualReps: Int,
        rpe: Int?
    ): LogSetOutcome {
        val accountId = accountSessionManager.requireActiveAccountId()
        return database.withTransaction {
            val session = sessionDao.getById(accountId = accountId, sessionId = sessionId)
                ?: return@withTransaction LogSetOutcome.NoActiveSession

            if (session.status !in ACTIVE_SESSION_STATUSES) {
                return@withTransaction LogSetOutcome.NoActiveSession
            }

            val orderedExercises = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId).sortedBy { it.exerciseOrder }
            val existingSetLogs = setLogDao.getForSession(accountId = accountId, sessionId = sessionId)
            val loggedSetCountByExercise = existingSetLogs
                .groupBy { it.sessionExerciseId }
                .mapValues { (_, logs) -> logs.size }

            val currentExercise = orderedExercises.firstOrNull { exercise ->
                val completedForExercise = loggedSetCountByExercise[exercise.id] ?: 0
                completedForExercise < exercise.targetSets
            }
                ?: return@withTransaction LogSetOutcome.NoActiveSession

            val currentExerciseIndex = orderedExercises.indexOfFirst { it.id == currentExercise.id }.coerceAtLeast(0)
            val completedForCurrentExercise = loggedSetCountByExercise[currentExercise.id] ?: 0
            val setNumber = (completedForCurrentExercise + 1).coerceAtMost(currentExercise.targetSets)
            val now = System.currentTimeMillis()

            setLogDao.insert(
                SessionSetLogEntity(
                    accountId = accountId,
                    sessionId = session.id,
                    sessionExerciseId = currentExercise.id,
                    setNumber = setNumber,
                    targetWeightKg = currentExercise.targetWeightKg,
                    actualWeightKg = actualWeightKg.coerceAtLeast(0),
                    targetReps = currentExercise.targetReps,
                    actualReps = actualReps.coerceAtLeast(1),
                    rpe = rpe,
                    completedAt = now,
                    createdAt = now,
                    updatedAt = now,
                    syncState = SyncState.LOCAL_ONLY
                )
            )

            val updatedSessionTotals = session.copy(
                totalSetsCompleted = session.totalSetsCompleted + 1,
                totalRepsCompleted = session.totalRepsCompleted + actualReps.coerceAtLeast(1),
                totalVolumeCompleted = session.totalVolumeCompleted + (actualWeightKg.coerceAtLeast(0) * actualReps.coerceAtLeast(1)).toDouble(),
                updatedAt = now
            )

            if (setNumber < currentExercise.targetSets) {
                sessionDao.update(
                    updatedSessionTotals.copy(
                        status = WorkoutSessionStatus.ACTIVE,
                        currentExerciseIndex = currentExerciseIndex,
                        currentSetIndex = setNumber
                    )
                )
                return@withTransaction LogSetOutcome.AdvancedSet(
                    nextSetNumber = setNumber + 1,
                    totalSetsForExercise = currentExercise.targetSets
                )
            }

            exerciseDao.updateStatus(
                accountId = accountId,
                sessionExerciseId = currentExercise.id,
                status = SessionExerciseStatus.COMPLETED,
                updatedAt = now
            )

            val nextExercise = orderedExercises.getOrNull(session.currentExerciseIndex + 1)
            if (nextExercise != null) {
                exerciseDao.updateStatus(
                    accountId = accountId,
                    sessionExerciseId = nextExercise.id,
                    status = SessionExerciseStatus.ACTIVE,
                    updatedAt = now
                )
                sessionDao.update(
                    updatedSessionTotals.copy(
                        status = WorkoutSessionStatus.ACTIVE,
                        currentExerciseIndex = currentExerciseIndex + 1,
                        currentSetIndex = 0
                    )
                )
                return@withTransaction LogSetOutcome.AdvancedExercise(
                    completedExerciseName = currentExercise.exerciseName,
                    nextExerciseName = nextExercise.exerciseName
                )
            }

            sessionDao.update(
                updatedSessionTotals.copy(
                    status = WorkoutSessionStatus.COMPLETED,
                    completedAt = now
                )
            )
            LogSetOutcome.SessionCompleted(
                completedExerciseName = currentExercise.exerciseName,
                sessionId = session.id
            )
        }
    }

    private fun SessionExerciseEntity.toSnapshot(): ActiveExerciseSnapshot {
        return ActiveExerciseSnapshot(
            id = id,
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            exerciseOrder = exerciseOrder,
            targetSets = targetSets,
            targetReps = targetReps,
            targetWeightKg = targetWeightKg
        )
    }
}
