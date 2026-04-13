package com.stepandemianenko.sdtfitness.data

import android.content.Context
import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import com.stepandemianenko.sdtfitness.data.health.HealthConnectManager
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import com.stepandemianenko.sdtfitness.data.repository.ProgressRepository
import com.stepandemianenko.sdtfitness.data.repository.WorkoutSessionRepository
import com.stepandemianenko.sdtfitness.home.HomeRepository

object AppGraph {
    @Volatile
    private var accountSessionManager: AccountSessionManager? = null

    @Volatile
    private var workoutSessionRepository: WorkoutSessionRepository? = null

    @Volatile
    private var progressRepository: ProgressRepository? = null

    @Volatile
    private var homeRepository: HomeRepository? = null

    @Volatile
    private var healthConnectManager: HealthConnectManager? = null

    fun accountSessionManager(context: Context): AccountSessionManager {
        return accountSessionManager ?: synchronized(this) {
            accountSessionManager ?: AccountSessionManager(
                database = WorkoutDatabase.getInstance(context)
            ).also { accountSessionManager = it }
        }
    }

    fun workoutSessionRepository(context: Context): WorkoutSessionRepository {
        return workoutSessionRepository ?: synchronized(this) {
            workoutSessionRepository ?: WorkoutSessionRepository(
                database = WorkoutDatabase.getInstance(context),
                accountSessionManager = accountSessionManager(context)
            ).also { workoutSessionRepository = it }
        }
    }

    fun progressRepository(context: Context): ProgressRepository {
        return progressRepository ?: synchronized(this) {
            progressRepository ?: ProgressRepository(
                database = WorkoutDatabase.getInstance(context),
                accountSessionManager = accountSessionManager(context)
            ).also { progressRepository = it }
        }
    }

    fun homeRepository(context: Context): HomeRepository {
        return homeRepository ?: synchronized(this) {
            homeRepository ?: HomeRepository(
                database = WorkoutDatabase.getInstance(context),
                accountSessionManager = accountSessionManager(context)
            ).also { homeRepository = it }
        }
    }

    fun healthConnectManager(context: Context): HealthConnectManager {
        return healthConnectManager ?: synchronized(this) {
            healthConnectManager ?: HealthConnectManager(context)
                .also { healthConnectManager = it }
        }
    }
}
