package com.stepandemianenko.sdtfitness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepandemianenko.sdtfitness.home.DailyGoalSummaryState
import com.stepandemianenko.sdtfitness.home.DailyQuestState
import com.stepandemianenko.sdtfitness.home.HomeUiEvent
import com.stepandemianenko.sdtfitness.home.HomeUiState
import com.stepandemianenko.sdtfitness.home.HomeViewModel
import com.stepandemianenko.sdtfitness.home.RecoveryOption
import com.stepandemianenko.sdtfitness.quicklog.QuickLogRoute
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
//import com.example.fitnessapp.ExerciseActivity
//import com.example.fitnessapp.ProfileActivity

//import com.example.fitnessapp.SettingsActivity

class Home : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HomeRoute(
                    onStartWorkoutClick = {
                        openStartWorkoutWithoutAnimation()
                    },
                    onWorkoutClick = {
                        openStartWorkoutWithoutAnimation()
                    },
                    onProgressClick = {
                        openProgressWithoutAnimation()
                    }
                )
            }
        }
    }

    private fun openStartWorkoutWithoutAnimation() {
        startActivity(Intent(this, StartWorkout::class.java))
        overridePendingTransition(0, 0)
    }

    private fun openProgressWithoutAnimation() {
        startActivity(Intent(this, Progress::class.java))
        overridePendingTransition(0, 0)
    }
}

private val AppBackground = Color(0xFFEBC0B0)
private val CardBackground = Color(0xFFF4E3D7)
private val ActionColor = Color(0xFFF08A67)
private val PrimaryText = Color(0xFF4F2912)
private val SecondaryText = Color(0xFF6B4637)
private val SoftGreen = Color(0xFF69C47A)
private val SoftOrange = Color(0xFFF05C2D)
private val DotGreen = Color(0xFF8ACA8A)
private val ProgressTrack = Color(0xFFE6B8A5)
private val BottomBarBg = Color(0xFFF5E5DA)
private val InactiveIcon = Color(0xFFC48778)
private val StreakHighlight = Color(0x80F88863)
private val HomeContentMaxWidth = 360.dp
private const val HomeReservedBottomFraction = 0.15f
private val HomeHorizontalPadding = 20.dp
private val HomeTopPadding = 30.dp
private val HomeBottomInsetCorner = 16.dp
private val CalendarMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

private enum class HomeScreen {
    Dashboard,
    RestDayDetails,
    QuickLogDetails
}

@Composable
fun HomeRoute(
    onStartWorkoutClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeOneScreen(
        uiState = uiState,
        onOpenDailyQuestEditor = { viewModel.onEvent(HomeUiEvent.OpenDailyQuestEditor) },
        onDismissDailyQuestEditor = { viewModel.onEvent(HomeUiEvent.DismissDailyQuestEditor) },
        onDailyQuestTargetInputChanged = { viewModel.onEvent(HomeUiEvent.DailyQuestTargetInputChanged(it)) },
        onDailyQuestCurrentInputChanged = { viewModel.onEvent(HomeUiEvent.DailyQuestCurrentInputChanged(it)) },
        onSaveDailyQuestEditor = { viewModel.onEvent(HomeUiEvent.SaveDailyQuestEditor) },
        onSaveRecoveryOption = { viewModel.onEvent(HomeUiEvent.SaveRecoveryOption(it)) },
        onPreviousRoutineMonth = { viewModel.onEvent(HomeUiEvent.PreviousRoutineMonth) },
        onNextRoutineMonth = { viewModel.onEvent(HomeUiEvent.NextRoutineMonth) },
        onStartWorkoutClick = onStartWorkoutClick,
        onWorkoutClick = onWorkoutClick,
        onProgressClick = onProgressClick,
        onProfileClick = onProfileClick,
        onSyncHealthConnectClick = viewModel::syncHealthConnectSteps
    )
}

