package com.stepandemianenko.sdtfitness.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ProgressPrimaryText = Color(0xFF4F2912)
private val ProgressSecondaryText = Color(0xFF6B4637)
private val ProgressTileBackground = Color(0xFFF1D5CB)
private val ProgressAccent = Color(0xFFF08A67)
private val ProgressPositive = Color(0xFF69C47A)
private val ProgressDivider = Color(0x40A67B6C)
private val ProgressTargetPlaceholder = Color(0xA0A67B6C)
private val ProgressSelectionBg = Color(0xFFFDEDE7)
private val ProgressRangeChipBg = Color(0xFFEBCBC0)

// Discrete zoom levels, ordered from most zoomed-in (SESSION, per-set entries of the reviewed
// session) to most zoomed-out (THREE_MONTHS). Pinch-to-zoom steps through this order.
internal enum class ChartRangeMode(
    val label: String,
    val days: Int
) {
    SESSION("Session", 0),
    SEVEN_DAYS("7 days", 7),
    THREE_MONTHS("3 months", 90)
}

internal enum class ChartMetricKind {
    WEIGHT,
    REPS,
    GENERIC
}

data class SetMetricChartUiModel(
    val title: String,
    val actualLabel: String,
    val actualValues: List<Float>,
    val targetValues: List<Float?>,
    val unitLabel: String,
    val pointTimestampsMillis: List<Long> = emptyList(),
    // Per-set values logged during the reviewed session (one entry per set). When present alongside
    // a time-trend series, the chart offers a most-zoomed-in "Session" level that plots these.
    val sessionSetValues: List<Float> = emptyList()
)

data class DailyStepsBarChartPoint(
    val dateLabel: String,
    val steps: Int
)

@Composable
fun WeeklySessionsComparisonGraph(
    thisWeekSessions: Int,
    lastWeekSessions: Int,
    modifier: Modifier = Modifier
) {
    val maxValue = max(1, max(thisWeekSessions, lastWeekSessions))

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Sessions this week vs last week",
            color = ProgressPrimaryText,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            WeeklyComparisonBar(
                modifier = Modifier.weight(1f),
                label = "This week",
                value = thisWeekSessions,
                maxValue = maxValue,
                color = ProgressAccent
            )
            WeeklyComparisonBar(
                modifier = Modifier.weight(1f),
                label = "Last week",
                value = lastWeekSessions,
                maxValue = maxValue,
                color = ProgressPositive
            )
        }
    }
}

@Composable
private fun WeeklyComparisonBar(
    modifier: Modifier,
    label: String,
    value: Int,
    maxValue: Int,
    color: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = value.toString(),
            color = ProgressPrimaryText,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .width(42.dp)
                .weight(1f)
                .background(color = ProgressTileBackground, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            val heightFraction = (value.toFloat() / maxValue.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(heightFraction)
                    .background(color = color, shape = RoundedCornerShape(8.dp))
            )
        }
        Text(
            text = label,
            color = ProgressSecondaryText,
            fontSize = 12.sp,
            lineHeight = 13.sp
        )
    }
}

