package com.stepandemianenko.sdtfitness.ui.components.loading

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun DelayedLoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    showDelayMillis: Long = 180,
    minVisibleMillis: Long = 700,
    exitFadeMillis: Int = 300,
    blurRadius: Dp = 12.dp,
    scrimAlpha: Float = 0.34f,
    content: @Composable () -> Unit,
    loadingContent: @Composable (
        isLoading: Boolean,
        onFinished: () -> Unit,
        modifier: Modifier
    ) -> Unit
) {
    var isOverlayVisible by remember { mutableStateOf(false) }
    var isLogoLoading by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    var visibleSinceMillis by remember { mutableStateOf(0L) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            isExiting = false
            isLogoLoading = true
            delay(showDelayMillis)
            isOverlayVisible = true
            visibleSinceMillis = SystemClock.uptimeMillis()
        } else if (isOverlayVisible) {
            val elapsedVisibleMillis = SystemClock.uptimeMillis() - visibleSinceMillis
            delay(max(0L, minVisibleMillis - elapsedVisibleMillis))
            isLogoLoading = false
        } else {
            isLogoLoading = false
            isExiting = false
        }
    }

    LaunchedEffect(isExiting) {
        if (isExiting) {
            delay(exitFadeMillis.toLong())
            isOverlayVisible = false
            isExiting = false
        }
    }

    val overlayProgress by animateFloatAsState(
        targetValue = if (isOverlayVisible && !isExiting) 1f else 0f,
        animationSpec = tween(durationMillis = exitFadeMillis),
        label = "delayed_loading_overlay_alpha"
    )
    val blurAmount = (blurRadius.value * overlayProgress).dp

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurAmount)
        ) {
            content()
        }

        if (isOverlayVisible || overlayProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha * overlayProgress)),
                contentAlignment = Alignment.Center
            ) {
                val logoScale = 0.96f + (0.04f * overlayProgress)
                loadingContent(
                    isLogoLoading,
                    {
                        isExiting = true
                    },
                    Modifier.graphicsLayer {
                        alpha = overlayProgress
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                )
            }
        }
    }
}
