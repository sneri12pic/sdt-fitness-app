package com.stepandemianenko.sdtfitness.data.repository

import com.stepandemianenko.sdtfitness.data.local.ExerciseCatalogDao
import com.stepandemianenko.sdtfitness.data.local.ExerciseCatalogListItem
import com.stepandemianenko.sdtfitness.data.local.SeedExerciseCatalog
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import kotlinx.coroutines.flow.Flow

class ExerciseCatalogRepository(
    database: WorkoutDatabase
) {
    private val exerciseCatalogDao: ExerciseCatalogDao = database.exerciseCatalogDao()

    fun observeExerciseList(): Flow<List<ExerciseCatalogListItem>> {
        return exerciseCatalogDao.observeListItems()
    }

    suspend fun getExerciseListItemsByIds(ids: Collection<String>): List<ExerciseCatalogListItem> {
        if (ids.isEmpty()) return emptyList()
        return exerciseCatalogDao.getListItemsByIds(ids.toList())
    }

    suspend fun ensureInitialSeeded(limit: Int) {
        if (exerciseCatalogDao.count() < limit) {
            exerciseCatalogDao.upsertAll(SeedExerciseCatalog.exercises.take(limit))
        }
    }

    suspend fun ensureSeeded() {
        if (exerciseCatalogDao.count() < SeedExerciseCatalog.exercises.size) {
            exerciseCatalogDao.upsertAll(SeedExerciseCatalog.exercises)
        }
    }
}
