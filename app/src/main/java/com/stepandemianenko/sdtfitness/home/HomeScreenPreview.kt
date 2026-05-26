package com.stepandemianenko.sdtfitness.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.stepandemianenko.sdtfitness.HomeOneScreen
import com.stepandemianenko.sdtfitness.ui.theme.SDTFitnessAppTheme
import java.time.LocalDate
import java.time.YearMonth

@Preview(name = "Home Screen", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun HomeScreenPreview() {
    SDTFitnessAppTheme(dynamicColor = false) {
        HomeOneScreen(uiState = previewHomeUiState())
    }
}

private fun previewHomeUiState(): HomeUiState {
    val today = LocalDate.now()

    return HomeUiState(
        dashboard = HomeDashboardState(
            dailyQuest = DailyQuestState(
                targetSteps = 8_000,
                currentSteps = 4_850,
                isManual = true
            ),
            weightInQuest = WeightInQuestState(
                isAdded = true,
                isCompleted = true,
                completionSource = DailyQuestCompletionSource.HEALTH_CONNECT,
                completedAtMillis = System.currentTimeMillis(),
                weightKg = 82.4
            ),
            dailyGoalSummary = DailyGoalSummaryState(
                stepsCurrent = 4_850,
                stepsTarget = 8_000,
                workoutsCompleted = 1,
                workoutsTarget = 1,
                activeMinutesCurrent = 22,
                activeMinutesTarget = 30
            ),
            routineStreakDates = setOf(
                today.minusDays(4),
                today.minusDays(3),
                today.minusDays(2),
                today.minusDays(1),
                today
            ),
            healthConnectLastSyncedAtMillis = System.currentTimeMillis()
        ),
        visibleRoutineMonth = YearMonth.from(today),
        activeAccountId = "preview-user",
        accounts = listOf(
            DebugAccountUiModel(
                id = "preview-user",
                type = "Preview",
                createdAt = System.currentTimeMillis(),
                isActive = true
            )
        )
    )
}
