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

data class LoggedWorkoutSetSnapshot(
    val id: Long,
    val setNumber: Int,
    val actualWeightKg: Int,
    val actualReps: Int,
    val rpe: Int?
)

data class LoggedSetUpdateDraft(
    val weightKg: Int,
    val reps: Int,
    val rpe: Int?
)

data class RestoreSetLogDraft(
    val id: Long,
    val setNumber: Int,
    val actualWeightKg: Int,
    val actualReps: Int,
    val rpe: Int?
)

data class RestoreExerciseDraft(
    val id: Long,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseOrder: Int,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Int,
    val loggedSets: List<RestoreSetLogDraft>
)

data class LogWorkoutExerciseSnapshot(
    val id: Long,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseOrder: Int,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Int,
    val loggedSets: List<LoggedWorkoutSetSnapshot>,
    val previousResult: PreviousExerciseResult?,
    val personalBest: PreviousExerciseResult?
)

data class LogWorkoutSessionSnapshot(
    val sessionId: Long,
    val status: String,
    val startedAt: Long,
    val totalSetsCompleted: Int,
    val totalSetsTarget: Int,
    val remainingSets: Int,
    val totalVolumeCompleted: Double,
    val exercises: List<LogWorkoutExerciseSnapshot>
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

                val totalSetsTarget = orderedExercises.sumOf { it.targetSets.coerceAtLeast(0) }
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

    suspend fun appendExercisesToSession(
        sessionId: Long,
        exercisesToAppend: List<SessionExerciseDraft>,
        forceSingleSet: Boolean = true
    ): Boolean {
        if (exercisesToAppend.isEmpty()) return false

        val accountId = accountSessionManager.requireActiveAccountId()
        val now = System.currentTimeMillis()
        return database.withTransaction {
            val session = sessionDao.getById(accountId = accountId, sessionId = sessionId)
                ?: return@withTransaction false
            if (session.status !in ACTIVE_SESSION_STATUSES) {
                return@withTransaction false
            }

            val existingExercises = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId)
                .sortedBy { it.exerciseOrder }
            val nextOrderStart = (existingExercises.maxOfOrNull { it.exerciseOrder } ?: -1) + 1

            val appendEntities = exercisesToAppend.mapIndexed { index, draft ->
                SessionExerciseEntity(
                    accountId = accountId,
                    sessionId = sessionId,
                    exerciseId = draft.exerciseId,
                    exerciseName = draft.exerciseName,
                    exerciseOrder = nextOrderStart + index,
                    targetSets = if (forceSingleSet) 1 else draft.targetSets.coerceAtLeast(1),
                    targetReps = draft.targetReps.coerceAtLeast(1),
                    targetWeightKg = draft.targetWeightKg.coerceAtLeast(0),
                    status = SessionExerciseStatus.PENDING,
                    createdAt = now,
                    updatedAt = now,
                    syncState = SyncState.LOCAL_ONLY
                )
            }
            exerciseDao.insertAll(appendEntities)

            val addedSetsTarget = appendEntities.sumOf { it.targetSets.coerceAtLeast(1) }
            sessionDao.update(
                session.copy(
                    status = WorkoutSessionStatus.ACTIVE,
                    totalSetsTarget = session.totalSetsTarget + addedSetsTarget,
                    updatedAt = now
                )
            )
            true
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLogWorkoutSession(sessionId: Long): Flow<LogWorkoutSessionSnapshot?> {
        return accountSessionManager.accountScope.flatMapLatest { scopeKey ->
            val accountId = scopeKey.accountId
            val sessionFlow = sessionDao.observeById(accountId = accountId, sessionId = sessionId)
            val exercisesFlow = exerciseDao.observeForSession(accountId = accountId, sessionId = sessionId)
            val setLogsFlow = setLogDao.observeForSession(accountId = accountId, sessionId = sessionId)

            combine(sessionFlow, exercisesFlow, setLogsFlow) { session, exercises, setLogs ->
                if (session == null) return@combine null

                val orderedExercises = exercises.sortedBy { it.exerciseOrder }
                val setsByExercise = setLogs
                    .sortedBy { it.completedAt }
                    .groupBy { it.sessionExerciseId }

                val exerciseSnapshots = orderedExercises.map { exercise ->
                    val loggedSets = setsByExercise[exercise.id]
                        .orEmpty()
                        .sortedBy { it.setNumber }
                        .map { log ->
                            LoggedWorkoutSetSnapshot(
                                id = log.id,
                                setNumber = log.setNumber,
                                actualWeightKg = log.actualWeightKg,
                                actualReps = log.actualReps,
                                rpe = log.rpe
                            )
                        }

                    val previousResult = setLogDao.getLatestCompletedResultForExercise(
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

                    val personalBest = setLogDao.getPersonalBestForExercise(
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

                    LogWorkoutExerciseSnapshot(
                        id = exercise.id,
                        exerciseId = exercise.exerciseId,
                        exerciseName = exercise.exerciseName,
                        exerciseOrder = exercise.exerciseOrder,
                        targetSets = exercise.targetSets,
                        targetReps = exercise.targetReps,
                        targetWeightKg = exercise.targetWeightKg,
                        loggedSets = loggedSets,
                        previousResult = previousResult,
                        personalBest = personalBest
                    )
                }

                val totalSetsTarget = orderedExercises.sumOf { it.targetSets.coerceAtLeast(0) }
                val totalSetsCompleted = setLogs.size
                val remainingSets = (totalSetsTarget - totalSetsCompleted).coerceAtLeast(0)
                val volume = setLogs.sumOf { (it.actualWeightKg * it.actualReps).toDouble() }

                LogWorkoutSessionSnapshot(
                    sessionId = session.id,
                    status = session.status,
                    startedAt = session.startedAt,
                    totalSetsCompleted = totalSetsCompleted,
                    totalSetsTarget = totalSetsTarget,
                    remainingSets = remainingSets,
                    totalVolumeCompleted = volume,
                    exercises = exerciseSnapshots
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

    suspend fun logSetForExercise(
        sessionId: Long,
        sessionExerciseId: Long,
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

            val orderedExercises = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId)
                .sortedBy { it.exerciseOrder }
            val targetExercise = orderedExercises.firstOrNull { it.id == sessionExerciseId }
                ?: return@withTransaction LogSetOutcome.NoActiveSession

            val existingSetLogs = setLogDao.getForSession(accountId = accountId, sessionId = sessionId)
            val loggedSetCountByExercise = existingSetLogs
                .groupBy { it.sessionExerciseId }
                .mapValues { (_, logs) -> logs.size }
            val completedForTargetExercise = loggedSetCountByExercise[targetExercise.id] ?: 0
            if (completedForTargetExercise >= targetExercise.targetSets) {
                return@withTransaction LogSetOutcome.NoActiveSession
            }

            val setNumber = completedForTargetExercise + 1
            val now = System.currentTimeMillis()
            val normalizedWeight = actualWeightKg.coerceAtLeast(0)
            val normalizedReps = actualReps.coerceAtLeast(1)

            setLogDao.insert(
                SessionSetLogEntity(
                    accountId = accountId,
                    sessionId = session.id,
                    sessionExerciseId = targetExercise.id,
                    setNumber = setNumber,
                    targetWeightKg = targetExercise.targetWeightKg,
                    actualWeightKg = normalizedWeight,
                    targetReps = targetExercise.targetReps,
                    actualReps = normalizedReps,
                    rpe = rpe,
                    completedAt = now,
                    createdAt = now,
                    updatedAt = now,
                    syncState = SyncState.LOCAL_ONLY
                )
            )

            val updatedLogs = setLogDao.getForSession(accountId = accountId, sessionId = sessionId)
            val updatedCounts = updatedLogs
                .groupBy { it.sessionExerciseId }
                .mapValues { (_, logs) -> logs.size }

            orderedExercises.forEach { exercise ->
                val completedCount = updatedCounts[exercise.id] ?: 0
                val nextStatus = when {
                    completedCount >= exercise.targetSets -> SessionExerciseStatus.COMPLETED
                    completedCount > 0 -> SessionExerciseStatus.ACTIVE
                    else -> SessionExerciseStatus.PENDING
                }
                exerciseDao.updateStatus(
                    accountId = accountId,
                    sessionExerciseId = exercise.id,
                    status = nextStatus,
                    updatedAt = now
                )
            }

            val allExercisesCompleted = orderedExercises.all { exercise ->
                (updatedCounts[exercise.id] ?: 0) >= exercise.targetSets
            }
            val firstIncompleteIndex = orderedExercises.indexOfFirst { exercise ->
                (updatedCounts[exercise.id] ?: 0) < exercise.targetSets
            }.coerceAtLeast(0)
            val activeExercise = orderedExercises.getOrNull(firstIncompleteIndex)
            val activeExerciseCompletedSets = activeExercise?.let { updatedCounts[it.id] ?: 0 } ?: 0

            sessionDao.update(
                session.copy(
                    status = WorkoutSessionStatus.ACTIVE,
                    completedAt = session.completedAt,
                    currentExerciseIndex = if (allExercisesCompleted) {
                        orderedExercises.lastIndex.coerceAtLeast(0)
                    } else {
                        firstIncompleteIndex
                    },
                    currentSetIndex = if (allExercisesCompleted) 0 else activeExerciseCompletedSets,
                    totalSetsCompleted = updatedLogs.size,
                    totalRepsCompleted = updatedLogs.sumOf { it.actualReps },
                    totalVolumeCompleted = updatedLogs.sumOf {
                        (it.actualWeightKg * it.actualReps).toDouble()
                    },
                    updatedAt = now
                )
            )

            if (allExercisesCompleted) {
                return@withTransaction LogSetOutcome.AdvancedExercise(
                    completedExerciseName = targetExercise.exerciseName,
                    nextExerciseName = targetExercise.exerciseName
                )
            }

            if (setNumber < targetExercise.targetSets) {
                return@withTransaction LogSetOutcome.AdvancedSet(
                    nextSetNumber = setNumber + 1,
                    totalSetsForExercise = targetExercise.targetSets
                )
            }

            val nextExerciseName = orderedExercises
                .firstOrNull { exercise -> (updatedCounts[exercise.id] ?: 0) < exercise.targetSets }
                ?.exerciseName
                ?: targetExercise.exerciseName
            LogSetOutcome.AdvancedExercise(
                completedExerciseName = targetExercise.exerciseName,
                nextExerciseName = nextExerciseName
            )
        }
    }

    suspend fun updateLoggedSet(
        sessionId: Long,
        sessionExerciseId: Long,
        setNumber: Int,
        update: LoggedSetUpdateDraft
    ): Boolean {
        val accountId = accountSessionManager.requireActiveAccountId()
        return database.withTransaction {
            val session = sessionDao.getById(accountId = accountId, sessionId = sessionId)
                ?: return@withTransaction false
            val existingSetLogs = setLogDao.getForSession(accountId = accountId, sessionId = sessionId)
            val targetSetLog = existingSetLogs.firstOrNull { log ->
                log.sessionExerciseId == sessionExerciseId && log.setNumber == setNumber
            } ?: return@withTransaction false

            val now = System.currentTimeMillis()
            setLogDao.updateLoggedSetValues(
                accountId = accountId,
                setLogId = targetSetLog.id,
                actualWeightKg = update.weightKg.coerceAtLeast(0),
                actualReps = update.reps.coerceAtLeast(1),
                rpe = update.rpe,
                updatedAt = now
            )

            val refreshedLogs = setLogDao.getForSession(accountId = accountId, sessionId = sessionId)
            sessionDao.update(
                session.copy(
                    totalSetsCompleted = refreshedLogs.size,
                    totalRepsCompleted = refreshedLogs.sumOf { it.actualReps },
                    totalVolumeCompleted = refreshedLogs.sumOf {
                        (it.actualWeightKg * it.actualReps).toDouble()
                    },
                    updatedAt = now
                )
            )
            true
        }
    }

    suspend fun unlogSetForExercise(
        sessionId: Long,
        sessionExerciseId: Long,
        setNumber: Int
    ): Boolean {
        val accountId = accountSessionManager.requireActiveAccountId()
        return database.withTransaction {
            val session = sessionDao.getById(accountId = accountId, sessionId = sessionId)
                ?: return@withTransaction false
            if (session.status !in ACTIVE_SESSION_STATUSES) {
                return@withTransaction false
            }

            val orderedExercises = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId)
                .sortedBy { it.exerciseOrder }
            val targetExercise = orderedExercises.firstOrNull { it.id == sessionExerciseId }
                ?: return@withTransaction false

            val existingSetLogs = setLogDao.getForSession(accountId = accountId, sessionId = sessionId)
            val targetSetLog = existingSetLogs.firstOrNull { log ->
                log.sessionExerciseId == sessionExerciseId && log.setNumber == setNumber
            } ?: return@withTransaction false

            val now = System.currentTimeMillis()
            setLogDao.deleteById(
                accountId = accountId,
                setLogId = targetSetLog.id
            )
            setLogDao.shiftSetNumbersDownAfter(
                accountId = accountId,
                sessionId = sessionId,
                sessionExerciseId = sessionExerciseId,
                removedSetNumber = setNumber,
                updatedAt = now
            )

            val refreshedLogs = setLogDao.getForSession(accountId = accountId, sessionId = sessionId)
            val refreshedCounts = refreshedLogs
                .groupBy { it.sessionExerciseId }
                .mapValues { (_, logs) -> logs.size }

            orderedExercises.forEach { exercise ->
                val completedCount = refreshedCounts[exercise.id] ?: 0
                val nextStatus = when {
                    completedCount >= exercise.targetSets -> SessionExerciseStatus.COMPLETED
                    completedCount > 0 -> SessionExerciseStatus.ACTIVE
                    else -> SessionExerciseStatus.PENDING
                }
                exerciseDao.updateStatus(
                    accountId = accountId,
                    sessionExerciseId = exercise.id,
                    status = nextStatus,
                    updatedAt = now
                )
            }

            val firstIncompleteIndex = orderedExercises.indexOfFirst { exercise ->
                (refreshedCounts[exercise.id] ?: 0) < exercise.targetSets
            }.coerceAtLeast(0)
            val activeExercise = orderedExercises.getOrNull(firstIncompleteIndex)
            val activeExerciseCompletedSets = activeExercise?.let { refreshedCounts[it.id] ?: 0 } ?: 0

            sessionDao.update(
                session.copy(
                    status = WorkoutSessionStatus.ACTIVE,
                    completedAt = null,
                    currentExerciseIndex = firstIncompleteIndex,
                    currentSetIndex = activeExerciseCompletedSets,
                    totalSetsCompleted = refreshedLogs.size,
                    totalRepsCompleted = refreshedLogs.sumOf { it.actualReps },
                    totalVolumeCompleted = refreshedLogs.sumOf {
                        (it.actualWeightKg * it.actualReps).toDouble()
                    },
                    updatedAt = now
                )
            )
            true
        }
    }

    suspend fun removeSetFromExercise(
        sessionId: Long,
        sessionExerciseId: Long,
        setNumber: Int
    ): Boolean {
        val accountId = accountSessionManager.requireActiveAccountId()
        return database.withTransaction {
            val session = sessionDao.getById(accountId = accountId, sessionId = sessionId)
                ?: return@withTransaction false
            if (session.status !in ACTIVE_SESSION_STATUSES) {
                return@withTransaction false
            }

            val exercises = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId)
                .sortedBy { it.exerciseOrder }
            val targetExercise = exercises.firstOrNull { it.id == sessionExerciseId }
                ?: return@withTransaction false
            if (targetExercise.targetSets <= 0) {
                return@withTransaction false
            }

            val normalizedSetNumber = setNumber.coerceIn(1, targetExercise.targetSets)
            val now = System.currentTimeMillis()
            val existingLogs = setLogDao.getForSession(accountId = accountId, sessionId = sessionId)
            val existingLog = existingLogs.firstOrNull { log ->
                log.sessionExerciseId == sessionExerciseId && log.setNumber == normalizedSetNumber
            }

            if (existingLog != null) {
                setLogDao.deleteById(
                    accountId = accountId,
                    setLogId = existingLog.id
                )
                setLogDao.shiftSetNumbersDownAfter(
                    accountId = accountId,
                    sessionId = sessionId,
                    sessionExerciseId = sessionExerciseId,
                    removedSetNumber = normalizedSetNumber,
                    updatedAt = now
                )
            }

            exerciseDao.updateTargetSets(
                accountId = accountId,
                sessionExerciseId = sessionExerciseId,
                targetSets = (targetExercise.targetSets - 1).coerceAtLeast(0),
                updatedAt = now
            )

            recalculateActiveSessionState(
                accountId = accountId,
                session = session,
                sessionId = sessionId,
                now = now
            )
            true
        }
    }

    suspend fun restoreSetInExercise(
        sessionId: Long,
        sessionExerciseId: Long,
        setNumber: Int,
        restoredLog: RestoreSetLogDraft?
    ): Boolean {
        val accountId = accountSessionManager.requireActiveAccountId()
        return database.withTransaction {
            val session = sessionDao.getById(accountId = accountId, sessionId = sessionId)
                ?: return@withTransaction false
            if (session.status !in ACTIVE_SESSION_STATUSES) {
                return@withTransaction false
            }

            val exercises = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId)
                .sortedBy { it.exerciseOrder }
            val targetExercise = exercises.firstOrNull { it.id == sessionExerciseId }
                ?: return@withTransaction false
            val insertSetNumber = setNumber.coerceIn(1, targetExercise.targetSets + 1)
            val now = System.currentTimeMillis()

            exerciseDao.updateTargetSets(
                accountId = accountId,
                sessionExerciseId = sessionExerciseId,
                targetSets = targetExercise.targetSets + 1,
                updatedAt = now
            )

            if (restoredLog != null) {
                setLogDao.shiftSetNumbersUpFrom(
                    accountId = accountId,
                    sessionId = sessionId,
                    sessionExerciseId = sessionExerciseId,
                    insertAtSetNumber = insertSetNumber,
                    updatedAt = now
                )
                setLogDao.insert(
                    SessionSetLogEntity(
                        id = restoredLog.id,
                        accountId = accountId,
                        sessionId = sessionId,
                        sessionExerciseId = sessionExerciseId,
                        setNumber = insertSetNumber,
                        targetWeightKg = targetExercise.targetWeightKg,
                        actualWeightKg = restoredLog.actualWeightKg,
                        targetReps = targetExercise.targetReps,
                        actualReps = restoredLog.actualReps,
                        rpe = restoredLog.rpe,
                        completedAt = now,
                        createdAt = now,
                        updatedAt = now,
                        syncState = SyncState.LOCAL_ONLY
                    )
                )
            }

            recalculateActiveSessionState(
                accountId = accountId,
                session = session,
                sessionId = sessionId,
                now = now
            )
            true
        }
    }

    suspend fun removeExerciseFromSession(
        sessionId: Long,
        sessionExerciseId: Long
    ): Boolean {
        val accountId = accountSessionManager.requireActiveAccountId()
        return database.withTransaction {
            val session = sessionDao.getById(accountId = accountId, sessionId = sessionId)
                ?: return@withTransaction false
            if (session.status !in ACTIVE_SESSION_STATUSES) {
                return@withTransaction false
            }

            val exercises = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId)
                .sortedBy { it.exerciseOrder }
            val targetExercise = exercises.firstOrNull { it.id == sessionExerciseId }
                ?: return@withTransaction false

            val now = System.currentTimeMillis()
            exerciseDao.deleteById(
                accountId = accountId,
                sessionExerciseId = sessionExerciseId
            )
            exerciseDao.shiftExerciseOrderDownAfter(
                accountId = accountId,
                sessionId = sessionId,
                removedOrder = targetExercise.exerciseOrder,
                updatedAt = now
            )

            recalculateActiveSessionState(
                accountId = accountId,
                session = session,
                sessionId = sessionId,
                now = now
            )
            true
        }
    }

    suspend fun restoreExerciseToSession(
        sessionId: Long,
        restoreDraft: RestoreExerciseDraft
    ): Boolean {
        val accountId = accountSessionManager.requireActiveAccountId()
        return database.withTransaction {
            val session = sessionDao.getById(accountId = accountId, sessionId = sessionId)
                ?: return@withTransaction false
            if (session.status !in ACTIVE_SESSION_STATUSES) {
                return@withTransaction false
            }

            val exercises = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId)
                .sortedBy { it.exerciseOrder }
            val boundedOrder = restoreDraft.exerciseOrder.coerceIn(0, exercises.size)
            val now = System.currentTimeMillis()

            exerciseDao.shiftExerciseOrderUpFrom(
                accountId = accountId,
                sessionId = sessionId,
                fromOrder = boundedOrder,
                updatedAt = now
            )

            exerciseDao.insert(
                SessionExerciseEntity(
                    id = restoreDraft.id,
                    accountId = accountId,
                    sessionId = sessionId,
                    exerciseId = restoreDraft.exerciseId,
                    exerciseName = restoreDraft.exerciseName,
                    exerciseOrder = boundedOrder,
                    targetSets = restoreDraft.targetSets.coerceAtLeast(0),
                    targetReps = restoreDraft.targetReps.coerceAtLeast(1),
                    targetWeightKg = restoreDraft.targetWeightKg.coerceAtLeast(0),
                    status = SessionExerciseStatus.PENDING,
                    createdAt = now,
                    updatedAt = now,
                    syncState = SyncState.LOCAL_ONLY
                )
            )

            restoreDraft.loggedSets
                .sortedBy { it.setNumber }
                .forEach { loggedSet ->
                    setLogDao.insert(
                        SessionSetLogEntity(
                            id = loggedSet.id,
                            accountId = accountId,
                            sessionId = sessionId,
                            sessionExerciseId = restoreDraft.id,
                            setNumber = loggedSet.setNumber.coerceAtLeast(1),
                            targetWeightKg = restoreDraft.targetWeightKg.coerceAtLeast(0),
                            actualWeightKg = loggedSet.actualWeightKg.coerceAtLeast(0),
                            targetReps = restoreDraft.targetReps.coerceAtLeast(1),
                            actualReps = loggedSet.actualReps.coerceAtLeast(1),
                            rpe = loggedSet.rpe,
                            completedAt = now + loggedSet.setNumber,
                            createdAt = now,
                            updatedAt = now,
                            syncState = SyncState.LOCAL_ONLY
                        )
                    )
                }

            recalculateActiveSessionState(
                accountId = accountId,
                session = session,
                sessionId = sessionId,
                now = now
            )
            true
        }
    }

    suspend fun increaseExerciseTargetSets(
        sessionId: Long,
        sessionExerciseId: Long,
        delta: Int = 1
    ): Boolean {
        if (delta <= 0) return false

        val accountId = accountSessionManager.requireActiveAccountId()
        return database.withTransaction {
            val session = sessionDao.getById(accountId = accountId, sessionId = sessionId)
                ?: return@withTransaction false
            if (session.status !in ACTIVE_SESSION_STATUSES) {
                return@withTransaction false
            }

            val exercise = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId)
                .firstOrNull { it.id == sessionExerciseId }
                ?: return@withTransaction false

            val now = System.currentTimeMillis()
            val nextTargetSets = (exercise.targetSets + delta).coerceAtLeast(0)
            exerciseDao.updateTargetSets(
                accountId = accountId,
                sessionExerciseId = sessionExerciseId,
                targetSets = nextTargetSets,
                updatedAt = now
            )
            sessionDao.update(
                session.copy(
                    totalSetsTarget = (session.totalSetsTarget + delta).coerceAtLeast(0),
                    updatedAt = now
                )
            )
            true
        }
    }

    suspend fun discardSession(sessionId: Long): Boolean {
        val accountId = accountSessionManager.requireActiveAccountId()
        return database.withTransaction {
            val session = sessionDao.getById(accountId = accountId, sessionId = sessionId)
                ?: return@withTransaction false
            if (session.status !in ACTIVE_SESSION_STATUSES) {
                return@withTransaction false
            }
            sessionDao.deleteById(accountId = accountId, sessionId = sessionId)
            true
        }
    }

    suspend fun completeSession(sessionId: Long): Boolean {
        val accountId = accountSessionManager.requireActiveAccountId()
        return database.withTransaction {
            val session = sessionDao.getById(accountId = accountId, sessionId = sessionId)
                ?: return@withTransaction false

            if (session.status == WorkoutSessionStatus.COMPLETED) {
                return@withTransaction true
            }

            val now = System.currentTimeMillis()
            val exercises = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId)
            val setLogs = setLogDao.getForSession(accountId = accountId, sessionId = sessionId)
            val loggedCountByExercise = setLogs
                .groupBy { it.sessionExerciseId }
                .mapValues { (_, logs) -> logs.size }

            exercises.forEach { exercise ->
                val loggedCount = loggedCountByExercise[exercise.id] ?: 0
                val nextStatus = if (loggedCount >= exercise.targetSets) {
                    SessionExerciseStatus.COMPLETED
                } else {
                    SessionExerciseStatus.SKIPPED
                }
                exerciseDao.updateStatus(
                    accountId = accountId,
                    sessionExerciseId = exercise.id,
                    status = nextStatus,
                    updatedAt = now
                )
            }

            sessionDao.update(
                session.copy(
                    status = WorkoutSessionStatus.COMPLETED,
                    completedAt = now,
                    currentExerciseIndex = exercises.lastIndex.coerceAtLeast(0),
                    currentSetIndex = 0,
                    updatedAt = now
                )
            )
            true
        }
    }

    private suspend fun recalculateActiveSessionState(
        accountId: String,
        session: WorkoutSessionEntity,
        sessionId: Long,
        now: Long
    ) {
        val orderedExercises = exerciseDao.getForSession(accountId = accountId, sessionId = sessionId)
            .sortedBy { it.exerciseOrder }
        val refreshedLogs = setLogDao.getForSession(accountId = accountId, sessionId = sessionId)
        val refreshedCounts = refreshedLogs
            .groupBy { it.sessionExerciseId }
            .mapValues { (_, logs) -> logs.size }

        orderedExercises.forEach { exercise ->
            val completedCount = refreshedCounts[exercise.id] ?: 0
            val nextStatus = when {
                completedCount >= exercise.targetSets -> SessionExerciseStatus.COMPLETED
                completedCount > 0 -> SessionExerciseStatus.ACTIVE
                else -> SessionExerciseStatus.PENDING
            }
            exerciseDao.updateStatus(
                accountId = accountId,
                sessionExerciseId = exercise.id,
                status = nextStatus,
                updatedAt = now
            )
        }

        val firstIncompleteIndex = orderedExercises.indexOfFirst { exercise ->
            (refreshedCounts[exercise.id] ?: 0) < exercise.targetSets
        }.coerceAtLeast(0)
        val activeExercise = orderedExercises.getOrNull(firstIncompleteIndex)
        val activeExerciseCompletedSets = activeExercise?.let { refreshedCounts[it.id] ?: 0 } ?: 0

        sessionDao.update(
            session.copy(
                status = WorkoutSessionStatus.ACTIVE,
                completedAt = null,
                currentExerciseIndex = firstIncompleteIndex,
                currentSetIndex = activeExerciseCompletedSets,
                totalSetsTarget = orderedExercises.sumOf { it.targetSets.coerceAtLeast(0) },
                totalSetsCompleted = refreshedLogs.size,
                totalRepsCompleted = refreshedLogs.sumOf { it.actualReps },
                totalVolumeCompleted = refreshedLogs.sumOf {
                    (it.actualWeightKg * it.actualReps).toDouble()
                },
                updatedAt = now
            )
        )
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