@Composable
fun HomeOneScreen(
    uiState: HomeUiState = HomeUiState(),
    onOpenDailyQuestEditor: () -> Unit = {},
    onDismissDailyQuestEditor: () -> Unit = {},
    onDailyQuestTargetInputChanged: (String) -> Unit = {},
    onDailyQuestCurrentInputChanged: (String) -> Unit = {},
    onSaveDailyQuestEditor: () -> Unit = {},
    onSaveRecoveryOption: (RecoveryOption) -> Unit = {},
    onPreviousRoutineMonth: () -> Unit = {},
    onNextRoutineMonth: () -> Unit = {},
    onStartWorkoutClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSyncHealthConnectClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Keep scroll content clear of the fixed bottom bar + system nav area.
            val reservedBottomHeight = maxHeight * HomeReservedBottomFraction
            var activeScreen by rememberSaveable { mutableStateOf(HomeScreen.Dashboard) }

            BackHandler(enabled = activeScreen != HomeScreen.Dashboard) {
                activeScreen = HomeScreen.Dashboard
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = HomeContentMaxWidth)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = HomeHorizontalPadding,
                                end = HomeHorizontalPadding,
                                top = HomeTopPadding,
                                bottom = reservedBottomHeight
                            ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (activeScreen) {
                            HomeScreen.Dashboard -> DashboardContent(
                                uiState = uiState,
                                onStartWorkoutClick = onStartWorkoutClick,
                                onWorkoutClick = onWorkoutClick,
                                onOpenQuickLogDetails = { activeScreen = HomeScreen.QuickLogDetails },
                                onOpenRestDayDetails = { activeScreen = HomeScreen.RestDayDetails },
                                onOpenDailyQuestEditor = onOpenDailyQuestEditor,
                                onPreviousRoutineMonth = onPreviousRoutineMonth,
                                onNextRoutineMonth = onNextRoutineMonth,
                                healthConnectLastUpdatedMillis = uiState.dashboard.dailyQuest.lastUpdatedMillis,
                                onSyncHealthConnectClick = onSyncHealthConnectClick
                            )

                            HomeScreen.RestDayDetails -> RestDayDetailsContent(
                                onSaveRestDay = {
                                    onSaveRecoveryOption(RecoveryOption.REST_DAY)
                                    activeScreen = HomeScreen.Dashboard
                                },
                                onBackClick = { activeScreen = HomeScreen.Dashboard }
                            )

                            HomeScreen.QuickLogDetails -> QuickLogRoute(
                                onBackClick = { activeScreen = HomeScreen.Dashboard },
                                onQuickLogSaved = { message ->
                                    activeScreen = HomeScreen.Dashboard
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(message = message)
                                    }
                                },
                                cardColor = CardBackground,
                                accentColor = ActionColor,
                                primaryTextColor = PrimaryText,
                                secondaryTextColor = SecondaryText
                            )
                        }
                    }
                }
                BottomNavigationBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onWorkoutClick = onWorkoutClick,
                    onProgressClick = onProgressClick,
                    onProfileClick = onProfileClick
                )

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .widthIn(max = HomeContentMaxWidth)
                        .padding(
                            start = HomeHorizontalPadding,
                            end = HomeHorizontalPadding,
                            bottom = reservedBottomHeight + 8.dp
                        )
                )
            }

            if (uiState.isDailyQuestEditorOpen) {
                DailyQuestEditorDialog(
                    targetStepsValue = uiState.draftTargetSteps,
                    currentStepsValue = uiState.draftCurrentSteps,
                    onTargetStepsChanged = onDailyQuestTargetInputChanged,
                    onCurrentStepsChanged = onDailyQuestCurrentInputChanged,
                    onDismiss = onDismissDailyQuestEditor,
                    onSave = onSaveDailyQuestEditor
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: HomeUiState,
    onStartWorkoutClick: () -> Unit,
    onWorkoutClick: () -> Unit,
    onOpenQuickLogDetails: () -> Unit,
    onOpenRestDayDetails: () -> Unit,
    onOpenDailyQuestEditor: () -> Unit,
    onPreviousRoutineMonth: () -> Unit,
    onNextRoutineMonth: () -> Unit,
    healthConnectLastUpdatedMillis: Long?,
    onSyncHealthConnectClick: () -> Unit
) {
    HeaderSection(
        healthConnectLastUpdatedMillis = healthConnectLastUpdatedMillis,
        onSyncHealthConnectClick = onSyncHealthConnectClick
    )
    DailyProgressCard(summary = uiState.dashboard.dailyGoalSummary)
    StartWorkoutRow(onStartWorkoutClick)

    SectionHeader(title = "Today's Plan", action = "Edit")
    PlanRow(
        title = "Gym Session",
        subtitle = "Upper Body  •  45 min",
        icon = {
            CircleIconContainer(iconRes = R.drawable.home_workout_thumb)
        },
        onClick = onStartWorkoutClick
    )
    PlanRow(
        title = "Quick Log",
        subtitle = "Log a small activity or update your day",
        icon = {
            CircleIconContainer(iconRes = R.drawable.quick_log)
        },
        onClick = onOpenQuickLogDetails
    )
    PlanRow(
        title = "Rest Day",
        subtitle = "Mark today as a recovery day",
        icon = {
            CircleIconContainer(iconRes = R.drawable.recovery)
        },
        onClick = onOpenRestDayDetails
    )
    SectionHeader(title = "Daily Quest", action = "Optional")
    DailyQuestCard(
        questState = uiState.dashboard.dailyQuest,
        onEditClick = onOpenDailyQuestEditor
    )
    AddTile(onClick = onWorkoutClick)
    SectionHeader(title = "Routine")
    RoutineCard(
        visibleMonth = uiState.visibleRoutineMonth,
        streakDates = uiState.dashboard.routineStreakDates,
        onPreviousMonth = onPreviousRoutineMonth,
        onNextMonth = onNextRoutineMonth,
        onClick = {}
    )
}

@Composable
private fun RestDayDetailsContent(
    onSaveRestDay: () -> Unit,
    onBackClick: () -> Unit
) {
    val optionSpacing = dimensionResource(id = R.dimen.rest_day_option_spacing)

    Row(
        modifier = Modifier
            .clickable(onClick = onBackClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            color = PrimaryText,
            fontSize = 22.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(id = R.string.rest_day_back_to_home),
            color = SecondaryText,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(optionSpacing)) {
        RestDayHeroCard()
        HomeCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.rest_day_recovery_title),
                        color = PrimaryText,
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(id = R.string.rest_day_recovery_subtitle),
                        color = SecondaryText,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }

                RestDayPrimaryAction(
                    label = stringResource(id = R.string.rest_day_save_rest_day),
                    onClick = onSaveRestDay
                )

                Text(
                    text = stringResource(id = R.string.rest_day_helper_text),
                    color = SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RestDayHeroCard() {
    HomeCard(horizontalPadding = 0.dp, verticalPadding = 0.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.rest_day_hero_height))
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.sleeping_pillow),
                contentDescription = stringResource(id = R.string.rest_day_hero_content_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}

@Composable
private fun RecoveryOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    supportingChips: List<String> = emptyList()
) {
    val optionRadius = dimensionResource(id = R.dimen.rest_day_option_corner_radius)
    val optionVerticalPadding = dimensionResource(id = R.dimen.rest_day_option_vertical_padding)
    val optionHorizontalPadding = dimensionResource(id = R.dimen.rest_day_option_horizontal_padding)
    val borderColor = if (isSelected) ActionColor else SecondaryText.copy(alpha = 0.20f)
    val containerColor = if (isSelected) ActionColor.copy(alpha = 0.13f) else CardBackground

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(optionRadius))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(optionRadius)
            )
            .background(containerColor)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                selected = isSelected
                role = Role.RadioButton
            }
            .padding(horizontal = optionHorizontalPadding, vertical = optionVerticalPadding)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) ActionColor else SecondaryText.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .background(if (isSelected) ActionColor.copy(alpha = 0.14f) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ActionColor)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        color = PrimaryText,
                        fontSize = 17.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        color = SecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            if (supportingChips.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    supportingChips.forEach { label ->
                        RestDaySupportChip(label = label)
                    }
                }
            }
        }
    }
}

