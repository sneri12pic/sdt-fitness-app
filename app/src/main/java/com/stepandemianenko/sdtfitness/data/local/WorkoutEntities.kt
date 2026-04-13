package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Entity
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

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["createdAt"])
    ]
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val type: String = AccountType.GUEST,
    val createdAt: Long,
    val isActive: Boolean = false,
    val updatedAt: Long
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