@Composable
fun DailyStepsBarChart(
    points: List<DailyStepsBarChartPoint>,
    modifier: Modifier = Modifier
) {
    val safePoints = points.ifEmpty {
        listOf(DailyStepsBarChartPoint(dateLabel = "Today", steps = 0))
    }
    val maxSteps = safePoints.maxOfOrNull { it.steps }?.coerceAtLeast(1) ?: 1

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Daily Steps",
            color = ProgressPrimaryText,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(color = ProgressTileBackground, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                safePoints.forEach { point ->
                    DailyStepsBar(
                        modifier = Modifier.weight(1f),
                        point = point,
                        maxSteps = maxSteps
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyStepsBar(
    modifier: Modifier,
    point: DailyStepsBarChartPoint,
    maxSteps: Int
) {
    val heightFraction = (point.steps.toFloat() / maxSteps.toFloat()).coerceIn(0f, 1f)
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = "%,d".format(point.steps.coerceAtLeast(0)),
            color = ProgressPrimaryText,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .padding(top = 5.dp, bottom = 6.dp)
                .width(26.dp)
                .weight(1f)
                .background(color = ProgressRangeChipBg, shape = RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(heightFraction)
                    .background(color = ProgressAccent, shape = RoundedCornerShape(8.dp))
            )
        }
        Text(
            text = point.dateLabel,
            color = ProgressSecondaryText,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ExerciseSetMetricChart(
    chart: SetMetricChartUiModel,
    modifier: Modifier = Modifier
) {
    // Time-trend charts carry one timestamp per point; the range chips then filter by date window.
    // Charts without timestamps (Home weight dialog, Progress) keep showing every point and hide the chips.
    val isTimeTrend = chart.pointTimestampsMillis.isNotEmpty()
    val hasSessionData = isTimeTrend && chart.sessionSetValues.isNotEmpty()
    // Zoom levels offered for this chart, most zoomed-in first. The "Session" level only appears
    // when per-set data is available; pinch-to-zoom steps through this same ordered list.
    val availableModes = remember(hasSessionData) {
        ChartRangeMode.values().filter { hasSessionData || it != ChartRangeMode.SESSION }
    }
    var rangeMode by remember(chart.title, chart.unitLabel, hasSessionData) {
        mutableStateOf(if (hasSessionData) ChartRangeMode.SESSION else ChartRangeMode.THREE_MONTHS)
    }
    val metricKind = remember(chart.unitLabel) {
        resolveMetricKind(chart.unitLabel)
    }

    val visibleSeries = remember(chart.actualValues, chart.targetValues, chart.sessionSetValues, chart.pointTimestampsMillis, isTimeTrend, rangeMode) {
        buildVisibleSeries(
            actualValues = chart.actualValues,
            targetValues = chart.targetValues,
            sessionSetValues = chart.sessionSetValues,
            timestamps = chart.pointTimestampsMillis,
            isTimeTrend = isTimeTrend,
            mode = rangeMode
        )
    }
    val visibleActualValues = visibleSeries.actualValues
    val visibleTargetValues = visibleSeries.targetValues
    val xAxisLabels = remember(visibleSeries.pointLabels) {
        selectXAxisLabels(visibleSeries.pointLabels)
    }

    val hasAnyActual = visibleActualValues.isNotEmpty()
    val targetSeriesHasValues = visibleTargetValues.any { it != null }

    val scale = remember(visibleActualValues, visibleTargetValues, metricKind) {
        buildDynamicAxisScale(
            values = visibleActualValues + visibleTargetValues.mapNotNull { it },
            metricKind = metricKind,
            labelCount = 4
        )
    }

    var selectedPointIndex by remember {
        mutableIntStateOf(if (visibleActualValues.isNotEmpty()) visibleActualValues.lastIndex else -1)
    }
    var graphSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val leftPaddingPx = with(density) { 8.dp.toPx() }
    val rightPaddingPx = with(density) { 8.dp.toPx() }
    val topPaddingPx = with(density) { 8.dp.toPx() }
    val bottomPaddingPx = with(density) { 12.dp.toPx() }

    val actualPoints = remember(
        visibleActualValues,
        graphSize,
        scale,
        leftPaddingPx,
        rightPaddingPx,
        topPaddingPx,
        bottomPaddingPx
    ) {
        if (graphSize.width <= 0 || graphSize.height <= 0 || visibleActualValues.isEmpty()) {
            emptyList()
        } else {
            computeGraphPoints(
                values = visibleActualValues,
                canvasWidth = graphSize.width.toFloat(),
                canvasHeight = graphSize.height.toFloat(),
                scaleMin = scale.minValue,
                scaleMax = scale.maxValue,
                leftPaddingPx = leftPaddingPx,
                rightPaddingPx = rightPaddingPx,
                topPaddingPx = topPaddingPx,
                bottomPaddingPx = bottomPaddingPx
            )
        }
    }

    LaunchedEffect(visibleActualValues) {
        selectedPointIndex = if (visibleActualValues.isNotEmpty()) visibleActualValues.lastIndex else -1
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = chart.title,
            color = ProgressPrimaryText,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        ChartLegend(
            actualLabel = chart.actualLabel,
            targetLabel = "Target (pending)"
        )

        if (isTimeTrend) {
            RangeModeControl(
                availableModes = availableModes,
                selectedMode = rangeMode,
                onModeSelected = { rangeMode = it }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(color = ProgressTileBackground, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            if (!hasAnyActual) {
                Text(
                    text = when {
                        !isTimeTrend -> "No recorded sets yet"
                        rangeMode == ChartRangeMode.SESSION -> "No sets recorded for this session"
                        else -> "No sessions in this window"
                    },
                    color = ProgressSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onSizeChanged { graphSize = it }
                        .pointerInput(availableModes) {
                            // Pinch the chart to step zoom levels: spreading fingers zooms in toward
                            // the per-set Session view, pinching together zooms out to wider windows.
                            var cumulativeZoom = 1f
                            detectTransformGestures { _, _, zoom, _ ->
                                cumulativeZoom *= zoom
                                val currentIndex = availableModes.indexOf(rangeMode).coerceAtLeast(0)
                                when {
                                    cumulativeZoom >= 1.25f -> {
                                        rangeMode = availableModes[(currentIndex - 1).coerceAtLeast(0)]
                                        cumulativeZoom = 1f
                                    }
                                    cumulativeZoom <= 0.8f -> {
                                        rangeMode = availableModes[(currentIndex + 1).coerceAtMost(availableModes.lastIndex)]
                                        cumulativeZoom = 1f
                                    }
                                }
                            }
                        }
                        .pointerInput(visibleActualValues, actualPoints) {
            detectTapGestures { tap ->
                                if (actualPoints.isEmpty()) {
                                    selectedPointIndex = -1
                                    return@detectTapGestures
                                }

                                val tapThresholdPx = 18.dp.toPx()
                                val nearest = actualPoints.withIndex()
                                    .minByOrNull { (_, point) ->
                                        distance(tap, point)
                                    }
                                val nearestDistance = nearest?.let { distance(tap, it.value) } ?: Float.MAX_VALUE
                                selectedPointIndex = if (nearestDistance <= tapThresholdPx) {
                                    nearest?.index ?: -1
                                } else {
                                    -1
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val points = computeGraphPoints(
                            values = visibleActualValues,
                            canvasWidth = size.width,
                            canvasHeight = size.height,
                            scaleMin = scale.minValue,
                            scaleMax = scale.maxValue,
                            leftPaddingPx = leftPaddingPx,
                            rightPaddingPx = rightPaddingPx,
                            topPaddingPx = topPaddingPx,
                            bottomPaddingPx = bottomPaddingPx
                        )

                        val left = leftPaddingPx
                        val right = size.width - rightPaddingPx
                        val top = topPaddingPx
                        val bottom = size.height - bottomPaddingPx
                        val graphHeight = (bottom - top).coerceAtLeast(1f)

                        repeat(4) { step ->
                            val y = top + (graphHeight / 3f) * step
                            drawLine(
                                color = ProgressDivider,
                                start = Offset(left, y),
                                end = Offset(right, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        if (points.isNotEmpty()) {
                            val path = Path()
                            points.forEachIndexed { index, p ->
                                if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                            }
                            drawPath(
                                path = path,
                                color = ProgressAccent,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )

                            points.forEachIndexed { index, p ->
                                val isSelected = index == selectedPointIndex
                                if (isSelected) {
                                    drawCircle(
                                        color = ProgressSelectionBg,
                                        radius = 6.dp.toPx(),
                                        center = p
                                    )
                                }
                                drawCircle(
                                    color = if (isSelected) ProgressPrimaryText else ProgressAccent,
                                    radius = if (isSelected) 4.dp.toPx() else 3.dp.toPx(),
                                    center = p
                                )
                            }
                        }

                        if (targetSeriesHasValues) {
                            var previousPoint: Offset? = null
                            visibleTargetValues.forEachIndexed { index, target ->
                                if (target == null) {
                                    previousPoint = null
                                } else {
                                    val targetPoint = computeSingleGraphPoint(
                                        index = index,
                                        value = target,
                                        totalPoints = max(visibleActualValues.size, visibleTargetValues.size),
                                        canvasWidth = size.width,
                                        canvasHeight = size.height,
                                        scaleMin = scale.minValue,
                                        scaleMax = scale.maxValue,
                                        leftPaddingPx = leftPaddingPx,
                                        rightPaddingPx = rightPaddingPx,
                                        topPaddingPx = topPaddingPx,
                                        bottomPaddingPx = bottomPaddingPx
                                    )
                                    previousPoint?.let { prev ->
                                        drawLine(
                                            color = ProgressTargetPlaceholder,
                                            start = prev,
                                            end = targetPoint,
                                            strokeWidth = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
                                            cap = StrokeCap.Round
                                        )
                                    }
                                    previousPoint = targetPoint
                                }
                            }
                        }
                    }

                    val selectedPoint = actualPoints.getOrNull(selectedPointIndex)
                    val selectedValue = visibleActualValues.getOrNull(selectedPointIndex)
                    if (selectedPoint != null && selectedValue != null) {
                        val xPx = selectedPoint.x
                        val yPx = selectedPoint.y
                        val xOffset = with(density) { (xPx - 36.dp.toPx()).toInt() }
                        val yOffset = with(density) { (yPx - 34.dp.toPx()).toInt() }

                        Text(
                            text = formatChartValue(
                                value = selectedValue,
                                unitLabel = chart.unitLabel,
                                metricKind = metricKind
                            ),
                            color = ProgressPrimaryText,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = xOffset.coerceAtLeast(0),
                                        y = yOffset.coerceAtLeast(0)
                                    )
                                }
                                .background(
                                    color = ProgressSelectionBg,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .width(34.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    scale.labelsDescending.forEach { label ->
                        Text(
                            text = formatAxisValue(label, metricKind),
                            color = ProgressSecondaryText,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            modifier = Modifier.widthIn(min = 24.dp)
                        )
                    }
                }
            }
        }

        // X-axis: dates for the trend windows, set numbers for the per-set Session view. Insets
        // mirror the chart's padding and y-axis column so labels sit under the plotted points.
        if (xAxisLabels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    horizontalArrangement = if (xAxisLabels.size == 1) {
                        Arrangement.Center
                    } else {
                        Arrangement.SpaceBetween
                    }
                ) {
                    xAxisLabels.forEach { label ->
                        Text(
                            text = label,
                            color = ProgressSecondaryText,
                            fontSize = 11.sp,
                            lineHeight = 11.sp
                        )
                    }
                }
                Box(modifier = Modifier.width(40.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when {
                    !isTimeTrend -> chart.unitLabel
                    rangeMode == ChartRangeMode.SESSION -> "${chart.unitLabel} • this session"
                    else -> "${chart.unitLabel} • last ${rangeMode.label}"
                },
                color = ProgressSecondaryText,
                fontSize = 12.sp,
                lineHeight = 12.sp
            )
            if (!targetSeriesHasValues) {
                Text(
                    text = "Target values pending",
                    color = ProgressSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
private fun RangeModeControl(
    availableModes: List<ChartRangeMode>,
    selectedMode: ChartRangeMode,
    onModeSelected: (ChartRangeMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Zoom:",
            color = ProgressSecondaryText,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        availableModes.forEach { mode ->
            val isSelected = mode == selectedMode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) ProgressAccent else ProgressRangeChipBg)
                    .clickable { onModeSelected(mode) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = mode.label,
                    color = if (isSelected) Color(0xFFFDEDE7) else ProgressSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

internal data class AxisScale(
    val minValue: Float,
    val maxValue: Float,
    val labelsDescending: List<Float>
)

internal fun buildDynamicAxisScale(
    values: List<Float>,
    metricKind: ChartMetricKind,
    labelCount: Int
): AxisScale {
    val safeLabelCount = labelCount.coerceAtLeast(2)
    val rawMin = values.minOrNull()?.coerceAtLeast(0f) ?: 0f
    val rawMax = values.maxOrNull()?.coerceAtLeast(0f) ?: 0f

    if (values.isEmpty()) {
        val defaultStep = when (metricKind) {
            ChartMetricKind.WEIGHT -> 5f
            ChartMetricKind.REPS -> 2f
            ChartMetricKind.GENERIC -> 1f
        }
        val maxValue = defaultStep * (safeLabelCount - 1)
        val labels = (safeLabelCount - 1 downTo 0).map { it * defaultStep }
        return AxisScale(
            minValue = 0f,
            maxValue = maxValue,
            labelsDescending = labels
        )
    }

    // Auto-fit: the y-axis simply frames the visible values with a little headroom.
    val spread = (rawMax - rawMin).coerceAtLeast(0f)
    val step = chooseBaseStep(metricKind = metricKind, spread = spread)
    val paddingSteps = 1

    var minValue = (floor((rawMin - step * paddingSteps) / step) * step).coerceAtLeast(0f)
    var maxValue = ceil((rawMax + step * paddingSteps) / step) * step

    val minRange = step * (safeLabelCount - 1)
    if ((maxValue - minValue) < minRange) {
        maxValue = minValue + minRange
    }

    val requiredSpan = (maxValue - minValue).coerceAtLeast(step)
    val intervalsPerLabel = ceil(requiredSpan / (step * (safeLabelCount - 1))).toInt().coerceAtLeast(1)
    val labelStep = step * intervalsPerLabel

    val axisTop = ceil(maxValue / labelStep) * labelStep
    val axisBottom = (axisTop - labelStep * (safeLabelCount - 1)).coerceAtLeast(0f)
    val labels = (0 until safeLabelCount).map { index ->
        axisTop - (labelStep * index)
    }
    return AxisScale(
        minValue = axisBottom,
        maxValue = axisTop,
        labelsDescending = labels
    )
}

internal fun resolveMetricKind(unitLabel: String): ChartMetricKind {
    val normalized = unitLabel.trim().lowercase(Locale.ENGLISH)
    return when {
        normalized.contains("kg") -> ChartMetricKind.WEIGHT
        normalized.contains("rep") -> ChartMetricKind.REPS
        else -> ChartMetricKind.GENERIC
    }
}

private fun chooseBaseStep(
    metricKind: ChartMetricKind,
    spread: Float
): Float {
    return when (metricKind) {
        ChartMetricKind.WEIGHT -> when {
            spread <= 12.5f -> 2.5f
            spread <= 35f -> 5f
            else -> 10f
        }

        ChartMetricKind.REPS -> when {
            spread <= 4f -> 1f
            spread <= 12f -> 2f
            else -> 5f
        }

        ChartMetricKind.GENERIC -> {
            val roughStep = spread / 3f
            niceNumber(roughStep.coerceAtLeast(1f))
        }
    }
}

internal data class VisibleSeries(
    val actualValues: List<Float>,
    val targetValues: List<Float?>,
    // One label per visible point, used to render the date (or set #) x-axis under the chart.
    val pointLabels: List<String>
)

private val XAxisDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

/**
 * Resolves the points the chart should plot for the selected zoom level.
 *
 * - [ChartRangeMode.SESSION] plots every set logged during the reviewed session, labelled "Set 1",
 *   "Set 2"… on the x-axis.
 * - The day-window modes keep only cross-session trend points whose timestamp falls within
 *   (now - mode.days .. now), labelled by completion date.
 *
 * Charts without timestamps (Home weight dialog, Progress) are not time trends and are returned
 * unchanged so they keep showing their full series with no x-axis labels.
 */
internal fun buildVisibleSeries(
    actualValues: List<Float>,
    targetValues: List<Float?>,
    sessionSetValues: List<Float>,
    timestamps: List<Long>,
    isTimeTrend: Boolean,
    mode: ChartRangeMode
): VisibleSeries {
    if (!isTimeTrend) {
        return VisibleSeries(
            actualValues = actualValues,
            targetValues = targetValues,
            pointLabels = emptyList()
        )
    }

    if (mode == ChartRangeMode.SESSION) {
        return VisibleSeries(
            actualValues = sessionSetValues,
            targetValues = List(sessionSetValues.size) { null },
            pointLabels = sessionSetValues.indices.map { "Set ${it + 1}" }
        )
    }

    val zone = ZoneId.systemDefault()
    val cutoffDate = LocalDate.now().minusDays(mode.days.toLong())
    val keptIndices = timestamps.indices.filter { index ->
        val date = Instant.ofEpochMilli(timestamps[index]).atZone(zone).toLocalDate()
        !date.isBefore(cutoffDate)
    }

    val filteredActual = keptIndices.mapNotNull { actualValues.getOrNull(it) }
    val filteredTarget = keptIndices.map { targetValues.getOrNull(it) }
    val filteredLabels = keptIndices.map { index ->
        Instant.ofEpochMilli(timestamps[index]).atZone(zone).toLocalDate().format(XAxisDateFormatter)
    }
    return VisibleSeries(
        actualValues = filteredActual,
        targetValues = filteredTarget,
        pointLabels = filteredLabels
    )
}

/**
 * Thins a per-point label list down to at most three evenly-spaced labels (first / middle / last)
 * so the x-axis stays readable when many points are visible.
 */
internal fun selectXAxisLabels(pointLabels: List<String>): List<String> {
    return when {
        pointLabels.size <= 3 -> pointLabels
        else -> listOf(
            pointLabels.first(),
            pointLabels[pointLabels.size / 2],
            pointLabels.last()
        )
    }
}

private fun niceNumber(value: Float): Float {
    if (value <= 0f) return 1f
    val exponent = floor(log10(value.toDouble())).toInt()
    val fraction = value / 10f.pow(exponent)
    val niceFraction = when {
        fraction <= 1f -> 1f
        fraction <= 2f -> 2f
        fraction <= 5f -> 5f
        else -> 10f
    }
    return niceFraction * 10f.pow(exponent)
}

private fun computeGraphPoints(
    values: List<Float>,
    canvasWidth: Float,
    canvasHeight: Float,
    scaleMin: Float,
    scaleMax: Float,
    leftPaddingPx: Float,
    rightPaddingPx: Float,
    topPaddingPx: Float,
    bottomPaddingPx: Float
): List<Offset> {
    if (values.isEmpty()) return emptyList()
    val pointCount = max(1, values.size)

    return values.mapIndexed { index, value ->
        computeSingleGraphPoint(
            index = index,
            value = value,
            totalPoints = pointCount,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            scaleMin = scaleMin,
            scaleMax = scaleMax,
            leftPaddingPx = leftPaddingPx,
            rightPaddingPx = rightPaddingPx,
            topPaddingPx = topPaddingPx,
            bottomPaddingPx = bottomPaddingPx
        )
    }
}

private fun computeSingleGraphPoint(
    index: Int,
    value: Float,
    totalPoints: Int,
    canvasWidth: Float,
    canvasHeight: Float,
    scaleMin: Float,
    scaleMax: Float,
    leftPaddingPx: Float,
    rightPaddingPx: Float,
    topPaddingPx: Float,
    bottomPaddingPx: Float
): Offset {
    val left = leftPaddingPx
    val right = canvasWidth - rightPaddingPx
    val top = topPaddingPx
    val bottom = canvasHeight - bottomPaddingPx
    val graphWidth = (right - left).coerceAtLeast(1f)
    val graphHeight = (bottom - top).coerceAtLeast(1f)

    val x = if (totalPoints <= 1) {
        left + graphWidth / 2f
    } else {
        left + (graphWidth * index / (totalPoints - 1).toFloat())
    }
    val minValue = scaleMin.coerceAtLeast(0f)
    val maxValue = max(scaleMax, minValue + 1f)
    val yRatio = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
    val y = bottom - yRatio * graphHeight
    return Offset(x, y)
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = abs(a.x - b.x)
    val dy = abs(a.y - b.y)
    return sqrt(dx * dx + dy * dy)
}

private fun formatChartValue(
    value: Float,
    unitLabel: String,
    metricKind: ChartMetricKind
): String {
    val formattedValue = when (metricKind) {
        ChartMetricKind.WEIGHT -> formatWeightValue(value)
        ChartMetricKind.REPS -> value.roundToInt().coerceAtLeast(0).toString()
        ChartMetricKind.GENERIC -> formatGenericValue(value)
    }
    return "$formattedValue $unitLabel"
}

private fun formatAxisValue(
    value: Float,
    metricKind: ChartMetricKind
): String {
    return when (metricKind) {
        ChartMetricKind.WEIGHT -> formatWeightValue(value)
        ChartMetricKind.REPS -> value.roundToInt().coerceAtLeast(0).toString()
        ChartMetricKind.GENERIC -> formatGenericValue(value)
    }
}

private fun formatWeightValue(value: Float): String {
    val roundedInt = value.roundToInt().toFloat()
    if (abs(value - roundedInt) < 0.001f) {
        return roundedInt.toInt().toString()
    }
    return String.format(Locale.ENGLISH, "%.2f", value).trimEnd('0').trimEnd('.')
}

private fun formatGenericValue(value: Float): String {
    return if (abs(value - value.roundToInt().toFloat()) < 0.001f) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.ENGLISH, "%.1f", value)
    }
}

@Composable
private fun ChartLegend(
    actualLabel: String,
    targetLabel: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(
            color = ProgressAccent,
            text = actualLabel,
            dashed = false
        )
        LegendItem(
            color = ProgressTargetPlaceholder,
            text = targetLabel,
            dashed = true
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    text: String,
    dashed: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(modifier = Modifier.size(width = 18.dp, height = 10.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 2.dp.toPx(),
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(7f, 4f), 0f) else null,
                cap = StrokeCap.Round
            )
        }
        Text(
            text = text,
            color = ProgressSecondaryText,
            fontSize = 12.sp,
            lineHeight = 12.sp
        )
    }
}
