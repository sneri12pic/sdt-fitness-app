package com.stepandemianenko.sdtfitness.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
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
