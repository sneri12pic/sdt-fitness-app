package com.stepandemianenko.sdtfitness.home

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class HomeRepository private constructor(
    context: Context
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _dashboardState = MutableStateFlow(readDashboardState())
    val dashboardState: StateFlow<HomeDashboardState> = _dashboardState.asStateFlow()

    fun setManualDailyQuest(
        targetSteps: Int,
        currentSteps: Int
    ) {
        val safeTarget = targetSteps.coerceAtLeast(1)
        val safeCurrent = currentSteps.coerceAtLeast(0)
        val now = System.currentTimeMillis()

        prefs.edit()
            .putString(KEY_DAILY_STEPS_SOURCE, DailyStepsSourceType.MANUAL.name)
            .putInt(KEY_DAILY_STEPS_TARGET, safeTarget)
            .putInt(KEY_DAILY_STEPS_CURRENT, safeCurrent)
            .putLong(KEY_DAILY_STEPS_LAST_UPDATED, now)
            .apply()
        publishUpdatedState()
    }

    fun updateStepsFromHealthConnect(
        currentSteps: Int,
        targetSteps: Int? = null
    ) {
        val safeCurrent = currentSteps.coerceAtLeast(0)
        val editor = prefs.edit()
            .putString(KEY_DAILY_STEPS_SOURCE, DailyStepsSourceType.HEALTH_CONNECT.name)
            .putInt(KEY_DAILY_STEPS_CURRENT, safeCurrent)
            .putLong(KEY_DAILY_STEPS_LAST_UPDATED, System.currentTimeMillis())

        targetSteps?.let { editor.putInt(KEY_DAILY_STEPS_TARGET, it.coerceAtLeast(1)) }
        editor.apply()
        publishUpdatedState()
    }

    fun setTodayWorkoutCompleted(
        completed: Boolean = true
    ) {
        val todayKey = LocalDate.now().toString()
        val existingWorkoutDates = prefs.getStringSet(KEY_WORKOUT_COMPLETED_DATES, emptySet()).orEmpty().toMutableSet()
        if (completed) {
            existingWorkoutDates.add(todayKey)
            addRoutineCompletion(todayKey)
        } else {
            existingWorkoutDates.remove(todayKey)
        }
        prefs.edit().putStringSet(KEY_WORKOUT_COMPLETED_DATES, existingWorkoutDates).apply()
        publishUpdatedState()
    }

    fun setTodayActiveMinutes(
        minutes: Int
    ) {
        val safeMinutes = minutes.coerceAtLeast(0)
        prefs.edit().putInt(KEY_ACTIVE_MINUTES_TODAY, safeMinutes).apply()
        if (safeMinutes > 0) {
            addRoutineCompletion(LocalDate.now().toString())
        }
        publishUpdatedState()
    }

    fun logTodayRecovery(
        option: RecoveryOption
    ) {
        val todayKey = LocalDate.now().toString()
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_RECOVERY_LOG_DATE, todayKey)
            .putString(KEY_RECOVERY_LOG_OPTION, option.name)
            .putLong(KEY_RECOVERY_LOG_AT_MILLIS, now)
            .apply()
        addRoutineCompletion(todayKey)
        publishUpdatedState()
    }

    private fun addRoutineCompletion(dateKey: String) {
        val existing = prefs.getStringSet(KEY_ROUTINE_COMPLETED_DATES, emptySet()).orEmpty().toMutableSet()
        existing.add(dateKey)
        prefs.edit().putStringSet(KEY_ROUTINE_COMPLETED_DATES, existing).apply()
    }

    private fun publishUpdatedState() {
        _dashboardState.value = readDashboardState()
    }

    private fun readDashboardState(): HomeDashboardState {
        val sourceType = prefs.getString(KEY_DAILY_STEPS_SOURCE, DailyStepsSourceType.MANUAL.name)
            ?.let {
                runCatching { DailyStepsSourceType.valueOf(it) }.getOrDefault(DailyStepsSourceType.MANUAL)
            }
            ?: DailyStepsSourceType.MANUAL

        val targetSteps = prefs.getInt(KEY_DAILY_STEPS_TARGET, DEFAULT_STEPS_TARGET).coerceAtLeast(1)
        val currentSteps = prefs.getInt(KEY_DAILY_STEPS_CURRENT, 0).coerceAtLeast(0)
        val lastUpdated = prefs.getLong(KEY_DAILY_STEPS_LAST_UPDATED, 0L).takeIf { it > 0L }

        val workoutCompletedDates = prefs.getStringSet(KEY_WORKOUT_COMPLETED_DATES, emptySet())
            .orEmpty()
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSet()

        val routineDates = prefs.getStringSet(KEY_ROUTINE_COMPLETED_DATES, emptySet())
            .orEmpty()
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSet()

        val workoutsCompletedToday = if (workoutCompletedDates.contains(LocalDate.now())) 1 else 0
        val activeMinutesToday = prefs.getInt(KEY_ACTIVE_MINUTES_TODAY, 0).coerceAtLeast(0)
        val todayKey = LocalDate.now().toString()
        val savedRecoveryDate = prefs.getString(KEY_RECOVERY_LOG_DATE, null)
        val savedRecoveryOption = if (savedRecoveryDate == todayKey) {
            prefs.getString(KEY_RECOVERY_LOG_OPTION, null)?.let { name ->
                runCatching { RecoveryOption.valueOf(name) }.getOrNull()
            }
        } else {
            null
        }
        val savedRecoveryAtMillis = prefs.getLong(KEY_RECOVERY_LOG_AT_MILLIS, 0L).takeIf {
            it > 0L && savedRecoveryDate == todayKey
        }

        return HomeDashboardState(
            dailyQuest = DailyQuestState(
                sourceType = sourceType,
                targetSteps = targetSteps,
                currentSteps = currentSteps,
                isManual = sourceType == DailyStepsSourceType.MANUAL,
                lastUpdatedMillis = lastUpdated
            ),
            dailyGoalSummary = DailyGoalSummaryState(
                stepsCurrent = currentSteps,
                stepsTarget = targetSteps,
                workoutsCompleted = workoutsCompletedToday,
                workoutsTarget = 1,
                activeMinutesCurrent = activeMinutesToday,
                activeMinutesTarget = DEFAULT_ACTIVE_MINUTES_TARGET
            ),
            routineStreakDates = routineDates,
            restDay = RestDayUiState(
                selectedOption = savedRecoveryOption ?: RecoveryOption.REST_DAY,
                savedTodayOption = savedRecoveryOption,
                savedTodayAtMillis = savedRecoveryAtMillis
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "home_dashboard_prefs"
        private const val KEY_DAILY_STEPS_SOURCE = "daily_steps_source"
        private const val KEY_DAILY_STEPS_TARGET = "daily_steps_target"
        private const val KEY_DAILY_STEPS_CURRENT = "daily_steps_current"
        private const val KEY_DAILY_STEPS_LAST_UPDATED = "daily_steps_last_updated"
        private const val KEY_WORKOUT_COMPLETED_DATES = "workout_completed_dates"
        private const val KEY_ACTIVE_MINUTES_TODAY = "active_minutes_today"
        private const val KEY_ROUTINE_COMPLETED_DATES = "routine_completed_dates"
        private const val KEY_RECOVERY_LOG_DATE = "recovery_log_date"
        private const val KEY_RECOVERY_LOG_OPTION = "recovery_log_option"
        private const val KEY_RECOVERY_LOG_AT_MILLIS = "recovery_log_at_millis"

        private const val DEFAULT_STEPS_TARGET = 5_000
        private const val DEFAULT_ACTIVE_MINUTES_TARGET = 30

        @Volatile
        private var INSTANCE: HomeRepository? = null

        fun getInstance(context: Context): HomeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HomeRepository(context).also { INSTANCE = it }
            }
        }
    }
}
