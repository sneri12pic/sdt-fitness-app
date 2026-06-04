package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(plan: WorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExercises(exercises: List<WorkoutPlanExerciseEntity>)

    @Query(
        """
        SELECT * FROM workout_plans
        WHERE accountId = :accountId
          AND deletedAt IS NULL
        ORDER BY updatedAt DESC, name COLLATE NOCASE ASC
        """
    )
    fun observePlans(accountId: String): Flow<List<WorkoutPlanEntity>>

    @Query(
        """
        SELECT * FROM workout_plan_exercises
        WHERE accountId = :accountId
          AND deletedAt IS NULL
        ORDER BY planId ASC, exerciseOrder ASC
        """
    )
    fun observePlanExercises(accountId: String): Flow<List<WorkoutPlanExerciseEntity>>

    @Query(
        """
        DELETE FROM workout_plan_exercises
        WHERE accountId = :accountId
          AND planId = :planId
        """
    )
    suspend fun deleteExercisesForPlan(accountId: String, planId: String)

    @Query(
        """
        DELETE FROM workout_plans
        WHERE accountId = :accountId
          AND id = :planId
        """
    )
    suspend fun deletePlan(accountId: String, planId: String)

    @Query("DELETE FROM workout_plans WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)
}
