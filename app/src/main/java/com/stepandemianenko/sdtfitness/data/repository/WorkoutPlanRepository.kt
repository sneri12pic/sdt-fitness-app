package com.stepandemianenko.sdtfitness.data.repository

import androidx.room.withTransaction
import com.stepandemianenko.sdtfitness.data.account.AccountSessionManager
import com.stepandemianenko.sdtfitness.data.local.SyncState
import com.stepandemianenko.sdtfitness.data.local.WorkoutDatabase
import com.stepandemianenko.sdtfitness.data.local.WorkoutPlanDao
import com.stepandemianenko.sdtfitness.data.local.WorkoutPlanEntity
import com.stepandemianenko.sdtfitness.data.local.WorkoutPlanExerciseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

data class WorkoutPlanSnapshot(
    val id: String,
    val name: String,
    val exerciseIds: Set<String>
)

class WorkoutPlanRepository(
    private val database: WorkoutDatabase,
    private val accountSessionManager: AccountSessionManager
) {
    private val workoutPlanDao: WorkoutPlanDao = database.workoutPlanDao()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observePlans(): Flow<List<WorkoutPlanSnapshot>> {
        return accountSessionManager.accountScope.flatMapLatest { scopeKey ->
            val accountId = scopeKey.accountId
            combine(
                workoutPlanDao.observePlans(accountId),
                workoutPlanDao.observePlanExercises(accountId)
            ) { plans, exercises ->
                val exercisesByPlan = exercises.groupBy { it.planId }
                plans.map { plan ->
                    WorkoutPlanSnapshot(
                        id = plan.id,
                        name = plan.name,
                        exerciseIds = exercisesByPlan[plan.id]
                            .orEmpty()
                            .sortedBy { it.exerciseOrder }
                            .mapTo(LinkedHashSet()) { it.exerciseId }
                    )
                }
            }
        }
    }

    suspend fun savePlan(
        id: String,
        name: String,
        exerciseIds: List<String>
    ) {
        val normalizedName = name.trim()
        val uniqueExerciseIds = exerciseIds.distinct()
        if (normalizedName.isBlank() || uniqueExerciseIds.isEmpty()) return

        val now = System.currentTimeMillis()
        val accountId = accountSessionManager.requireActiveAccountId()

        database.withTransaction {
            workoutPlanDao.upsert(
                WorkoutPlanEntity(
                    accountId = accountId,
                    id = id,
                    name = normalizedName,
                    createdAt = now,
                    updatedAt = now,
                    syncState = SyncState.LOCAL_ONLY
                )
            )
            workoutPlanDao.deleteExercisesForPlan(accountId = accountId, planId = id)
            workoutPlanDao.upsertExercises(
                uniqueExerciseIds.mapIndexed { index, exerciseId ->
                    WorkoutPlanExerciseEntity(
                        accountId = accountId,
                        planId = id,
                        exerciseId = exerciseId,
                        exerciseOrder = index,
                        createdAt = now,
                        updatedAt = now,
                        syncState = SyncState.LOCAL_ONLY
                    )
                }
            )
        }
    }

    suspend fun deletePlan(id: String) {
        val accountId = accountSessionManager.requireActiveAccountId()
        database.withTransaction {
            workoutPlanDao.deletePlan(accountId = accountId, planId = id)
        }
    }
}
