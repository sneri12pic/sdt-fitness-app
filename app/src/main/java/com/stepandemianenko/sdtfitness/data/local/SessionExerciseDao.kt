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
        WHERE sessionId = :sessionId
        ORDER BY exerciseOrder ASC
        """
    )
    suspend fun getForSession(sessionId: Long): List<SessionExerciseEntity>

    @Query(
        """
        SELECT * FROM session_exercises
        WHERE sessionId = :sessionId
        ORDER BY exerciseOrder ASC
        """
    )
    fun observeForSession(sessionId: Long): Flow<List<SessionExerciseEntity>>

    @Query("UPDATE session_exercises SET status = :status WHERE id = :sessionExerciseId")
    suspend fun updateStatus(sessionExerciseId: Long, status: String)
}
