package com.stepandemianenko.sdtfitness.data.health

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyStepsSample(
    val date: LocalDate,
    val steps: Long
)

data class WeightSample(
    val time: Instant,
    val weightKg: Double
)

class HealthConnectManager(
    context: Context
) {
    private val appContext = context.applicationContext

    val readPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    val writePermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    )

    val allPermissions: Set<String> = readPermissions + writePermissions

    fun getSdkStatus(): Int {
        return HealthConnectClient.getSdkStatus(appContext, PROVIDER_PACKAGE_NAME)
    }

    fun isAvailable(): Boolean = getSdkStatus() == HealthConnectClient.SDK_AVAILABLE

    fun isProviderUpdateRequired(): Boolean {
        return getSdkStatus() == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
    }

    suspend fun hasAllPermissions(): Boolean {
        if (!isAvailable()) return false
        val granted = healthConnectClient().permissionController.getGrantedPermissions()
        return granted.containsAll(readPermissions)
    }

    suspend fun hasWritePermissions(): Boolean {
        if (!isAvailable()) return false
        val granted = healthConnectClient().permissionController.getGrantedPermissions()
        return granted.containsAll(writePermissions)
    }

    /** Upserts one water log; [logId] keys the HC record so re-pushes don't duplicate. */
    suspend fun writeWaterLog(logId: Long, amountMl: Int, timestampMillis: Long) {
        if (!isAvailable()) return
        val start = Instant.ofEpochMilli(timestampMillis)
        val offset = ZoneId.systemDefault().rules.getOffset(start)
        healthConnectClient().insertRecords(
            listOf(
                HydrationRecord(
                    startTime = start,
                    startZoneOffset = offset,
                    endTime = start.plusSeconds(60),
                    endZoneOffset = offset,
                    volume = Volume.milliliters(amountMl.toDouble()),
                    metadata = Metadata.manualEntryWithId("water_$logId")
                )
            )
        )
    }

    /** Upserts one completed workout session; [sessionId] keys the HC record so re-pushes don't duplicate. */
    suspend fun writeWorkoutSession(sessionId: Long, startedAtMillis: Long, completedAtMillis: Long) {
        if (!isAvailable()) return
        val start = Instant.ofEpochMilli(startedAtMillis)
        val end = Instant.ofEpochMilli(completedAtMillis.coerceAtLeast(startedAtMillis + 60_000L))
        val zone = ZoneId.systemDefault()
        healthConnectClient().insertRecords(
            listOf(
                ExerciseSessionRecord(
                    startTime = start,
                    startZoneOffset = zone.rules.getOffset(start),
                    endTime = end,
                    endZoneOffset = zone.rules.getOffset(end),
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                    title = "Workout",
                    metadata = Metadata.manualEntryWithId("workout_$sessionId")
                )
            )
        )
    }

    /** Contract to launch the Health Connect permission sheet; pass [readPermissions] when launching. */
    fun requestPermissionsContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    /** Revoke everything we were granted — the "Disconnect from Health Connect" action. */
    suspend fun disconnect() {
        if (!isAvailable()) return
        healthConnectClient().permissionController.revokeAllPermissions()
    }

    suspend fun readTodaySteps(): Long {
        if (!isAvailable()) return 0L

        val now = Instant.now()
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        val result = healthConnectClient().aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
        )

        return result[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    suspend fun readDailyStepsHistory(days: Int): List<DailyStepsSample> {
        if (!isAvailable()) return emptyList()

        val safeDays = days.coerceIn(1, 31)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val firstDate = today.minusDays((safeDays - 1).toLong())
        val now = Instant.now()

        return (0 until safeDays).map { offset ->
            val date = firstDate.plusDays(offset.toLong())
            val start = date.atStartOfDay(zone).toInstant()
            val end = if (date == today) {
                now
            } else {
                date.plusDays(1).atStartOfDay(zone).toInstant()
            }
            val result = healthConnectClient().aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            DailyStepsSample(
                date = date,
                steps = result[StepsRecord.COUNT_TOTAL] ?: 0L
            )
        }
    }

    suspend fun readLatestWeightKg(): Double? {
        if (!isAvailable()) return null

        val response = healthConnectClient().readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(Instant.EPOCH, Instant.now()),
                ascendingOrder = false,
                pageSize = 1
            )
        )

        return response.records.firstOrNull()?.weight?.inKilograms
    }

    suspend fun readTodayWeightKg(): Double? {
        return readTodayWeightSample()?.weightKg
    }

    suspend fun readTodayWeightSample(): WeightSample? {
        if (!isAvailable()) return null

        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val startOfDay = LocalDate.now(zone)
            .atStartOfDay(zone)
            .toInstant()

        val response = healthConnectClient().readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
                ascendingOrder = false,
                pageSize = 1
            )
        )

        return response.records.firstOrNull()?.let { record ->
            WeightSample(
                time = record.time,
                weightKg = record.weight.inKilograms
            )
        }
    }

    suspend fun readWeightHistory(days: Int, limit: Int = 20): List<WeightSample> {
        if (!isAvailable()) return emptyList()

        val safeDays = days.coerceIn(1, 365)
        val safeLimit = limit.coerceIn(1, 100)
        val now = Instant.now()
        val start = now.minusSeconds(safeDays.toLong() * 24L * 60L * 60L)
        val response = healthConnectClient().readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, now),
                ascendingOrder = false,
                pageSize = safeLimit
            )
        )

        return response.records.asReversed().map { record ->
            WeightSample(
                time = record.time,
                weightKg = record.weight.inKilograms
            )
        }
    }

    private fun healthConnectClient(): HealthConnectClient {
        return HealthConnectClient.getOrCreate(appContext)
    }

    private companion object {
        private const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
    }
}
