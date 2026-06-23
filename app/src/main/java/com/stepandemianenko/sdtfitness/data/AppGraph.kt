package com.stepandemianenko.sdtfitness.data

import android.content.Context
import com.stepandemianenko.sdtfitness.App
import com.stepandemianenko.sdtfitness.auth.domain.AuthRepository
import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import com.stepandemianenko.sdtfitness.data.health.HealthConnectManager
import com.stepandemianenko.sdtfitness.data.repository.ExerciseCatalogRepository
import com.stepandemianenko.sdtfitness.data.repository.WorkoutPlanRepository
import com.stepandemianenko.sdtfitness.data.repository.WorkoutSessionRepository
import com.stepandemianenko.sdtfitness.domain.repository.ProgressRepository
import com.stepandemianenko.sdtfitness.domain.usecase.GetProgressSnapshotUseCase
import com.stepandemianenko.sdtfitness.home.HomeRepository
import com.stepandemianenko.sdtfitness.profile.ProfileRepository
import com.stepandemianenko.sdtfitness.profile.RoutineReminderScheduler

/**
 * Compatibility shim over [AppContainer]. Existing call sites still ask for a dependency by passing a
 * [Context]; this resolves the process-wide container from the [App] instance. New code should prefer
 * constructor injection from the container directly.
 */
object AppGraph {

    private fun container(context: Context): AppContainer =
        (context.applicationContext as App).container

    fun accountSessionManager(context: Context): AccountSessionManager =
        container(context).accountSessionManager

    fun workoutSessionRepository(context: Context): WorkoutSessionRepository =
        container(context).workoutSessionRepository

    fun workoutPlanRepository(context: Context): WorkoutPlanRepository =
        container(context).workoutPlanRepository

    fun exerciseCatalogRepository(context: Context): ExerciseCatalogRepository =
        container(context).exerciseCatalogRepository

    fun progressRepository(context: Context): ProgressRepository =
        container(context).progressRepository

    fun getProgressSnapshotUseCase(context: Context): GetProgressSnapshotUseCase =
        container(context).getProgressSnapshotUseCase

    fun homeRepository(context: Context): HomeRepository =
        container(context).homeRepository

    fun profileRepository(context: Context): ProfileRepository =
        container(context).profileRepository

    fun routineReminderScheduler(context: Context): RoutineReminderScheduler =
        container(context).routineReminderScheduler

    fun healthConnectManager(context: Context): HealthConnectManager =
        container(context).healthConnectManager

    fun authRepository(context: Context): AuthRepository =
        container(context).authRepository
}
