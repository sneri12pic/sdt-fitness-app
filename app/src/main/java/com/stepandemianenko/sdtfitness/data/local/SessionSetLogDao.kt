package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionSetLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(setLog: SessionSetLogEntity): Long

    @Query(
        """
        SELECT * FROM session_set_logs
        WHERE sessionId = :sessionId
        ORDER BY completedAt ASC
        """
    )
    suspend fun getForSession(sessionId: Long): List<SessionSetLogEntity>

    @Query(
        """
        SELECT * FROM session_set_logs
        WHERE sessionId = :sessionId
        ORDER BY completedAt ASC
        """
    )
    fun observeForSession(sessionId: Long): Flow<List<SessionSetLogEntity>>

    @Query(
        """
        SELECT ssl.actualWeightKg, ssl.actualReps, ssl.completedAt
        FROM session_set_logs AS ssl
        INNER JOIN session_exercises AS se ON se.id = ssl.sessionExerciseId
        INNER JOIN workout_sessions AS ws ON ws.id = ssl.sessionId
        WHERE se.exerciseId = :exerciseId
          AND ws.status = :completedStatus
          AND ws.id != :excludeSessionId
        ORDER BY ssl.completedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestCompletedResultForExercise(
        exerciseId: String,
        completedStatus: String,
        excludeSessionId: Long
    ): ExerciseSetResultRow?

    @Query(
        """
        SELECT ssl.actualWeightKg, ssl.actualReps, ssl.completedAt
        FROM session_set_logs AS ssl
        INNER JOIN session_exercises AS se ON se.id = ssl.sessionExerciseId
        INNER JOIN workout_sessions AS ws ON ws.id = ssl.sessionId
        WHERE se.exerciseId = :exerciseId
          AND ws.status = :completedStatus
          AND ws.id != :excludeSessionId
        ORDER BY ssl.actualWeightKg DESC, ssl.actualReps DESC, ssl.completedAt DESC
        LIMIT 1
        """
    )
    suspend fun getPersonalBestForExercise(
        exerciseId: String,
        completedStatus: String,
        excludeSessionId: Long
    ): ExerciseSetResultRow?

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM workout_sessions WHERE status = :completedStatus) AS completedSessions,
            (SELECT COALESCE(SUM(totalSetsCompleted), 0) FROM workout_sessions WHERE status = :completedStatus) AS totalSets,
            (SELECT COALESCE(SUM(totalRepsCompleted), 0) FROM workout_sessions WHERE status = :completedStatus) AS totalReps,
            (SELECT COALESCE(SUM(totalVolumeCompleted), 0.0) FROM workout_sessions WHERE status = :completedStatus) AS totalVolume,
            (
                SELECT COALESCE(MAX(ssl.actualWeightKg), 0)
                FROM session_set_logs AS ssl
                INNER JOIN workout_sessions AS ws ON ws.id = ssl.sessionId
                WHERE ws.status = :completedStatus
            ) AS bestLiftKg
        """
    )
    suspend fun getProgressTotals(completedStatus: String): ProgressTotalsRow

    @Query(
        """
        SELECT DISTINCT date(startedAt / 1000, 'unixepoch') AS workoutDay
        FROM workout_sessions
        WHERE status = :completedStatus
        ORDER BY workoutDay DESC
        """
    )
    suspend fun getCompletedWorkoutDays(completedStatus: String): List<WorkoutDayRow>

    @Query(
        """
        SELECT
            se.exerciseName AS exerciseName,
            COALESCE(SUM(ssl.actualWeightKg * ssl.actualReps), 0.0) AS totalVolume
        FROM session_set_logs AS ssl
        INNER JOIN session_exercises AS se ON se.id = ssl.sessionExerciseId
        INNER JOIN workout_sessions AS ws ON ws.id = ssl.sessionId
        WHERE ws.status = :completedStatus
        GROUP BY se.exerciseId, se.exerciseName
        ORDER BY totalVolume DESC
        LIMIT 1
        """
    )
    suspend fun getTopExerciseByVolume(completedStatus: String): TopExerciseVolumeRow?
}
