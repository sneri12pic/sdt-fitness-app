package com.stepandemianenko.sdtfitness.profile

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

class ProfileRepository(
    private val database: WorkoutDatabase,
    private val accountSessionManager: AccountSessionManager,
    private val reminderScheduler: RoutineReminderScheduler
) {
    private val userSettingsDao: UserSettingsDao = database.userSettingsDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _routineSettings = MutableStateFlow(RoutineSettings())
    val routineSettings: StateFlow<RoutineSettings> = _routineSettings.asStateFlow()

    init {
        scope.launch {
            accountSessionManager.accountScope.collectLatest { scopeKey ->
                publishRoutine(accountId = scopeKey.accountId)
            }
        }
        scope.launch {
            publishRoutine(accountId = accountSessionManager.requireActiveAccountId())
        }
    }

    suspend fun saveRoutine(settings: RoutineSettings): RoutineSettings {
        val accountId = accountSessionManager.requireActiveAccountId()
        val now = System.currentTimeMillis()
        val sanitized = settings.sanitized()
        database.withTransaction {
            val current = userSettingsDao.getByAccountId(accountId)
                ?: defaultSettings(accountId = accountId, now = now)
            userSettingsDao.upsert(
                current.copy(
                    routineGoalId = sanitized.goalId,
                    routineFrequencyId = sanitized.frequencyId,
                    routineDayIdsCsv = encodeCsv(sanitized.dayIds),
                    routineReminderEnabled = sanitized.reminderEnabled,
                    routineReminderTimesCsv = encodeCsv(sanitized.reminderTimes),
                    routineCustomReminderTimesCsv = encodeCsv(sanitized.customReminderTimes),
                    updatedAt = now,
                    syncState = SyncState.LOCAL_ONLY
                )
            )
        }
        publishRoutine(accountId = accountId)
        return sanitized
    }

    private suspend fun publishRoutine(accountId: String) {
        val now = System.currentTimeMillis()
        val settings = userSettingsDao.getByAccountId(accountId)
            ?: defaultSettings(accountId = accountId, now = now).also {
                userSettingsDao.upsert(it)
            }
        val routine = settings.toRoutineSettings()
        _routineSettings.value = routine
        reminderScheduler.schedule(accountId = accountId, routine = routine)
    }

    private fun UserSettingsEntity.toRoutineSettings(): RoutineSettings {
        return RoutineSettings(
            goalId = routineGoalId.ifBlank { RoutineDefaults.goalId },
            frequencyId = routineFrequencyId.ifBlank { RoutineDefaults.frequencyId },
            dayIds = decodeCsv(routineDayIdsCsv).ifEmpty { RoutineDefaults.dayIds },
            reminderEnabled = routineReminderEnabled,
            reminderTimes = decodeCsv(routineReminderTimesCsv)
                .filterValidTimes()
                .ifEmpty { RoutineDefaults.reminderTimes },
            customReminderTimes = decodeCsv(routineCustomReminderTimesCsv).filterValidTimes()
        ).sanitized()
    }

    private fun RoutineSettings.sanitized(): RoutineSettings {
        return copy(
            goalId = goalId.ifBlank { RoutineDefaults.goalId },
            frequencyId = frequencyId.ifBlank { RoutineDefaults.frequencyId },
            dayIds = dayIds.filter { it.isNotBlank() }.toSet().ifEmpty { RoutineDefaults.dayIds },
            reminderTimes = reminderTimes.filterValidTimes().distinct().sorted(),
            customReminderTimes = customReminderTimes.filterValidTimes().distinct().sorted()
        )
    }

    private fun Collection<String>.filterValidTimes(): List<String> {
        return mapNotNull { time ->
            parseReminderTime(time)?.let { (hour, minute) -> formatReminderTime(hour, minute) }
        }
    }

    private fun decodeCsv(value: String): Set<String> {
        if (value.isBlank()) return emptySet()
        return value.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun encodeCsv(values: Collection<String>): String {
        return values
            .filter { it.isNotBlank() }
            .sorted()
            .joinToString(separator = ",")
    }

    private fun defaultSettings(accountId: String, now: Long): UserSettingsEntity {
        return UserSettingsEntity(
            accountId = accountId,
            createdAt = now,
            updatedAt = now,
            syncState = SyncState.LOCAL_ONLY
        )
    }
}
