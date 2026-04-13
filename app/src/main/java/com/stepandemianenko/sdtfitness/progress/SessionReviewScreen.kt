package com.stepandemianenko.sdtfitness.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val ProgressBackground = Color(0xFFEBC0B0)
private val ProgressCardBackground = Color(0xFFF5E5DA)
private val ProgressTileBackground = Color(0xFFF1D5CB)
private val ProgressPrimaryText = Color(0xFF4F2912)
private val ProgressSecondaryText = Color(0xFF6B4637)
private val ProgressDivider = Color(0x40A67B6C)
private val ProgressAccent = Color(0xFFF08A67)

@Composable
fun SessionReviewRoute(
    sessionId: Long,
    onBackClick: () -> Unit,
    viewModel: SessionReviewViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.load(sessionId)
    }

    SessionReviewScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = { viewModel.load(sessionId) }
    )
}

@Composable
fun SessionReviewScreen(
    uiState: SessionReviewUiState,
    onBackClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ProgressBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReviewBackRow(
                text = "Back to Completed Sessions",
                onClick = onBackClick
            )

            Text(
                text = uiState.title,
                color = ProgressPrimaryText,
                fontSize = 30.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold
            )

            if (uiState.dateLabel.isNotBlank()) {
                Text(
                    text = uiState.dateLabel,
                    color = ProgressSecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                )
            }

            when {
                uiState.isLoading -> {
                    Text(
                        text = "Loading session review...",
                        color = ProgressSecondaryText,
                        fontSize = 14.sp,
                        lineHeight = 16.sp
                    )
                }

                uiState.errorMessage != null -> {
                    ReviewCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = uiState.errorMessage,
                                color = ProgressSecondaryText,
                                fontSize = 14.sp,
                                lineHeight = 16.sp
                            )
                            Button(
                                onClick = onRetryClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ProgressAccent,
                                    contentColor = Color(0xFFFDEDE7)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Retry",
                                    fontSize = 14.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                uiState.exercises.isEmpty() -> {
                    ReviewCard {
                        Text(
                            text = "No exercise data recorded for this session.",
                            color = ProgressSecondaryText,
                            fontSize = 14.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                else -> {
                    uiState.exercises.forEach { exercise ->
                        ExerciseReviewCard(exercise = exercise)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ExerciseReviewCard(
    exercise: SessionExerciseReviewUiModel
) {
    ReviewCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = exercise.exerciseName,
                color = ProgressPrimaryText,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            SetRowsTable(sets = exercise.sets)

            ExerciseSetMetricChart(chart = exercise.weightChart)
            ExerciseSetMetricChart(chart = exercise.repsChart)
        }
    }
}

@Composable
private fun SetRowsTable(
    sets: List<SessionSetRowUiModel>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ProgressTileBackground)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Set",
                color = ProgressSecondaryText,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Weight",
                color = ProgressSecondaryText,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Reps",
                color = ProgressSecondaryText,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (sets.isEmpty()) {
            Text(
                text = "No set logs for this exercise",
                color = ProgressSecondaryText,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        } else {
            sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = set.setLabel,
                        color = ProgressPrimaryText,
                        fontSize = 13.sp,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = set.weightLabel,
                        color = ProgressPrimaryText,
                        fontSize = 13.sp,
                        lineHeight = 14.sp
                    )
                    Text(
                        text = set.repsLabel,
                        color = ProgressPrimaryText,
                        fontSize = 13.sp,
                        lineHeight = 14.sp
                    )
                }
                if (index < sets.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(ProgressDivider)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewBackRow(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            color = ProgressPrimaryText,
            fontSize = 22.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = ProgressSecondaryText,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReviewCard(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ProgressCardBackground)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionReviewScreenPreview() {
    MaterialTheme {
        SessionReviewScreen(
            uiState = SessionReviewUiState(
                isLoading = false,
                title = "Upper Body Session",
                dateLabel = "Sun, Apr 12, 2026",
                exercises = listOf(
                    SessionExerciseReviewUiModel(
                        exerciseName = "Bench Press",
                        sets = listOf(
                            SessionSetRowUiModel("Set 1", "60 kg", "8 reps"),
                            SessionSetRowUiModel("Set 2", "62 kg", "8 reps")
                        ),
                        weightChart = SetMetricChartUiModel(
                            title = "Weight across sets",
                            actualLabel = "Actual weight",
                            actualValues = listOf(60f, 62f),
                            targetValues = listOf(null, null),
                            unitLabel = "kg"
                        ),
                        repsChart = SetMetricChartUiModel(
                            title = "Reps across sets",
                            actualLabel = "Actual reps",
                            actualValues = listOf(8f, 8f),
                            targetValues = listOf(null, null),
                            unitLabel = "reps"
                        )
                    )
                )
            ),
            onBackClick = {},
            onRetryClick = {}
        )
    }
}
