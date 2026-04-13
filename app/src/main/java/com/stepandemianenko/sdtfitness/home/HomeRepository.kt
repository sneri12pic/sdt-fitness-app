package com.stepandemianenko.sdtfitness.home

import androidx.room.withTransaction
import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import com.stepandemianenko.sdtfitness.data.local.SyncState
import com.stepandemianenko.sdtfitness.data.local.UserSettingsDao
import com.stepandemianenko.sdtfitness.data.local.UserSettingsEntity
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeRepository(
    private val database: WorkoutDatabase,
    private val accountSessionManager: AccountSessionManager
) {
    private val userSettingsDao: UserSettingsDao = database.userSettingsDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _dashboardState = MutableStateFlow(HomeDashboardState())
    val dashboardState: StateFlow<HomeDashboardState> = _dashboardState.asStateFlow()

    init {
        scope.launch {
            accountSessionManager.accountScope.collectLatest { scopeKey ->
                publishUpdatedState(accountId = scopeKey.accountId)
            }
        }
        scope.launch {
            val accountId = accountSessionManager.requireActiveAccountId()
            publishUpdatedState(accountId = accountId)
        }
    }

    fun setManualDailyQuest(
        targetSteps: Int,
        currentSteps: Int
    ) {
        mutateSettings { current, now ->
            current.copy(
                dailyStepsSource = DailyStepsSourceType.MANUAL.name,
                dailyStepsTarget = targetSteps.coerceAtLeast(1),
                dailyStepsCurrent = currentSteps.coerceAtLeast(0),
                dailyStepsLastUpdated = now
            )
        }
    }

    fun updateStepsFromHealthConnect(
        currentSteps: Int,
        targetSteps: Int? = null
    ) {
        mutateSettings { current, now ->
            current.copy(
                dailyStepsSource = DailyStepsSourceType.HEALTH_CONNECT.name,
                dailyStepsCurrent = currentSteps.coerceAtLeast(0),
                dailyStepsTarget = targetSteps?.coerceAtLeast(1) ?: current.dailyStepsTarget,
                dailyStepsLastUpdated = now
            )
        }
    }

    fun recordHealthConnectImport(
        importedSteps: Long,
        latestWeightKg: Double?
    ) {
        mutateSettings { current, now ->
            current.copy(
                healthConnectLastSyncedAt = now,
                healthConnectLastImportedSteps = importedSteps
                    .coerceAtLeast(0L)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt(),
                healthConnectLatestWeightKg = latestWeightKg
            )
        }
    }

    fun setTodayWorkoutCompleted(
        completed: Boolean = true
    ) {
        mutateSettings { current, _ ->
            val todayKey = LocalDate.now().toString()
            val workoutDates = decodeDateSet(current.workoutCompletedDatesCsv).toMutableSet()
            val routineDates = decodeDateSet(current.routineCompletedDatesCsv).toMutableSet()

            if (completed) {
                workoutDates.add(todayKey)
                routineDates.add(todayKey)
            } else {
                workoutDates.remove(todayKey)
            }

            current.copy(
                workoutCompletedDatesCsv = encodeDateSet(workoutDates),
                routineCompletedDatesCsv = encodeDateSet(routineDates)
            )
        }
    }

    fun setTodayActiveMinutes(
        minutes: Int
    ) {
        mutateSettings { current, _ ->
            val safeMinutes = minutes.coerceAtLeast(0)
            val routineDates = decodeDateSet(current.routineCompletedDatesCsv).toMutableSet()
            if (safeMinutes > 0) {
                routineDates.add(LocalDate.now().toString())
            }
            current.copy(
                activeMinutesToday = safeMinutes,
                routineCompletedDatesCsv = encodeDateSet(routineDates)
            )
        }
    }

    fun logTodayRecovery(
        option: RecoveryOption
    ) {
        mutateSettings { current, now ->
            val todayKey = LocalDate.now().toString()
            val routineDates = decodeDateSet(current.routineCompletedDatesCsv).toMutableSet().apply {
                add(todayKey)
            }
            current.copy(
                recoveryLogDate = todayKey,
                recoveryLogOption = option.name,
                recoveryLogAtMillis = now,
                routineCompletedDatesCsv = encodeDateSet(routineDates)
            )
        }
    }

    fun logTodayQuickActivity(
        type: QuickLogType,
        durationMinutes: Int,
        timestampMillis: Long = System.currentTimeMillis(),
        source: String = "manual_quick_log"
    ) {
        mutateSettings { current, _ ->
            val todayKey = LocalDate.now().toString()
            val safeDuration = durationMinutes.coerceAtLeast(1)
            val updatedActiveMinutes = (current.activeMinutesToday + safeDuration)
                .coerceAtMost(Int.MAX_VALUE)
            val routineDates = decodeDateSet(current.routineCompletedDatesCsv).toMutableSet().apply {
                add(todayKey)
            }

            current.copy(
                quickLogDate = todayKey,
                quickLogType = type.name,
                quickLogDurationMinutes = safeDuration,
                quickLogTimestamp = timestampMillis,
                quickLogSource = source,
                activeMinutesToday = updatedActiveMinutes,
                routineCompletedDatesCsv = encodeDateSet(routineDates)
            )
        }
    }

    private fun mutateSettings(
        transform: (UserSettingsEntity, Long) -> UserSettingsEntity
    ) {
        scope.launch {
            val accountId = accountSessionManager.requireActiveAccountId()
            val now = System.currentTimeMillis()
            database.withTransaction {
                val current = userSettingsDao.getByAccountId(accountId)
                    ?: defaultSettings(accountId = accountId, now = now)
                val updated = transform(current, now).copy(
                    accountId = accountId,
                    updatedAt = now
                )
                userSettingsDao.upsert(updated)
            }
            publishUpdatedState(accountId = accountId)
        }
    }

    private suspend fun publishUpdatedState(accountId: String) {
        val settings = userSettingsDao.getByAccountId(accountId)
            ?: defaultSettings(accountId = accountId, now = System.currentTimeMillis()).also {
                userSettingsDao.upsert(it)
            }
        _dashboardState.value = settings.toDashboardState()
    }

    private fun defaultSettings(accountId: String, now: Long): UserSettingsEntity {
        return UserSettingsEntity(
            accountId = accountId,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.LOCAL_ONLY
        )
    }

    private fun UserSettingsEntity.toDashboardState(): HomeDashboardState {
        val sourceType = runCatching {
            DailyStepsSourceType.valueOf(dailyStepsSource)
        }.getOrDefault(DailyStepsSourceType.MANUAL)

        val targetSteps = dailyStepsTarget.coerceAtLeast(1)
        val currentSteps = dailyStepsCurrent.coerceAtLeast(0)

        val workoutCompletedDates = decodeDateSet(workoutCompletedDatesCsv)
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSet()
        val routineDates = decodeDateSet(routineCompletedDatesCsv)
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSet()

        val workoutsCompletedToday = if (workoutCompletedDates.contains(LocalDate.now())) 1 else 0
        val todayKey = LocalDate.now().toString()
        val savedRecoveryOption = if (recoveryLogDate == todayKey) {
            recoveryLogOption?.let { optionName ->
                runCatching { RecoveryOption.valueOf(optionName) }.getOrNull()
            }
        } else {
            null
        }
        val savedRecoveryAtMillis = recoveryLogAtMillis?.takeIf { recoveryLogDate == todayKey }

        val savedQuickLogEntry = if (quickLogDate == todayKey) {
            val type = quickLogType?.let { typeName ->
                runCatching { QuickLogType.valueOf(typeName) }.getOrNull()
            }
            val duration = quickLogDurationMinutes.coerceAtLeast(0)
            val timestamp = quickLogTimestamp ?: 0L
            val source = quickLogSource ?: "manual_quick_log"
            if (type != null && duration > 0 && timestamp > 0L) {
                QuickLogEntry(
                    quickLogType = type,
                    durationMinutes = duration,
                    timestamp = timestamp,
                    source = source
                )
            } else {
                null
            }
        } else {
            null
        }

        return HomeDashboardState(
            dailyQuest = DailyQuestState(
                sourceType = sourceType,
                targetSteps = targetSteps,
                currentSteps = currentSteps,
                isManual = sourceType == DailyStepsSourceType.MANUAL,
                lastUpdatedMillis = dailyStepsLastUpdated
            ),
            dailyGoalSummary = DailyGoalSummaryState(
                stepsCurrent = currentSteps,
                stepsTarget = targetSteps,
                workoutsCompleted = workoutsCompletedToday,
                workoutsTarget = 1,
                activeMinutesCurrent = activeMinutesToday.coerceAtLeast(0),
                activeMinutesTarget = DEFAULT_ACTIVE_MINUTES_TARGET
            ),
            routineStreakDates = routineDates,
            restDay = RestDayUiState(
                selectedOption = savedRecoveryOption ?: RecoveryOption.REST_DAY,
                savedTodayOption = savedRecoveryOption,
                savedTodayAtMillis = savedRecoveryAtMillis
            ),
            quickLogToday = savedQuickLogEntry,
            healthConnectLastSyncedAtMillis = healthConnectLastSyncedAt,
            healthConnectImportedSteps = healthConnectLastImportedSteps?.toLong(),
            healthConnectLatestWeightKg = healthConnectLatestWeightKg
        )
    }

    private fun decodeDateSet(value: String): Set<String> {
        if (value.isBlank()) return emptySet()
        return value.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun encodeDateSet(values: Set<String>): String {
        return values
            .filter { it.isNotBlank() }
            .sorted()
            .joinToString(separator = ",")
    }

    companion object {
        private const val DEFAULT_ACTIVE_MINUTES_TARGET = 30
    }
}
