package com.stepandemianenko.sdtfitness.data

import android.content.Context
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
import com.stepandemianenko.sdtfitness.data.repository.WorkoutPlanRepository
import com.stepandemianenko.sdtfitness.data.repository.ProgressRepositoryImpl
import com.stepandemianenko.sdtfitness.data.repository.WorkoutSessionRepository
import com.stepandemianenko.sdtfitness.domain.repository.ProgressRepository
import com.stepandemianenko.sdtfitness.domain.usecase.GetProgressSnapshotUseCase
import com.stepandemianenko.sdtfitness.home.HomeRepository
import com.stepandemianenko.sdtfitness.profile.ProfileRepository
import com.stepandemianenko.sdtfitness.profile.RoutineReminderScheduler

object AppGraph {
    @Volatile
    private var accountSessionManager: AccountSessionManager? = null

    @Volatile
    private var workoutSessionRepository: WorkoutSessionRepository? = null

    @Volatile
    private var workoutPlanRepository: WorkoutPlanRepository? = null

    @Volatile
    private var exerciseCatalogRepository: ExerciseCatalogRepository? = null

    @Volatile
    private var progressRepository: ProgressRepository? = null

    @Volatile
    private var progressMemoryCache: ProgressMemoryCache? = null

    @Volatile
    private var getProgressSnapshotUseCase: GetProgressSnapshotUseCase? = null

    @Volatile
    private var homeRepository: HomeRepository? = null

    @Volatile
    private var profileRepository: ProfileRepository? = null

    @Volatile
    private var routineReminderScheduler: RoutineReminderScheduler? = null

    @Volatile
    private var healthConnectManager: HealthConnectManager? = null

    @Volatile
    private var authRepository: AuthRepository? = null

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

    fun workoutPlanRepository(context: Context): WorkoutPlanRepository {
        return workoutPlanRepository ?: synchronized(this) {
            workoutPlanRepository ?: WorkoutPlanRepository(
                database = WorkoutDatabase.getInstance(context),
                accountSessionManager = accountSessionManager(context)
            ).also { workoutPlanRepository = it }
        }
    }

    fun exerciseCatalogRepository(context: Context): ExerciseCatalogRepository {
        return exerciseCatalogRepository ?: synchronized(this) {
            exerciseCatalogRepository ?: ExerciseCatalogRepository(
                database = WorkoutDatabase.getInstance(context)
            ).also { exerciseCatalogRepository = it }
        }
    }

    fun progressRepository(context: Context): ProgressRepository {
        return progressRepository ?: synchronized(this) {
            progressRepository ?: ProgressRepositoryImpl(
                database = WorkoutDatabase.getInstance(context),
                accountSessionManager = accountSessionManager(context),
                progressMemoryCache = progressMemoryCache()
            ).also { progressRepository = it }
        }
    }

    fun getProgressSnapshotUseCase(context: Context): GetProgressSnapshotUseCase {
        return getProgressSnapshotUseCase ?: synchronized(this) {
            getProgressSnapshotUseCase ?: GetProgressSnapshotUseCase(
                repository = progressRepository(context)
            ).also { getProgressSnapshotUseCase = it }
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

    fun profileRepository(context: Context): ProfileRepository {
        return profileRepository ?: synchronized(this) {
            profileRepository ?: ProfileRepository(
                database = WorkoutDatabase.getInstance(context),
                accountSessionManager = accountSessionManager(context),
                reminderScheduler = routineReminderScheduler(context)
            ).also { profileRepository = it }
        }
    }

    fun routineReminderScheduler(context: Context): RoutineReminderScheduler {
        return routineReminderScheduler ?: synchronized(this) {
            routineReminderScheduler ?: RoutineReminderScheduler(
                appContext = context.applicationContext
            ).also { routineReminderScheduler = it }
        }
    }

    fun healthConnectManager(context: Context): HealthConnectManager {
        return healthConnectManager ?: synchronized(this) {
            healthConnectManager ?: HealthConnectManager(context)
                .also { healthConnectManager = it }
        }
    }

    fun authRepository(context: Context): AuthRepository {
        return authRepository ?: synchronized(this) {
            authRepository ?: AuthRepositoryImpl(
                remoteAuthDataSource = HttpRemoteAuthDataSource(BuildConfig.AUTH_BASE_URL),
                secureSessionStore = SecureSessionStore(context.applicationContext),
                accountSessionManager = accountSessionManager(context)
            ).also { authRepository = it }
        }
    }

    private fun progressMemoryCache(): ProgressMemoryCache {
        return progressMemoryCache ?: synchronized(this) {
            progressMemoryCache ?: ProgressMemoryCache()
                .also { progressMemoryCache = it }
        }
    }
}