@Composable
private fun RestDaySupportChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.rest_day_chip_corner_radius)))
            .background(CardBackground.copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = SecondaryText.copy(alpha = 0.25f),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.rest_day_chip_corner_radius))
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.rest_day_chip_horizontal_padding),
                vertical = dimensionResource(id = R.dimen.rest_day_chip_vertical_padding)
            )
    ) {
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RestDayPrimaryAction(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.rest_day_primary_button_height)),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = ActionColor,
            contentColor = Color(0xFFFCE8DA)
        )
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HeaderSection(
    healthConnectLastUpdatedMillis: Long?,
    onSyncHealthConnectClick: () -> Unit
) {
    val syncStatusText = healthConnectLastUpdatedMillis?.let {
        "Synced from Health Connect - ${formatSyncAgeText(it)}"
    } ?: "Not synced with Health Connect"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Good afternoon",
            color = PrimaryText,
            fontSize = 40.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Let's keep the momentum going",
                    color = SecondaryText,
                    fontSize = 16.sp,
                    lineHeight = 18.sp
                )
                Row(verticalAlignment = Alignment.Top) {
                    SyncedStatusIcon(modifier = Modifier.padding(top = 1.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = syncStatusText,
                        color = SecondaryText,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            SyncActionButton(
                onClick = onSyncHealthConnectClick,
                modifier = Modifier.height(34.dp)
            )
        }
    }
}

