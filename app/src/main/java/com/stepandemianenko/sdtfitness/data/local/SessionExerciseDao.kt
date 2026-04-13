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
}
