package com.stepandemianenko.sdtfitness.data.cache

import com.stepandemianenko.sdtfitness.domain.model.ProgressSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProgressMemoryCache {
    private val _snapshot = MutableStateFlow<ProgressSnapshot?>(null)
    val snapshot: StateFlow<ProgressSnapshot?> = _snapshot.asStateFlow()
    private var accountId: String? = null

    fun update(snapshot: ProgressSnapshot) {
        _snapshot.value = snapshot
    }

    fun update(accountId: String, snapshot: ProgressSnapshot) {
        this.accountId = accountId
        _snapshot.value = snapshot
    }

    fun snapshotFor(accountId: String): ProgressSnapshot? {
        return _snapshot.value.takeIf { this.accountId == accountId }
    }

    fun clear() {
        accountId = null
        _snapshot.value = null
    }
}
