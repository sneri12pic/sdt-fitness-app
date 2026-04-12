package com.stepandemianenko.sdtfitness.data

import android.content.Context
import com.stepandemianenko.sdtfitness.data.health.HealthConnectManager
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import com.stepandemianenko.sdtfitness.data.repository.ProgressRepository
import com.stepandemianenko.sdtfitness.data.repository.WorkoutSessionRepository

object AppGraph {
    @Volatile
    private var workoutSessionRepository: WorkoutSessionRepository? = null

    @Volatile
    private var progressRepository: ProgressRepository? = null

    @Volatile
    private var healthConnectManager: HealthConnectManager? = null

    fun workoutSessionRepository(context: Context): WorkoutSessionRepository {
        return workoutSessionRepository ?: synchronized(this) {
            workoutSessionRepository ?: WorkoutSessionRepository(
                database = WorkoutDatabase.getInstance(context)
            ).also { workoutSessionRepository = it }
        }
    }

    fun progressRepository(context: Context): ProgressRepository {
        return progressRepository ?: synchronized(this) {
            progressRepository ?: ProgressRepository(
                database = WorkoutDatabase.getInstance(context)
            ).also { progressRepository = it }
        }
    }

    fun healthConnectManager(context: Context): HealthConnectManager {
        return healthConnectManager ?: synchronized(this) {
            healthConnectManager ?: HealthConnectManager(context)
                .also { healthConnectManager = it }
        }
    }
}