@Composable
private fun SyncActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.synched_title),
        contentDescription = "Refresh and import Health Connect data",
        modifier = modifier
            .clickable(onClick = onClick),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun SyncedStatusIcon(
    modifier: Modifier = Modifier
) {

    Image(
        painter = painterResource(id = R.drawable.synched),
        contentDescription = "Sync",
        modifier = modifier.size(15.dp)
    )

}

private fun formatSyncAgeText(lastUpdatedMillis: Long): String {
    val elapsedMs = (System.currentTimeMillis() - lastUpdatedMillis).coerceAtLeast(0L)
    val elapsedMinutes = elapsedMs / (60 * 1000)

    return when {
        elapsedMinutes < 1 -> "Just now"
        elapsedMinutes < 60 -> "${elapsedMinutes} min ago"
        elapsedMinutes < 120 -> "1 hour ago"
        elapsedMinutes < 60 * 24 -> "${elapsedMinutes / 60} hours ago"
        else -> "${(elapsedMinutes / 60) / 24} days ago"
    }
}

@Composable
private fun DailyProgressCard(
    summary: DailyGoalSummaryState
) {
    HomeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoalProgressRing(
                progress = summary.overallProgress,
                modifier = Modifier.size(92.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${formatCount(summary.stepsCurrent)} / ${formatCount(summary.stepsTarget)} steps",
                    color = SecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${summary.workoutsCompletedCapped} / ${summary.workoutsTarget} workouts",
                    color = SecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(DotGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${summary.activeMinutesCurrent} / ${summary.activeMinutesTarget} active min",
                        color = SecondaryText,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalProgressRing(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            val bounded = progress.coerceIn(0f, 1f)
            val greenSweep = 360f * bounded
            val orangeSweep = 360f - greenSweep
            drawArc(
                color = SoftGreen,
                startAngle = -90f,
                sweepAngle = greenSweep,
                useCenter = false,
                style = stroke,
                size = Size(size.width, size.height)
            )
            drawArc(
                color = SoftOrange,
                startAngle = -90f + greenSweep + 4f,
                sweepAngle = (orangeSweep - 4f).coerceAtLeast(0f),
                useCenter = false,
                style = stroke,
                size = Size(size.width, size.height)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress * 100).toInt()}%",
                color = PrimaryText,
                fontSize = 20.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Daily Goal",
                color = SecondaryText,
                fontSize = 13.sp,
                lineHeight = 11.sp
            )
        }
    }
}

