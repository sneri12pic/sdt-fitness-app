package com.stepandemianenko.sdtfitness.domain.usecase

import com.stepandemianenko.sdtfitness.domain.model.ProgressSnapshot
import com.stepandemianenko.sdtfitness.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class GetProgressSnapshotUseCase(
    private val repository: ProgressRepository
) {
    val cachedSnapshot: StateFlow<ProgressSnapshot?> = repository.cachedProgressSnapshot

    operator fun invoke(): Flow<ProgressSnapshot> {
        return repository.observeProgressSnapshot()
    }

    fun clearCache() {
        repository.clearProgressCache()
    }
}
