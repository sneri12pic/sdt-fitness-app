package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseCatalogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exercises: List<ExerciseCatalogEntity>)

    @Query("SELECT COUNT(*) FROM exercise_catalog")
    suspend fun count(): Int

    @Query(
        """
        SELECT * FROM exercise_catalog
        ORDER BY sortOrder ASC, title COLLATE NOCASE ASC
        """
    )
    fun observeAll(): Flow<List<ExerciseCatalogEntity>>
}
