package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object AccountType {
    const val GUEST = "guest"
    const val AUTH = "auth"
}

object SyncState {
    const val LOCAL_ONLY = "local_only"
    const val PENDING_UPLOAD = "pending_upload"
    const val SYNCED = "synced"
    const val PENDING_DELETE = "pending_delete"
}

object WorkoutSessionStatus {
    const val PLANNED = "planned"
    const val ACTIVE = "active"
    const val COMPLETED = "completed"
    const val ABANDONED = "abandoned"
}

object SessionExerciseStatus {
    const val PENDING = "pending"
    const val ACTIVE = "active"
    const val COMPLETED = "completed"
    const val SKIPPED = "skipped"
}

object SetLogSource {
    const val MANUAL = "manual"
}

object DailyQuestId {
    const val WEIGHT_IN = "weight_in"
}

object DailyQuestCompletionSource {
    const val MANUAL = "manual"
    const val HEALTH_CONNECT = "health_connect"
}

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["createdAt"]),
        Index(value = ["remoteUserId"]),
        Index(value = ["authProvider", "remoteUserId"], unique = true)
    ]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val type: String = AccountType.GUEST,
    val createdAt: Long,
    val isActive: Boolean = false,
    val updatedAt: Long,
    val remoteUserId: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val authProvider: String? = null,
    val lastLoginAt: Long? = null
)

@Entity(
    tableName = "user_settings",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"], unique = true),
        Index(value = ["updatedAt"])
    ]
)
data class UserSettingsEntity(
    @PrimaryKey val accountId: String,
    val dailyStepsSource: String = "MANUAL",
    val dailyStepsTarget: Int = 5_000,
    val dailyStepsCurrent: Int = 0,
    val dailyStepsLastUpdated: Long? = null,
    val workoutCompletedDatesCsv: String = "",
    val activeMinutesToday: Int = 0,
    val routineCompletedDatesCsv: String = "",
    @ColumnInfo(defaultValue = "") val restDayDatesCsv: String = "",
    @ColumnInfo(defaultValue = "") val quickLogDatesCsv: String = "",
    val recoveryLogDate: String? = null,
    val recoveryLogOption: String? = null,
    val recoveryLogAtMillis: Long? = null,
    val quickLogDate: String? = null,
    val quickLogType: String? = null,
    val quickLogDurationMinutes: Int = 0,
    val quickLogTimestamp: Long? = null,
    val quickLogSource: String? = null,
    val healthConnectLastSyncedAt: Long? = null,
    val healthConnectLastImportedSteps: Int? = null,
    val healthConnectLatestWeightKg: Double? = null,
    @ColumnInfo(defaultValue = "0") val weightInQuestEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "0") val creatineQuestEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "5") val creatineTargetGrams: Int = 5,
    @ColumnInfo(defaultValue = "5") val creatinePortionGrams: Int = 5,
    @ColumnInfo(defaultValue = "0") val waterQuestEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "2000") val waterTargetMl: Int = 2000,
    @ColumnInfo(defaultValue = "250") val waterPortionMl: Int = 250,
    @ColumnInfo(defaultValue = "") val routineGoalId: String = "",
    @ColumnInfo(defaultValue = "") val routineFrequencyId: String = "",
    @ColumnInfo(defaultValue = "") val routineDayIdsCsv: String = "",
    @ColumnInfo(defaultValue = "1") val routineReminderEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "") val routineReminderTimesCsv: String = "",
    @ColumnInfo(defaultValue = "") val routineCustomReminderTimesCsv: String = "",
    @ColumnInfo(defaultValue = "OFF") val healthShareMode: String = "OFF",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY
)

@Entity(
    tableName = "exercise_catalog",
    indices = [
        Index(value = ["muscleGroup"]),
        Index(value = ["title"])
    ]
)
data class ExerciseCatalogEntity(
    @PrimaryKey val id: String,
    val title: String,
    val muscleGroup: String,
    val sortOrder: Int
)

