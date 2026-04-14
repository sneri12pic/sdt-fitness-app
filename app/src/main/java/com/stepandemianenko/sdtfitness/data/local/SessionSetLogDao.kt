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
        WHERE accountId = :accountId
          AND sessionId = :sessionId
        ORDER BY completedAt ASC
        """
    )
    suspend fun getForSession(accountId: String, sessionId: Long): List<SessionSetLogEntity>

    @Query(
        """
        SELECT * FROM session_set_logs
        WHERE accountId = :accountId
          AND sessionId = :sessionId
        ORDER BY completedAt ASC
        """
    )
    fun observeForSession(accountId: String, sessionId: Long): Flow<List<SessionSetLogEntity>>

    @Query(
        """
        SELECT ssl.actualWeightKg, ssl.actualReps, ssl.completedAt
        FROM session_set_logs AS ssl
        INNER JOIN session_exercises AS se ON se.id = ssl.sessionExerciseId
        INNER JOIN workout_sessions AS ws ON ws.id = ssl.sessionId
        WHERE ssl.accountId = :accountId
          AND se.accountId = :accountId
          AND ws.accountId = :accountId
          AND se.exerciseId = :exerciseId
          AND ws.status = :completedStatus
          AND ws.id != :excludeSessionId
        ORDER BY ssl.completedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestCompletedResultForExercise(
        accountId: String,
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
        WHERE ssl.accountId = :accountId
          AND se.accountId = :accountId
          AND ws.accountId = :accountId
          AND se.exerciseId = :exerciseId
          AND ws.status = :completedStatus
          AND ws.id != :excludeSessionId
        ORDER BY ssl.actualWeightKg DESC, ssl.actualReps DESC, ssl.completedAt DESC
        LIMIT 1
        """
    )
    suspend fun getPersonalBestForExercise(
        accountId: String,
        exerciseId: String,
        completedStatus: String,
        excludeSessionId: Long
    ): ExerciseSetResultRow?

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM workout_sessions WHERE accountId = :accountId AND status = :completedStatus) AS completedSessions,
            (SELECT COALESCE(SUM(totalSetsCompleted), 0) FROM workout_sessions WHERE accountId = :accountId AND status = :completedStatus) AS totalSets,
            (SELECT COALESCE(SUM(totalRepsCompleted), 0) FROM workout_sessions WHERE accountId = :accountId AND status = :completedStatus) AS totalReps,
            (SELECT COALESCE(SUM(totalVolumeCompleted), 0.0) FROM workout_sessions WHERE accountId = :accountId AND status = :completedStatus) AS totalVolume,
            (
                SELECT COALESCE(MAX(ssl.actualWeightKg), 0)
                FROM session_set_logs AS ssl
                INNER JOIN workout_sessions AS ws ON ws.id = ssl.sessionId
                WHERE ssl.accountId = :accountId
                  AND ws.accountId = :accountId
                  AND ws.status = :completedStatus
            ) AS bestLiftKg
        """
    )
    suspend fun getProgressTotals(accountId: String, completedStatus: String): ProgressTotalsRow

    @Query(
        """
        SELECT DISTINCT date(startedAt / 1000, 'unixepoch') AS workoutDay
        FROM workout_sessions
        WHERE accountId = :accountId
          AND status = :completedStatus
        ORDER BY workoutDay DESC
        """
    )
    suspend fun getCompletedWorkoutDays(accountId: String, completedStatus: String): List<WorkoutDayRow>

    @Query(
        """
        SELECT
            se.exerciseName AS exerciseName,
            COALESCE(SUM(ssl.actualWeightKg * ssl.actualReps), 0.0) AS totalVolume
        FROM session_set_logs AS ssl
        INNER JOIN session_exercises AS se ON se.id = ssl.sessionExerciseId
        INNER JOIN workout_sessions AS ws ON ws.id = ssl.sessionId
        WHERE ssl.accountId = :accountId
          AND se.accountId = :accountId
          AND ws.accountId = :accountId
          AND ws.status = :completedStatus
        GROUP BY se.exerciseId, se.exerciseName
        ORDER BY totalVolume DESC
        LIMIT 1
        """
    )
    suspend fun getTopExerciseByVolume(accountId: String, completedStatus: String): TopExerciseVolumeRow?

    @Query("DELETE FROM session_set_logs WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)

    @Query(
        """
        UPDATE session_set_logs
        SET actualWeightKg = :actualWeightKg,
            actualReps = :actualReps,
            rpe = :rpe,
            updatedAt = :updatedAt
        WHERE accountId = :accountId
          AND id = :setLogId
        """
    )
    suspend fun updateLoggedSetValues(
        accountId: String,
        setLogId: Long,
        actualWeightKg: Int,
        actualReps: Int,
        rpe: Int?,
        updatedAt: Long
    )

    @Query(
        """
        DELETE FROM session_set_logs
        WHERE accountId = :accountId
          AND id = :setLogId
        """
    )
    suspend fun deleteById(
        accountId: String,
        setLogId: Long
    )

    @Query(
        """
        UPDATE session_set_logs
        SET setNumber = setNumber - 1,
            updatedAt = :updatedAt
        WHERE accountId = :accountId
          AND sessionId = :sessionId
          AND sessionExerciseId = :sessionExerciseId
          AND setNumber > :removedSetNumber
        """
    )
    suspend fun shiftSetNumbersDownAfter(
        accountId: String,
        sessionId: Long,
        sessionExerciseId: Long,
        removedSetNumber: Int,
        updatedAt: Long
    )
}
