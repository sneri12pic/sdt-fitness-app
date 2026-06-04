package com.stepandemianenko.sdtfitness.data.repository

import com.stepandemianenko.sdtfitness.data.local.ExerciseCatalogDao
import com.stepandemianenko.sdtfitness.data.local.ExerciseCatalogEntity
import com.stepandemianenko.sdtfitness.data.local.SeedExerciseCatalog
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import kotlinx.coroutines.flow.Flow

class ExerciseCatalogRepository(
    database: WorkoutDatabase
) {
    private val exerciseCatalogDao: ExerciseCatalogDao = database.exerciseCatalogDao()

    fun observeExercises(): Flow<List<ExerciseCatalogEntity>> {
        return exerciseCatalogDao.observeAll()
    }

    suspend fun ensureSeeded() {
        if (exerciseCatalogDao.count() < SeedExerciseCatalog.exercises.size) {
            exerciseCatalogDao.upsertAll(SeedExerciseCatalog.exercises)
        }
    }
}
