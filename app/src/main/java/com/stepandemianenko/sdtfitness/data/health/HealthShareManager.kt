package com.stepandemianenko.sdtfitness.data.health

import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import com.stepandemianenko.sdtfitness.data.local.SyncState
import com.stepandemianenko.sdtfitness.data.local.UserSettingsEntity
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import com.stepandemianenko.sdtfitness.data.local.WorkoutSessionStatus
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** How app data (water, workouts) is pushed to Health Connect. */
enum class HealthShareMode {
    AUTO,   // pushed as soon as it's logged
    MANUAL, // pushed only via "Sync now"
    OFF;    // never pushed

    companion object {
        fun fromStorage(value: String?): HealthShareMode =
            entries.firstOrNull { it.name == value } ?: OFF
    }
}

/** Pushing is allowed only when sharing is on, HC is connected, and write permissions are granted. */
fun canPushToHealthConnect(
    mode: HealthShareMode,
    connected: Boolean,
    hasWritePermissions: Boolean
): Boolean = mode != HealthShareMode.OFF && connected && hasWritePermissions

/**
 * Owns the sharing mode (a `user_settings` column) and all pushes of app data to Health Connect.
 * Every push is best-effort: a Health Connect failure never blocks or fails the local write.
 */
class HealthShareManager(
    database: WorkoutDatabase,
    private val accountSessionManager: AccountSessionManager,
    private val healthConnect: HealthConnectManager
) {
    private val userSettingsDao = database.userSettingsDao()
    private val waterLogDao = database.waterIntakeLogDao()
    private val sessionDao = database.workoutSessionDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun mode(): HealthShareMode {
        val accountId = accountSessionManager.requireActiveAccountId()
        return HealthShareMode.fromStorage(userSettingsDao.getByAccountId(accountId)?.healthShareMode)
    }

    suspend fun setMode(mode: HealthShareMode) {
        val accountId = accountSessionManager.requireActiveAccountId()
        val now = System.currentTimeMillis()
        val current = userSettingsDao.getByAccountId(accountId)
            ?: UserSettingsEntity(accountId = accountId, createdAt = now, updatedAt = now)
        userSettingsDao.upsert(
            current.copy(healthShareMode = mode.name, updatedAt = now, syncState = SyncState.LOCAL_ONLY)
        )
    }

    suspend fun lastSyncedAt(): Long? {
        val accountId = accountSessionManager.requireActiveAccountId()
        return userSettingsDao.getByAccountId(accountId)?.healthConnectLastSyncedAt
    }

    /** Fire-and-forget push of one water log when mode is AUTO. */
    fun autoPushWater(logId: Long, amountMl: Int, timestampMillis: Long) {
        scope.launch {
            runCatching {
                if (canAutoPush()) healthConnect.writeWaterLog(logId, amountMl, timestampMillis)
            }
        }
    }

    /** Fire-and-forget push of one completed workout when mode is AUTO. */
    fun autoPushWorkout(sessionId: Long, startedAtMillis: Long, completedAtMillis: Long) {
        scope.launch {
            runCatching {
                if (canAutoPush()) healthConnect.writeWorkoutSession(sessionId, startedAtMillis, completedAtMillis)
            }
        }
    }

    /**
     * Push today's water logs and recently completed workouts, then stamp `healthConnectLastSyncedAt`.
     * clientRecordId keys make every push an upsert, so running this repeatedly can't duplicate.
     * ponytail: workouts limited to the last 30 days; add per-record synced flags if full history matters.
     */
    suspend fun syncNow(): Boolean {
        if (!canPushToHealthConnect(mode(), healthConnect.hasAllPermissions(), healthConnect.hasWritePermissions())) {
            return false
        }
        val accountId = accountSessionManager.requireActiveAccountId()
        val result = runCatching {
            val today = LocalDate.now().toString()
            waterLogDao.getForDate(accountId, today).forEach { log ->
                healthConnect.writeWaterLog(log.id, log.amountMl.coerceAtLeast(0), log.timestamp)
            }
            val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            sessionDao.getByStatus(accountId, WorkoutSessionStatus.COMPLETED)
                .filter { it.deletedAt == null && (it.completedAt ?: 0L) >= cutoff }
                .forEach { session ->
                    healthConnect.writeWorkoutSession(
                        sessionId = session.id,
                        startedAtMillis = session.startedAt,
                        completedAtMillis = session.completedAt ?: session.startedAt
                    )
                }
        }
        if (result.isSuccess) {
            val now = System.currentTimeMillis()
            userSettingsDao.getByAccountId(accountId)?.let {
                userSettingsDao.upsert(it.copy(healthConnectLastSyncedAt = now, updatedAt = now))
            }
        }
        return result.isSuccess
    }

    private suspend fun canAutoPush(): Boolean {
        val mode = mode()
        return mode == HealthShareMode.AUTO &&
            canPushToHealthConnect(mode, healthConnect.hasAllPermissions(), healthConnect.hasWritePermissions())
    }
}
