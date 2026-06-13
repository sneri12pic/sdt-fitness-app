package com.stepandemianenko.sdtfitness.domain.repository

import com.stepandemianenko.sdtfitness.data.repository.CompletedSessionReview
import com.stepandemianenko.sdtfitness.data.repository.CompletedSessionsHistory
import com.stepandemianenko.sdtfitness.data.repository.ExerciseTrend
import com.stepandemianenko.sdtfitness.domain.model.ProgressSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

interface ProgressRepository {
    val cachedProgressSnapshot: StateFlow<ProgressSnapshot?>

    fun observeProgressSnapshot(now: LocalDate = LocalDate.now()): Flow<ProgressSnapshot>

    fun clearProgressCache()

    suspend fun getCompletedSessionsHistory(now: LocalDate = LocalDate.now()): CompletedSessionsHistory

    suspend fun getCompletedSessionReview(sessionId: Long): CompletedSessionReview?

    suspend fun getExerciseTrends(sessionId: Long): List<ExerciseTrend>
}
