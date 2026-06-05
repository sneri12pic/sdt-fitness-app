package com.stepandemianenko.sdtfitness.home

import androidx.room.withTransaction
import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import com.stepandemianenko.sdtfitness.data.local.DailyQuestId
import com.stepandemianenko.sdtfitness.data.local.DailyQuestRecordDao
import com.stepandemianenko.sdtfitness.data.local.DailyQuestRecordEntity
import com.stepandemianenko.sdtfitness.data.local.CreatineIntakeLogDao
import com.stepandemianenko.sdtfitness.data.local.CreatineIntakeLogEntity
import com.stepandemianenko.sdtfitness.data.local.SyncState
import com.stepandemianenko.sdtfitness.data.local.UserSettingsDao
import com.stepandemianenko.sdtfitness.data.local.UserSettingsEntity
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import java.time.Instant
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
    private val dailyQuestRecordDao: DailyQuestRecordDao = database.dailyQuestRecordDao()
    private val creatineIntakeLogDao: CreatineIntakeLogDao = database.creatineIntakeLogDao()
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
        latestWeightKg: Double?,
        todayWeightKg: Double? = null,
        todayWeightRecordedAt: Instant? = null
    ) {
        scope.launch {
            val accountId = accountSessionManager.requireActiveAccountId()
            val now = System.currentTimeMillis()
            val todayKey = LocalDate.now().toString()
            database.withTransaction {
                val current = userSettingsDao.getByAccountId(accountId)
                    ?: defaultSettings(accountId = accountId, now = now)
                userSettingsDao.upsert(
                    current.copy(
                        accountId = accountId,
                        healthConnectLastSyncedAt = now,
                        healthConnectLastImportedSteps = importedSteps
                            .coerceAtLeast(0L)
                            .coerceAtMost(Int.MAX_VALUE.toLong())
                            .toInt(),
                        healthConnectLatestWeightKg = latestWeightKg,
                        updatedAt = now
                    )
                )
                if (todayWeightKg != null) {
                    upsertWeightInRecord(
                        accountId = accountId,
                        date = todayKey,
                        now = now,
                        completed = true,
                        completionSource = DailyQuestCompletionSource.HEALTH_CONNECT.name,
                        weightKg = todayWeightKg,
                        completedAt = todayWeightRecordedAt?.toEpochMilli() ?: now
                    )
                }
            }
            publishUpdatedState(accountId = accountId)
        }
    }

    fun addTodayWeightInQuest() {
        scope.launch {
            val accountId = accountSessionManager.requireActiveAccountId()
            val now = System.currentTimeMillis()
            val todayKey = LocalDate.now().toString()
            database.withTransaction {
                upsertWeightInRecord(
                    accountId = accountId,
                    date = todayKey,
                    now = now,
                    completed = false,
                    completionSource = null,
                    weightKg = null,
                    completedAt = null
                )
            }
            publishUpdatedState(accountId = accountId)
        }
    }

    fun addCreatineIntakeQuest() {
        mutateSettings { current, _ ->
            current.copy(creatineQuestEnabled = true)
        }
    }

    fun addTodayCreatinePortion() {
        scope.launch {
            val accountId = accountSessionManager.requireActiveAccountId()
            val now = System.currentTimeMillis()
            val todayKey = LocalDate.now().toString()
            database.withTransaction {
                val settings = userSettingsDao.getByAccountId(accountId)
                    ?: defaultSettings(accountId = accountId, now = now)
                val portionGrams = settings.creatinePortionGrams.coerceAtLeast(1)
                creatineIntakeLogDao.insert(
                    CreatineIntakeLogEntity(
                        accountId = accountId,
                        date = todayKey,
                        amountGrams = portionGrams,
                        timestamp = now,
                        createdAt = now,
                        updatedAt = now,
                        syncState = SyncState.LOCAL_ONLY
                    )
                )
            }
            publishUpdatedState(accountId = accountId)
        }
    }

    fun setCreatineTarget(targetGrams: Int) {
        require(targetGrams > 0) { "Creatine target must be positive" }
        mutateSettings { current, _ ->
            current.copy(creatineTargetGrams = targetGrams)
        }
    }

    fun setCreatinePortion(portionGrams: Int) {
        require(portionGrams > 0) { "Creatine portion must be positive" }
        mutateSettings { current, _ ->
            current.copy(creatinePortionGrams = portionGrams)
        }
    }

    fun deleteCreatinePortion(logId: Long) {
        require(logId > 0) { "Creatine log ID must be positive" }
        scope.launch {
            val accountId = accountSessionManager.requireActiveAccountId()
            val now = System.currentTimeMillis()
            creatineIntakeLogDao.markDeleted(
                accountId = accountId,
                logId = logId,
                deletedAt = now,
                syncState = SyncState.PENDING_DELETE
            )
            publishUpdatedState(accountId = accountId)
        }
    }

    fun setTodayWeightInCompleted(completed: Boolean) {
        scope.launch {
            val accountId = accountSessionManager.requireActiveAccountId()
            val now = System.currentTimeMillis()
            val todayKey = LocalDate.now().toString()
            database.withTransaction {
                upsertWeightInRecord(
                    accountId = accountId,
                    date = todayKey,
                    now = now,
                    completed = completed,
                    completionSource = if (completed) DailyQuestCompletionSource.MANUAL.name else null,
                    weightKg = null,
                    completedAt = null
                )
            }
            publishUpdatedState(accountId = accountId)
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
        val todayKey = LocalDate.now().toString()
        val questRecords = dailyQuestRecordDao.getForDate(accountId = accountId, date = todayKey)
        val creatineLogs = creatineIntakeLogDao.getForDate(accountId = accountId, date = todayKey)
        _dashboardState.value = settings.toDashboardState(
            questRecords = questRecords,
            creatineLogs = creatineLogs
        )
    }

    private fun defaultSettings(accountId: String, now: Long): UserSettingsEntity {
        return UserSettingsEntity(
            accountId = accountId,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.LOCAL_ONLY
        )
    }

    private suspend fun upsertWeightInRecord(
        accountId: String,
        date: String,
        now: Long,
        completed: Boolean,
        completionSource: String?,
        weightKg: Double?,
        completedAt: Long?
    ) {
        val current = dailyQuestRecordDao.getByQuestAndDate(
            accountId = accountId,
            questId = DailyQuestId.WEIGHT_IN,
            date = date
        )
        val record = DailyQuestRecordEntity(
            accountId = accountId,
            questId = DailyQuestId.WEIGHT_IN,
            date = date,
            isAdded = true,
            isCompleted = completed,
            completionSource = completionSource,
            completedAt = if (completed) completedAt ?: now else null,
            valueKg = weightKg ?: current?.valueKg,
            createdAt = current?.createdAt ?: now,
            updatedAt = now,
            deletedAt = null,
            syncState = SyncState.LOCAL_ONLY
        )
        dailyQuestRecordDao.upsert(record)
    }

    private fun UserSettingsEntity.toDashboardState(
        questRecords: List<DailyQuestRecordEntity>,
        creatineLogs: List<CreatineIntakeLogEntity>
    ): HomeDashboardState {
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

        val weightInRecord = questRecords.firstOrNull { it.questId == DailyQuestId.WEIGHT_IN }
        val weightInSource = weightInRecord?.completionSource?.let { sourceName ->
            runCatching { DailyQuestCompletionSource.valueOf(sourceName) }.getOrNull()
        }

        return HomeDashboardState(
            dailyQuest = DailyQuestState(
                sourceType = sourceType,
                targetSteps = targetSteps,
                currentSteps = currentSteps,
                isManual = sourceType == DailyStepsSourceType.MANUAL,
                lastUpdatedMillis = dailyStepsLastUpdated
            ),
            weightInQuest = WeightInQuestState(
                isAdded = weightInRecord?.isAdded == true,
                isCompleted = weightInRecord?.isCompleted == true,
                completionSource = weightInSource,
                completedAtMillis = weightInRecord?.completedAt,
                weightKg = weightInRecord?.valueKg
            ),
            creatineIntakeQuest = CreatineIntakeQuestState(
                isAdded = creatineQuestEnabled,
                currentGramsToday = creatineLogs.sumOf { it.amountGrams.toLong() }
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt(),
                targetGrams = creatineTargetGrams.coerceAtLeast(1),
                portionGrams = creatinePortionGrams.coerceAtLeast(1),
                todayLogs = creatineLogs.map { log ->
                    CreatineIntakeLog(
                        id = log.id,
                        amountGrams = log.amountGrams.coerceAtLeast(0),
                        timestampMillis = log.timestamp
                    )
                }
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
