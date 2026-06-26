package com.stepandemianenko.sdtfitness.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure chart logic powering the session-review trend charts:
 * zoom-level series resolution, x-axis label thinning, metric detection, and axis scaling.
 */
class ProgressChartsTest {

    private val dayMillis = 24L * 60L * 60L * 1000L

    // region buildVisibleSeries

    @Test
    fun buildVisibleSeries_nonTimeTrend_returnsSeriesUnchangedWithoutLabels() {
        val result = buildVisibleSeries(
            actualValues = listOf(1f, 2f),
            targetValues = listOf(null, 3f),
            sessionSetValues = listOf(9f),
            timestamps = emptyList(),
            isTimeTrend = false,
            mode = ChartRangeMode.THREE_MONTHS
        )

        assertEquals(listOf(1f, 2f), result.actualValues)
        assertEquals(listOf(null, 3f), result.targetValues)
        assertTrue(result.pointLabels.isEmpty())
    }

    @Test
    fun buildVisibleSeries_sessionMode_plotsPerSetValuesWithSetLabels() {
        val result = buildVisibleSeries(
            actualValues = listOf(60f, 65f), // cross-session trend, ignored in Session mode
            targetValues = listOf(null, null),
            sessionSetValues = listOf(50f, 55f, 60f),
            timestamps = listOf(System.currentTimeMillis()),
            isTimeTrend = true,
            mode = ChartRangeMode.SESSION
        )

        assertEquals(listOf(50f, 55f, 60f), result.actualValues)
        assertEquals(listOf("Set 1", "Set 2", "Set 3"), result.pointLabels)
        // Targets aren't tracked per set, so they come back as nulls matching the point count.
        assertEquals(listOf(null, null, null), result.targetValues)
    }

    @Test
    fun buildVisibleSeries_sevenDays_keepsOnlyPointsInsideWindow() {
        val now = System.currentTimeMillis()
        val tenDaysAgo = now - 10 * dayMillis // outside 7-day window
        val threeDaysAgo = now - 3 * dayMillis // inside
        val oneDayAgo = now - 1 * dayMillis // inside

        val result = buildVisibleSeries(
            actualValues = listOf(10f, 20f, 30f),
            targetValues = listOf(null, null, null),
            sessionSetValues = emptyList(),
            timestamps = listOf(tenDaysAgo, threeDaysAgo, oneDayAgo),
            isTimeTrend = true,
            mode = ChartRangeMode.SEVEN_DAYS
        )

        assertEquals(listOf(20f, 30f), result.actualValues)
        assertEquals(2, result.pointLabels.size)
    }

    @Test
    fun buildVisibleSeries_threeMonths_excludesPointsOlderThanNinetyDays() {
        val now = System.currentTimeMillis()
        val hundredDaysAgo = now - 100 * dayMillis // outside 90-day window
        val eightyDaysAgo = now - 80 * dayMillis // inside

        val result = buildVisibleSeries(
            actualValues = listOf(5f, 6f),
            targetValues = listOf(null, null),
            sessionSetValues = emptyList(),
            timestamps = listOf(hundredDaysAgo, eightyDaysAgo),
            isTimeTrend = true,
            mode = ChartRangeMode.THREE_MONTHS
        )

        assertEquals(listOf(6f), result.actualValues)
    }

    // endregion

    // region selectXAxisLabels

    @Test
    fun selectXAxisLabels_threeOrFewer_returnedUnchanged() {
        assertEquals(listOf("a"), selectXAxisLabels(listOf("a")))
        assertEquals(listOf("a", "b"), selectXAxisLabels(listOf("a", "b")))
        assertEquals(listOf("a", "b", "c"), selectXAxisLabels(listOf("a", "b", "c")))
    }

    @Test
    fun selectXAxisLabels_moreThanThree_thinsToFirstMiddleLast() {
        assertEquals(listOf("a", "c", "e"), selectXAxisLabels(listOf("a", "b", "c", "d", "e")))
        // Even count: middle is size/2.
        assertEquals(listOf("a", "c", "d"), selectXAxisLabels(listOf("a", "b", "c", "d")))
    }

    // endregion

    // region resolveMetricKind

    @Test
    fun resolveMetricKind_detectsWeightRepsAndGeneric() {
        assertEquals(ChartMetricKind.WEIGHT, resolveMetricKind("kg"))
        assertEquals(ChartMetricKind.WEIGHT, resolveMetricKind("Total KG"))
        assertEquals(ChartMetricKind.REPS, resolveMetricKind("reps"))
        assertEquals(ChartMetricKind.GENERIC, resolveMetricKind("steps"))
    }

    // endregion

    // region buildDynamicAxisScale

    @Test
    fun buildDynamicAxisScale_empty_returnsDefaultDescendingLabelsFromZero() {
        val scale = buildDynamicAxisScale(
            values = emptyList(),
            metricKind = ChartMetricKind.WEIGHT,
            labelCount = 4
        )

        assertEquals(4, scale.labelsDescending.size)
        assertEquals(0f, scale.minValue, 0.001f)
        assertEquals(scale.maxValue, scale.labelsDescending.first(), 0.001f)
        assertEquals(scale.minValue, scale.labelsDescending.last(), 0.001f)
        assertEquals(scale.labelsDescending.sortedDescending(), scale.labelsDescending)
    }

    @Test
    fun buildDynamicAxisScale_framesValuesWithDescendingLabels() {
        val scale = buildDynamicAxisScale(
            values = listOf(60f, 72f),
            metricKind = ChartMetricKind.WEIGHT,
            labelCount = 4
        )

        assertEquals(4, scale.labelsDescending.size)
        assertTrue("max should sit at or above the highest value", scale.maxValue >= 72f)
        assertTrue("min should sit at or below the lowest value", scale.minValue <= 60f)
        assertTrue("min should never go negative", scale.minValue >= 0f)
        assertEquals(scale.maxValue, scale.labelsDescending.first(), 0.001f)
        assertEquals(scale.minValue, scale.labelsDescending.last(), 0.001f)
        assertEquals(scale.labelsDescending.sortedDescending(), scale.labelsDescending)
    }

    // endregion
}
