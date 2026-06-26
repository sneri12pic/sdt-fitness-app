package com.stepandemianenko.sdtfitness.profile

import androidx.room.withTransaction
import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import com.stepandemianenko.sdtfitness.data.local.AccountType
import com.stepandemianenko.sdtfitness.data.local.DailyQuestId
import com.stepandemianenko.sdtfitness.data.local.SyncState
import com.stepandemianenko.sdtfitness.data.local.UserSettingsDao
import com.stepandemianenko.sdtfitness.data.local.UserSettingsEntity
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionStatus
import com.stepandemianenko.sdtfitness.home.calculateCurrentStreak
import java.time.LocalDate
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
    private val accountDao = database.accountDao()
    private val sessionDao = database.workoutSessionDao()
    private val questRecordDao = database.dailyQuestRecordDao()
    private val creatineLogDao = database.creatineIntakeLogDao()
    private val waterLogDao = database.waterIntakeLogDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _routineSettings = MutableStateFlow(RoutineSettings())
    val routineSettings: StateFlow<RoutineSettings> = _routineSettings.asStateFlow()

    private val _overview = MutableStateFlow(ProfileOverview())
    val overview: StateFlow<ProfileOverview> = _overview.asStateFlow()

    init {
        scope.launch {
            accountSessionManager.accountScope.collectLatest { scopeKey ->
                publishRoutine(accountId = scopeKey.accountId)
                publishOverview(accountId = scopeKey.accountId)
            }
        }
        scope.launch {
            val accountId = accountSessionManager.requireActiveAccountId()
            publishRoutine(accountId = accountId)
            publishOverview(accountId = accountId)
        }
    }

    /**
     * Account identity + all-time stats. Recomputed when the active account changes; a profile
     * screen doesn't need live-ticking counts, so this isn't wired to every workout/quest write.
     * ponytail: refresh-on-account-switch only; observe the source DAOs if live updates matter.
     */
    private suspend fun publishOverview(accountId: String) {
        val account = accountDao.getById(accountId)
        val settings = userSettingsDao.getByAccountId(accountId)
        val streakDates = settings?.routineCompletedDatesCsv
            ?.let(::decodeCsv)
            ?.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.toSet()
            ?: emptySet()
        _overview.value = ProfileOverview(
            displayName = account?.displayName?.takeIf { it.isNotBlank() }
                ?: account?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                ?: "Guest",
            isGuest = (account?.type ?: AccountType.GUEST) == AccountType.GUEST,
            stats = ProfileStats(
                workouts = sessionDao.countByStatus(accountId, WorkoutSessionStatus.COMPLETED),
                streakDays = calculateCurrentStreak(streakDates, LocalDate.now()),
                questsDone = questRecordDao.countCompleted(accountId)
            )
        )
    }

    /** App-generated data summary for the Health Connect hub (workouts all-time, water today). */
    suspend fun inAppShareSummary(): InAppShareSummary {
        val accountId = accountSessionManager.requireActiveAccountId()
        val today = LocalDate.now().toString()
        val waterMlToday = waterLogDao.getForDate(accountId, today).sumOf { it.amountMl.coerceAtLeast(0) }
        return InAppShareSummary(
            workoutsCompleted = sessionDao.countByStatus(accountId, WorkoutSessionStatus.COMPLETED),
            waterMlToday = waterMlToday
        )
    }

    /** Times each quest was completed within [range], one entry per quest, for the bar chart. */
    suspend fun questCounts(range: QuestChartRange): List<QuestBarPoint> {
        val accountId = accountSessionManager.requireActiveAccountId()
        val since = System.currentTimeMillis() - range.days * 24L * 60L * 60L * 1000L
        return listOf(
            QuestBarPoint("Weight-In", questRecordDao.countCompletedSince(accountId, DailyQuestId.WEIGHT_IN, since)),
            QuestBarPoint("Creatine", creatineLogDao.countSince(accountId, since)),
            QuestBarPoint("Water", waterLogDao.countSince(accountId, since))
        )
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