@Composable
private fun StartWorkoutRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ActionColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "▶",
            color = Color(0xFFFCE8DA),
            fontSize = 17.sp,
            lineHeight = 17.sp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "Start Workout",
            color = Color(0xFFFCE8DA),
            fontSize = 17.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.home_icon_creamwhite),
            contentDescription = "Go",
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            color = PrimaryText,
            fontSize = 18.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (action != null) {
            Text(
                text = action,
                color = SecondaryText,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PlanRow(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    HomeCard(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalPadding = 12.dp,
        verticalPadding = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = PrimaryText,
                    fontSize = 18.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = SecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                )
            }
            Image(
                painter = painterResource(id = R.drawable.home_icon_chevron),
                contentDescription = "Open",
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun CircleIconContainer(iconRes: Int) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color(0xBBF88863)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun DailyQuestCard(
    questState: DailyQuestState,
    onEditClick: () -> Unit
) {
    HomeCard(
        modifier = Modifier.clickable(onClick = onEditClick),
        horizontalPadding = 10.dp,
        verticalPadding = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconContainer(iconRes = R.drawable.home_shoes)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Walk ${formatCount(questState.targetSteps)} steps",
                    color = PrimaryText,
                    fontSize = 18.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${formatCount(questState.currentSteps)} / ${formatCount(questState.targetSteps)}",
                    color = SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ProgressTrack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(questState.progress)
                            .height(6.dp)
                            .background(SoftGreen)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF2D111))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (questState.isManual) "Manual" else "Imported",
                        color = PrimaryText,
                        fontSize = 11.sp,
                        lineHeight = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.home_icon_chevron),
                    contentDescription = "Edit daily quest",
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

private fun formatCount(value: Int): String {
    return "%,d".format(value.coerceAtLeast(0))
}

@Composable
private fun DailyQuestEditorDialog(
    targetStepsValue: String,
    currentStepsValue: String,
    onTargetStepsChanged: (String) -> Unit,
    onCurrentStepsChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Daily Quest Steps",
                color = PrimaryText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = targetStepsValue,
                    onValueChange = onTargetStepsChanged,
                    label = { Text("Target steps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = currentStepsValue,
                    onValueChange = onCurrentStepsChanged,
                    label = { Text("Current steps") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    HomeCard(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalPadding = 0.dp,
        verticalPadding = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = PrimaryText,
                fontSize = 28.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
private fun RoutineCard(
    visibleMonth: YearMonth,
    streakDates: Set<LocalDate>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onClick: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val calendarDays = remember(visibleMonth, streakDates, today) {
        buildCalendarDays(visibleMonth, streakDates, today)
    }
    val streakCount = calculateCurrentStreakForRoutine(streakDates, today)

    HomeCard(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalPadding = 10.dp,
        verticalPadding = 10.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = visibleMonth.format(CalendarMonthFormatter),
                    color = PrimaryText,
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.width(14.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.fire_icon),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$streakCount-day consistency",
                        color = SecondaryText,
                        fontSize = 15.sp,
                        lineHeight = 15.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.home_calendar_prev),
                        contentDescription = "Previous month",
                        modifier = Modifier
                            .size(width = 22.dp, height = 19.dp)
                            .clickable(onClick = onPreviousMonth)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.home_calendar_next),
                        contentDescription = "Next month",
                        modifier = Modifier
                            .size(width = 22.dp, height = 19.dp)
                            .clickable(onClick = onNextMonth)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SecondaryText.copy(alpha = 0.35f))
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { dayName ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayName,
                            color = SecondaryText,
                            fontSize = 12.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }

            calendarDays.chunked(7).forEach { week ->
                CalendarWeekRow(days = week)
            }
        }
    }
}

private data class CalendarDayUi(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isInStreak: Boolean,
    val isToday: Boolean
)

private fun buildCalendarDays(
    visibleMonth: YearMonth,
    streakDates: Set<LocalDate>,
    today: LocalDate
): List<CalendarDayUi> {
    val firstOfMonth = visibleMonth.atDay(1)
    val mondayOffset = (firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val gridStart = firstOfMonth.minusDays(mondayOffset.toLong())

    return (0 until 42).map { index ->
        val date = gridStart.plusDays(index.toLong())
        CalendarDayUi(
            date = date,
            isCurrentMonth = date.month == visibleMonth.month,
            isInStreak = streakDates.contains(date),
            isToday = date == today
        )
    }
}

private fun calculateCurrentStreakForRoutine(
    streakDates: Set<LocalDate>,
    today: LocalDate
): Int {
    if (streakDates.isEmpty()) return 0
    var streak = 0
    var cursor = today
    while (streakDates.contains(cursor)) {
        streak += 1
        cursor = cursor.minusDays(1)
    }
    return streak
}

@Composable
private fun CalendarWeekRow(days: List<CalendarDayUi>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEachIndexed { index, day ->
            val previousInStreak = index > 0 && days[index - 1].isInStreak
            val nextInStreak = index < days.lastIndex && days[index + 1].isInStreak
            val streakShape = when {
                !day.isInStreak -> RoundedCornerShape(0.dp)
                !previousInStreak && !nextInStreak -> RoundedCornerShape(10.dp)
                !previousInStreak -> RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                !nextInStreak -> RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                else -> RoundedCornerShape(0.dp)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(streakShape)
                    .background(if (day.isInStreak) StreakHighlight else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                val dayTextColor = if (day.isCurrentMonth) SecondaryText else SecondaryText.copy(alpha = 0.65f)
                val todayIndicatorModifier = if (day.isToday) {
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(width = 1.dp, color = ActionColor, shape = CircleShape)
                } else {
                    Modifier
                }

                Box(
                    modifier = todayIndicatorModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        color = if (day.isToday) PrimaryText else dayTextColor,
                        fontSize = 12.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    onWorkoutClick: () -> Unit,
    onProgressClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BottomBarBg)
                .border(width = 1.dp, color = Color(0x80D6AA98))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavItem(label = "Home", icon = R.drawable.home_nav_home_curr, textColor = Color(0xFFBF7E65), onClick = {})
            BottomNavItem(label = "Workout", icon = R.drawable.home_nav_workout, textColor = InactiveIcon, onClick = onWorkoutClick)
            BottomNavItem(label = "Progress", icon = R.drawable.home_nav_progress, textColor = InactiveIcon, onClick = onProgressClick)
            BottomNavItem(label = "Profile", icon = R.drawable.home_nav_profile, textColor = InactiveIcon, onClick = onProfileClick)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(
                    color = BottomBarBg,
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = HomeBottomInsetCorner,
                        bottomEnd = HomeBottomInsetCorner
                    )
                )
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: Int,
    textColor: Color,
    iconWidth: Dp = 24.dp,
    iconHeight: Dp = 24.dp,
    iconContentScale: ContentScale = ContentScale.Fit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = label,
            modifier = Modifier
                .width(iconWidth)
                .height(iconHeight),
            contentScale = iconContentScale
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            lineHeight = 10.sp
        )
    }
}

@Composable
private fun HomeCard(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        content()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun HomeOneScreenPreview() {
    MaterialTheme {
        HomeOneScreen()
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun RestDayDetailsPreview() {
    MaterialTheme {
        RestDayDetailsContent(
            onSaveRestDay = {},
            onBackClick = {}
        )
    }
}
