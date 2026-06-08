package com.stepandemianenko.sdtfitness.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ExerciseCatalogListItem(
    val id: String,
    val title: String,
    val muscleGroup: String
)

@Dao
interface ExerciseCatalogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exercises: List<ExerciseCatalogEntity>)

    @Query("SELECT COUNT(*) FROM exercise_catalog")
    suspend fun count(): Int

    @Query(
        """
        SELECT id, title, muscleGroup
        FROM exercise_catalog
        ORDER BY sortOrder ASC, title COLLATE NOCASE ASC
        """
    )
    fun observeListItems(): Flow<List<ExerciseCatalogListItem>>

    @Query(
        """
        SELECT id, title, muscleGroup
        FROM exercise_catalog
        WHERE id IN (:ids)
        ORDER BY sortOrder ASC, title COLLATE NOCASE ASC
        """
    )
    suspend fun getListItemsByIds(ids: List<String>): List<ExerciseCatalogListItem>
}
