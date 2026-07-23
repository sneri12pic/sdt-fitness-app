package com.stepandemianenko.sdtfitness.profile

import com.stepandemianenko.sdtfitness.data.health.HealthShareMode

/** Connection state of Health Connect, derived from SDK status + granted read permissions. */
enum class HealthConnectStatus {
    UNKNOWN,          // still checking
    UNAVAILABLE,      // Health Connect not installed / not supported on this device
    UPDATE_REQUIRED,  // provider needs an update
    NOT_CONNECTED,    // available but our read permissions aren't granted
    CONNECTED         // available and all read permissions granted
}

/** App-generated data the user could share later. Creatine is intentionally excluded (no HC type). */
data class InAppShareSummary(
    val workoutsCompleted: Int = 0,
    val waterMlToday: Int = 0
)

data class HealthConnectUiState(
    val status: HealthConnectStatus = HealthConnectStatus.UNKNOWN,
    val isLoading: Boolean = true,
    val todaySteps: Long? = null,
    val latestWeightKg: Double? = null,
    val inApp: InAppShareSummary = InAppShareSummary(),
    val shareMode: HealthShareMode = HealthShareMode.OFF,
    val hasWritePermissions: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncedAt: Long? = null
) {
    val isConnected: Boolean get() = status == HealthConnectStatus.CONNECTED

    /** Sharing is on but HC never got the write grants (e.g. connected back when we asked read-only). */
    val needsWriteAccess: Boolean get() =
        shareMode != HealthShareMode.OFF && isConnected && !hasWritePermissions
}
