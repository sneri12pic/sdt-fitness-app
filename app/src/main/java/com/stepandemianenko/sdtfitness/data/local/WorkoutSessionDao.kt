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

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE accountId = :accountId
          AND id = :sessionId
        LIMIT 1
        """
    )
    suspend fun getById(accountId: String, sessionId: Long): WorkoutSessionEntity?

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE accountId = :accountId
          AND id = :sessionId
        LIMIT 1
        """
    )
    fun observeById(accountId: String, sessionId: Long): Flow<WorkoutSessionEntity?>

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE accountId = :accountId
          AND status IN (:activeStatuses)
        ORDER BY startedAt DESC
        LIMIT 1
        """
    )
    suspend fun getMostRecentByStatuses(
        accountId: String,
        activeStatuses: List<String>
    ): WorkoutSessionEntity?

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE accountId = :accountId
          AND status = :status
        ORDER BY completedAt DESC
        """
    )
    suspend fun getByStatus(accountId: String, status: String): List<WorkoutSessionEntity>

    @Query(
        """
        UPDATE workout_sessions
        SET status = :newStatus, completedAt = :endedAt, updatedAt = :endedAt
        WHERE accountId = :accountId
          AND status IN (:fromStatuses)
        """
    )
    suspend fun updateStatuses(
        accountId: String,
        fromStatuses: List<String>,
        newStatus: String,
        endedAt: Long
    )

    @Query(
        """
        DELETE FROM workout_sessions
        WHERE accountId = :accountId
          AND id = :sessionId
        """
    )
    suspend fun deleteById(accountId: String, sessionId: Long)

    @Query("DELETE FROM workout_sessions WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)
}
