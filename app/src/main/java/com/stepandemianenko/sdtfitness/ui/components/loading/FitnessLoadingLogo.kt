package com.stepandemianenko.sdtfitness.ui.components.loading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stepandemianenko.sdtfitness.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun FitnessLoadingLogo(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 164.dp,
    completedHoldMillis: Long = LoadingLogoCompletedHoldMillis.toLong(),
    onFinished: () -> Unit = {}
) {
    val progress = remember { Animatable(0f) }
    val latestOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(isLoading) {
        if (isLoading) {
            while (isActive) {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = LoadingLogoSequenceMillis,
                        easing = LinearEasing
                    )
                )
                delay(completedHoldMillis)
            }
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = LoadingLogoFinishMillis,
                    easing = LinearEasing
                )
            )
            delay(completedHoldMillis)
            latestOnFinished()
        }
    }

    FitnessLoadingLogoProgress(
        progress = progress.value,
        modifier = modifier.size(size)
    )
}

@Composable
fun FitnessLoadingLogoProgress(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.fitness_loading_logo_base_not_ready),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        FitnessLoadingLogoOverlay(
            drawableRes = R.drawable.fitness_loading_logo_ready_l1,
            alpha = loadingLogoSegmentAlpha(clampedProgress, PairOneStart, PairOneEnd)
        )
        FitnessLoadingLogoOverlay(
            drawableRes = R.drawable.fitness_loading_logo_ready_r1,
            alpha = loadingLogoSegmentAlpha(clampedProgress, PairOneStart, PairOneEnd)
        )
        FitnessLoadingLogoOverlay(
            drawableRes = R.drawable.fitness_loading_logo_ready_l2,
            alpha = loadingLogoSegmentAlpha(clampedProgress, PairTwoStart, PairTwoEnd)
        )
        FitnessLoadingLogoOverlay(
            drawableRes = R.drawable.fitness_loading_logo_ready_r2,
            alpha = loadingLogoSegmentAlpha(clampedProgress, PairTwoStart, PairTwoEnd)
        )
        FitnessLoadingLogoOverlay(
            drawableRes = R.drawable.fitness_loading_logo_ready_l3,
            alpha = loadingLogoSegmentAlpha(clampedProgress, PairThreeStart, PairThreeEnd)
        )
        FitnessLoadingLogoOverlay(
            drawableRes = R.drawable.fitness_loading_logo_ready_r3,
            alpha = loadingLogoSegmentAlpha(clampedProgress, PairThreeStart, PairThreeEnd)
        )
        FitnessLoadingLogoOverlay(
            drawableRes = R.drawable.fitness_loading_logo_ready_dumbbell,
            alpha = loadingLogoSegmentAlpha(clampedProgress, DumbbellStart, DumbbellEnd)
        )
    }
}

@Composable
private fun FitnessLoadingLogoOverlay(
    drawableRes: Int,
    alpha: Float
) {
    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha),
        contentScale = ContentScale.Fit
    )
}

private fun loadingLogoSegmentAlpha(
    progress: Float,
    start: Float,
    end: Float
): Float {
    return ((progress - start) / (end - start)).coerceIn(0f, 1f)
}

private const val PairFillMillis = 250
private const val DumbbellFillMillis = 300
private const val LoadingLogoCompletedHoldMillis = 400
private const val LoadingLogoFinishMillis = 300
private const val LoadingLogoSequenceMillis = PairFillMillis * 3 + DumbbellFillMillis

private const val PairOneStart = 0f
private val PairOneEnd = PairFillMillis.toFloat() / LoadingLogoSequenceMillis
private val PairTwoStart = PairOneEnd
private val PairTwoEnd = (PairFillMillis * 2).toFloat() / LoadingLogoSequenceMillis
private val PairThreeStart = PairTwoEnd
private val PairThreeEnd = (PairFillMillis * 3).toFloat() / LoadingLogoSequenceMillis
private val DumbbellStart = PairThreeEnd
private const val DumbbellEnd = 1f

@Preview(showBackground = true, backgroundColor = 0xFFEBC0B0)
@Composable
private fun FitnessLoadingLogoNotReadyPreview() {
    FitnessLoadingLogoProgress(
        progress = 0f,
        modifier = Modifier.size(164.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFEBC0B0)
@Composable
private fun FitnessLoadingLogoPartiallyLoadedPreview() {
    FitnessLoadingLogoProgress(
        progress = PairTwoEnd,
        modifier = Modifier.size(164.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFEBC0B0)
@Composable
private fun FitnessLoadingLogoReadyPreview() {
    FitnessLoadingLogoProgress(
        progress = 1f,
        modifier = Modifier.size(164.dp)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFEBC0B0)
@Composable
private fun FitnessLoadingLogoAnimatedPreview() {
    FitnessLoadingLogo(
        isLoading = true,
        size = 164.dp
    )
}