@Entity(
    tableName = "daily_quest_records",
    primaryKeys = ["accountId", "questId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId", "date"]),
        Index(value = ["accountId", "questId", "date"], unique = true),
        Index(value = ["updatedAt"])
    ]
)
data class DailyQuestRecordEntity(
    val accountId: String,
    val questId: String,
    val date: String,
    val isAdded: Boolean = true,
    val isCompleted: Boolean = false,
    val completionSource: String? = null,
    val completedAt: Long? = null,
    val valueKg: Double? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY
)

@Entity(
    tableName = "creatine_intake_logs",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId", "date"]),
        Index(value = ["accountId", "date", "timestamp"]),
        Index(value = ["updatedAt"])
    ]
)
data class CreatineIntakeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val date: String,
    val amountGrams: Int,
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY
)

@Entity(
    tableName = "water_intake_logs",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId", "date"]),
        Index(value = ["accountId", "date", "timestamp"]),
        Index(value = ["updatedAt"])
    ]
)
data class WaterIntakeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: String,
    val date: String,
    val amountMl: Int,
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY
)

@Entity(
    tableName = "workout_plans",
    primaryKeys = ["accountId", "id"],
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId"]),
        Index(value = ["accountId", "name"]),
        Index(value = ["accountId", "updatedAt"])
    ]
)
data class WorkoutPlanEntity(
    val accountId: String,
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY
)

@Entity(
    tableName = "workout_plan_exercises",
    primaryKeys = ["accountId", "planId", "exerciseId"],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlanEntity::class,
            parentColumns = ["accountId", "id"],
            childColumns = ["accountId", "planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountId", "planId"]),
        Index(value = ["accountId", "planId", "exerciseOrder"], unique = true)
    ]
)
data class WorkoutPlanExerciseEntity(
    val accountId: String,
    val planId: String,
    val exerciseId: String,
    val exerciseOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY
)

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["status"]),
        Index(value = ["startedAt"]),
        Index(value = ["completedAt"]),
        Index(value = ["accountId", "status"]),
        Index(value = ["accountId", "completedAt"]),
        Index(value = ["accountId", "startedAt"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: String,
    val templateId: String? = null,
    val startedAt: Long,
    val completedAt: Long? = null,
    val status: String = WorkoutSessionStatus.ACTIVE,
    val currentExerciseIndex: Int = 0,
    val currentSetIndex: Int = 0,
    val totalSetsTarget: Int,
    val totalSetsCompleted: Int = 0,
    val totalRepsCompleted: Int = 0,
    val totalVolumeCompleted: Double = 0.0,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"]),
        Index(value = ["sessionId", "exerciseOrder"], unique = true),
        Index(value = ["accountId", "sessionId", "exerciseOrder"])
    ]
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: String,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseOrder: Int,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Int,
    val status: String = SessionExerciseStatus.PENDING,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY
)

@Entity(
    tableName = "session_set_logs",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionExerciseId"]),
        Index(value = ["completedAt"]),
        Index(value = ["accountId", "sessionId", "completedAt"])
    ]
)
data class SessionSetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val accountId: String,
    val sessionId: Long,
    val sessionExerciseId: Long,
    val setNumber: Int,
    val targetWeightKg: Int,
    val actualWeightKg: Int,
    val targetReps: Int,
    val actualReps: Int,
    val rpe: Int?,
    val completedAt: Long,
    val source: String = SetLogSource.MANUAL,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String = SyncState.LOCAL_ONLY
)

data class ExerciseSetResultRow(
    val actualWeightKg: Int,
    val actualReps: Int,
    val completedAt: Long
)

data class ExerciseSessionTrendRow(
    val sessionId: Long,
    val completedAtMillis: Long,
    val maxWeightKg: Int,
    val maxReps: Int
)

data class ProgressTotalsRow(
    val completedSessions: Int,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolume: Double,
    val bestLiftKg: Int
)

data class WorkoutDayRow(
    val workoutDay: String
)

data class TopExerciseVolumeRow(
    val exerciseName: String,
    val totalVolume: Double
)
