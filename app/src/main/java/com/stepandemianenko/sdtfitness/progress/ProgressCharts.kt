package com.stepandemianenko.sdtfitness.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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

private enum class ChartRangeMode(val label: String) {
    CLOSE("Close"),
    AUTO("Auto"),
    FULL("Full")
}

private enum class ChartMetricKind {
    WEIGHT,
    REPS,
    GENERIC
}

data class SetMetricChartUiModel(
    val title: String,
    val actualLabel: String,
    val actualValues: List<Float>,
    val targetValues: List<Float?>,
    val unitLabel: String
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
fun ExerciseSetMetricChart(
    chart: SetMetricChartUiModel,
    modifier: Modifier = Modifier
) {
    val hasAnyActual = chart.actualValues.isNotEmpty()
    val targetSeriesHasValues = chart.targetValues.any { it != null }
    var rangeMode by remember(chart.title, chart.unitLabel) { mutableStateOf(ChartRangeMode.AUTO) }
    val metricKind = remember(chart.unitLabel) {
        resolveMetricKind(chart.unitLabel)
    }
    val scale = remember(chart.actualValues, chart.targetValues, metricKind, rangeMode) {
        buildDynamicAxisScale(
            values = chart.actualValues + chart.targetValues.mapNotNull { it },
            metricKind = metricKind,
            mode = rangeMode,
            labelCount = 4
        )
    }

    var selectedPointIndex by remember { mutableIntStateOf(-1) }
    var graphSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val leftPaddingPx = with(density) { 8.dp.toPx() }
    val rightPaddingPx = with(density) { 8.dp.toPx() }
    val topPaddingPx = with(density) { 8.dp.toPx() }
    val bottomPaddingPx = with(density) { 12.dp.toPx() }

    val actualPoints = remember(
        chart.actualValues,
        graphSize,
        scale,
        leftPaddingPx,
        rightPaddingPx,
        topPaddingPx,
        bottomPaddingPx
    ) {
        if (graphSize.width <= 0 || graphSize.height <= 0 || chart.actualValues.isEmpty()) {
            emptyList()
        } else {
            computeGraphPoints(
                values = chart.actualValues,
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

    LaunchedEffect(chart.actualValues) {
        selectedPointIndex = -1
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

        RangeModeControl(
            selectedMode = rangeMode,
            onModeSelected = { rangeMode = it }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(color = ProgressTileBackground, shape = RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            if (!hasAnyActual) {
                Text(
                    text = "No recorded sets yet",
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
                        .pointerInput(chart.actualValues, actualPoints) {
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
                            values = chart.actualValues,
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
                            chart.targetValues.forEachIndexed { index, target ->
                                if (target == null) {
                                    previousPoint = null
                                } else {
                                    val targetPoint = computeSingleGraphPoint(
                                        index = index,
                                        value = target,
                                        totalPoints = max(chart.actualValues.size, chart.targetValues.size),
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
                    val selectedValue = chart.actualValues.getOrNull(selectedPointIndex)
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${chart.unitLabel} • ${rangeMode.label}",
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
    selectedMode: ChartRangeMode,
    onModeSelected: (ChartRangeMode) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChartRangeMode.values().forEach { mode ->
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

private data class AxisScale(
    val minValue: Float,
    val maxValue: Float,
    val labelsDescending: List<Float>
)

private fun buildDynamicAxisScale(
    values: List<Float>,
    metricKind: ChartMetricKind,
    mode: ChartRangeMode,
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

    val spread = (rawMax - rawMin).coerceAtLeast(0f)
    val baseStep = chooseBaseStep(metricKind = metricKind, spread = spread)
    val step = adjustStepByRangeMode(
        metricKind = metricKind,
        baseStep = baseStep,
        mode = mode
    )
    val paddingSteps = when (mode) {
        ChartRangeMode.CLOSE -> 1
        ChartRangeMode.AUTO -> 2
        ChartRangeMode.FULL -> 4
    }

    var minValue = floor((rawMin - step * paddingSteps) / step) * step
    var maxValue = ceil((rawMax + step * paddingSteps) / step) * step

    if (mode == ChartRangeMode.FULL) {
        minValue = 0f
        maxValue = ceil(max(rawMax + step * paddingSteps, step * (safeLabelCount - 1)) / step) * step
    }

    minValue = minValue.coerceAtLeast(0f)

    val minRange = step * (safeLabelCount - 1)
    if ((maxValue - minValue) < minRange) {
        maxValue = minValue + minRange
    }

    val requiredSpan = (maxValue - minValue).coerceAtLeast(step)
    val intervalsPerLabel = ceil(requiredSpan / (step * (safeLabelCount - 1))).toInt().coerceAtLeast(1)
    val labelStep = step * intervalsPerLabel

    var axisTop = ceil(maxValue / labelStep) * labelStep
    if (mode == ChartRangeMode.FULL) {
        axisTop = max(axisTop, labelStep * (safeLabelCount - 1))
    }

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

private fun resolveMetricKind(unitLabel: String): ChartMetricKind {
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

private fun adjustStepByRangeMode(
    metricKind: ChartMetricKind,
    baseStep: Float,
    mode: ChartRangeMode
): Float {
    return when (metricKind) {
        ChartMetricKind.WEIGHT -> when (mode) {
            ChartRangeMode.CLOSE -> when (baseStep) {
                10f -> 5f
                5f -> 2.5f
                else -> 2.5f
            }
            ChartRangeMode.AUTO -> baseStep
            ChartRangeMode.FULL -> when (baseStep) {
                2.5f -> 5f
                else -> 10f
            }
        }

        ChartMetricKind.REPS -> when (mode) {
            ChartRangeMode.CLOSE -> when (baseStep) {
                5f -> 2f
                else -> 1f
            }
            ChartRangeMode.AUTO -> baseStep
            ChartRangeMode.FULL -> when (baseStep) {
                1f -> 2f
                else -> 5f
            }
        }

        ChartMetricKind.GENERIC -> when (mode) {
            ChartRangeMode.CLOSE -> (baseStep / 2f).coerceAtLeast(1f)
            ChartRangeMode.AUTO -> baseStep
            ChartRangeMode.FULL -> baseStep * 2f
        }
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
    val snapped = snapToStep(value, 2.5f)
    return if (abs(snapped - snapped.roundToInt().toFloat()) < 0.001f) {
        snapped.roundToInt().toString()
    } else {
        String.format(Locale.ENGLISH, "%.1f", snapped)
    }
}

private fun formatGenericValue(value: Float): String {
    return if (abs(value - value.roundToInt().toFloat()) < 0.001f) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.ENGLISH, "%.1f", value)
    }
}

private fun snapToStep(
    value: Float,
    step: Float
): Float {
    if (step <= 0f) return value
    return (value / step).roundToInt() * step
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
