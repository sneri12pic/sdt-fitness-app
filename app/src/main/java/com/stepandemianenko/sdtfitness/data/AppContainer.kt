package com.stepandemianenko.sdtfitness.data

import android.content.Context
import android.content.SharedPreferences
import com.stepandemianenko.sdtfitness.BuildConfig
import com.stepandemianenko.sdtfitness.auth.data.AuthRepositoryImpl
import com.stepandemianenko.sdtfitness.auth.data.HttpRemoteAuthDataSource
import com.stepandemianenko.sdtfitness.auth.data.SecureSessionStore
import com.stepandemianenko.sdtfitness.auth.domain.AuthRepository
import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import com.stepandemianenko.sdtfitness.data.cache.ProgressMemoryCache
import com.stepandemianenko.sdtfitness.data.health.HealthConnectManager
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import com.stepandemianenko.sdtfitness.data.repository.ExerciseCatalogRepository
import com.stepandemianenko.sdtfitness.data.repository.ProgressRepositoryImpl
import com.stepandemianenko.sdtfitness.data.repository.WorkoutPlanRepository
import com.stepandemianenko.sdtfitness.data.repository.WorkoutSessionRepository
import com.stepandemianenko.sdtfitness.domain.repository.ProgressRepository
import com.stepandemianenko.sdtfitness.domain.usecase.GetProgressSnapshotUseCase
import com.stepandemianenko.sdtfitness.home.HomeRepository
import com.stepandemianenko.sdtfitness.profile.ProfileRepository
import com.stepandemianenko.sdtfitness.profile.RoutineReminderScheduler

/**
 * Single source of the app's process-lifetime singletons. Built once in [com.stepandemianenko.sdtfitness.App.onCreate]
 * and reused for the life of the process, so the dependencies are constructed once on the main thread
 * at startup instead of via per-call double-checked locking.
 *
 * Account scoping is intentionally NOT done here: repositories observe [AccountSessionManager.accountScope]
 * at runtime, so a single instance per dependency is correct for both guest and signed-in accounts.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: WorkoutDatabase by lazy { WorkoutDatabase.getInstance(appContext) }

    private val progressMemoryCache: ProgressMemoryCache by lazy { ProgressMemoryCache() }

    val accountSessionManager: AccountSessionManager by lazy {
        AccountSessionManager(database = database)
    }

    val workoutSessionRepository: WorkoutSessionRepository by lazy {
        WorkoutSessionRepository(
            database = database,
            accountSessionManager = accountSessionManager
        )
    }

    val workoutPlanRepository: WorkoutPlanRepository by lazy {
        WorkoutPlanRepository(
            database = database,
            accountSessionManager = accountSessionManager
        )
    }

    val exerciseCatalogRepository: ExerciseCatalogRepository by lazy {
        ExerciseCatalogRepository(database = database)
    }

    val progressRepository: ProgressRepository by lazy {
        ProgressRepositoryImpl(
            database = database,
            accountSessionManager = accountSessionManager,
            progressMemoryCache = progressMemoryCache
        )
    }

    val getProgressSnapshotUseCase: GetProgressSnapshotUseCase by lazy {
        GetProgressSnapshotUseCase(repository = progressRepository)
    }

    val homeRepository: HomeRepository by lazy {
        HomeRepository(
            database = database,
            accountSessionManager = accountSessionManager
        )
    }

    val routineReminderScheduler: RoutineReminderScheduler by lazy {
        RoutineReminderScheduler(appContext = appContext)
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(
            database = database,
            accountSessionManager = accountSessionManager,
            reminderScheduler = routineReminderScheduler
        )
    }

    val healthConnectManager: HealthConnectManager by lazy {
        HealthConnectManager(appContext)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(
            remoteAuthDataSource = HttpRemoteAuthDataSource(BuildConfig.AUTH_BASE_URL),
            secureSessionStore = SecureSessionStore(appContext),
            accountSessionManager = accountSessionManager
        )
    }

    /** Backing store for the ongoing-workout rest-timer hint. */
    val ongoingWorkoutPreferences: SharedPreferences by lazy {
        appContext.getSharedPreferences("ongoing_workout_preferences", Context.MODE_PRIVATE)
    }
}
