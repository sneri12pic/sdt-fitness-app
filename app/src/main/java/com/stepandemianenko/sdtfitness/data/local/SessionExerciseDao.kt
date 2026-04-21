package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionExerciseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(exercises: List<SessionExerciseEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(exercise: SessionExerciseEntity): Long

    @Query(
        """
        SELECT * FROM session_exercises
        WHERE accountId = :accountId
          AND sessionId = :sessionId
        ORDER BY exerciseOrder ASC
        """
    )
    suspend fun getForSession(accountId: String, sessionId: Long): List<SessionExerciseEntity>

    @Query(
        """
        SELECT * FROM session_exercises
        WHERE accountId = :accountId
          AND sessionId = :sessionId
        ORDER BY exerciseOrder ASC
        """
    )
    fun observeForSession(accountId: String, sessionId: Long): Flow<List<SessionExerciseEntity>>

    @Query(
        """
        UPDATE session_exercises
        SET status = :status, updatedAt = :updatedAt
        WHERE accountId = :accountId
          AND id = :sessionExerciseId
        """
    )
    suspend fun updateStatus(
        accountId: String,
        sessionExerciseId: Long,
        status: String,
        updatedAt: Long
    )

    @Query("DELETE FROM session_exercises WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)

    @Query(
        """
        UPDATE session_exercises
        SET targetSets = :targetSets, updatedAt = :updatedAt
        WHERE accountId = :accountId
          AND id = :sessionExerciseId
        """
    )
    suspend fun updateTargetSets(
        accountId: String,
        sessionExerciseId: Long,
        targetSets: Int,
        updatedAt: Long
    )

    @Query(
        """
        DELETE FROM session_exercises
        WHERE accountId = :accountId
          AND id = :sessionExerciseId
        """
    )
    suspend fun deleteById(
        accountId: String,
        sessionExerciseId: Long
    )

    @Query(
        """
        UPDATE session_exercises
        SET exerciseOrder = exerciseOrder - 1,
            updatedAt = :updatedAt
        WHERE accountId = :accountId
          AND sessionId = :sessionId
          AND exerciseOrder > :removedOrder
        """
    )
    suspend fun shiftExerciseOrderDownAfter(
        accountId: String,
        sessionId: Long,
        removedOrder: Int,
        updatedAt: Long
    )

    @Query(
        """
        UPDATE session_exercises
        SET exerciseOrder = exerciseOrder + 1,
            updatedAt = :updatedAt
        WHERE accountId = :accountId
          AND sessionId = :sessionId
          AND exerciseOrder >= :fromOrder
        """
    )
    suspend fun shiftExerciseOrderUpFrom(
        accountId: String,
        sessionId: Long,
        fromOrder: Int,
        updatedAt: Long
    )
}
