package com.stepandemianenko.sdtfitness.domain.model

data class ProgressSnapshot(
    val completedSessions: Int,
    val workoutDays: Int,
    val streakDays: Int,
    val last7DaySessions: Int,
    val previous7DaySessions: Int,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolumeKg: Double,
    val bestLiftKg: Int,
    val topExerciseName: String?,
    val topExerciseVolumeKg: Double?,
    val latestSessionCompletedToday: Boolean,
    val latestSessionVolumeKg: Double,
    val latestSessionSets: Int,
    val latestSessionReps: Int,
    val generatedAtMillis: Long
)
