package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    tableName = "workout_sessions",
    indices = [
        Index(value = ["status"]),
        Index(value = ["startedAt"]),
        Index(value = ["completedAt"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val templateId: String? = null,
    val startedAt: Long,
    val completedAt: Long? = null,
    val status: String = WorkoutSessionStatus.ACTIVE,
    val currentExerciseIndex: Int = 0,
    val currentSetIndex: Int = 0,
    val totalSetsTarget: Int,
    val totalSetsCompleted: Int = 0,
    val totalRepsCompleted: Int = 0,
    val totalVolumeCompleted: Double = 0.0
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
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
        Index(value = ["sessionId", "exerciseOrder"], unique = true)
    ]
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseOrder: Int,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Int,
    val status: String = SessionExerciseStatus.PENDING
)

@Entity(
    tableName = "session_set_logs",
    foreignKeys = [
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
        Index(value = ["completedAt"])
    ]
)
data class SessionSetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val sessionExerciseId: Long,
    val setNumber: Int,
    val targetWeightKg: Int,
    val actualWeightKg: Int,
    val targetReps: Int,
    val actualReps: Int,
    val rpe: Int?,
    val completedAt: Long,
    val source: String = SetLogSource.MANUAL
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
