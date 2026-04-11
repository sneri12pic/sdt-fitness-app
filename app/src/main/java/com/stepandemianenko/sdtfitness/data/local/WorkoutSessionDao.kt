package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: WorkoutSessionEntity): Long

    @Update
    suspend fun update(session: WorkoutSessionEntity)

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    fun observeById(sessionId: Long): Flow<WorkoutSessionEntity?>

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE status IN (:activeStatuses)
        ORDER BY startedAt DESC
        LIMIT 1
        """
    )
    suspend fun getMostRecentByStatuses(activeStatuses: List<String>): WorkoutSessionEntity?

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE status = :status
        ORDER BY completedAt DESC
        """
    )
    suspend fun getByStatus(status: String): List<WorkoutSessionEntity>

    @Query(
        """
        UPDATE workout_sessions
        SET status = :newStatus, completedAt = :endedAt
        WHERE status IN (:fromStatuses)
        """
    )
    suspend fun updateStatuses(
        fromStatuses: List<String>,
        newStatus: String,
        endedAt: Long
    )
}
