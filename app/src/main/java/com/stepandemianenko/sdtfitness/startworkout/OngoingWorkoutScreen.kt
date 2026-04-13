package com.stepandemianenko.sdtfitness.startworkout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepandemianenko.sdtfitness.R
import kotlinx.coroutines.flow.collect

private val WorkoutBackground = Color(0xFFF8C6B5)
private val WorkoutCard = Color(0xFFF7E6DC)
private val WorkoutPrimary = Color(0xFFF88863)
private val WorkoutText = Color(0xFF582C1F)
private val WorkoutSecondary = Color(0xBF582C1F)
private val WorkoutGreen = Color(0xFF70C97D)
private val WorkoutGreenSoft = Color(0xFFABE5A0)
private val WorkoutLineTrack = Color(0xFFE9B9AA)
private val WorkoutNavBg = Color(0xFFF7E6DC)
private val WorkoutInactiveIcon = Color(0xFFC48778)

@Composable
fun OngoingWorkoutRoute(
    initialSessionId: Long?,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onLogSetClick: (weightKg: Int, reps: Int, rpeIndex: Int) -> Unit,
    onSessionCompleted: (Long) -> Unit = {},
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: OngoingWorkoutViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialSessionId) {
        viewModel.attachSession(initialSessionId)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OngoingWorkoutEffect.NavigateToProgress -> onSessionCompleted(effect.sessionId)
            }
        }
    }

    OngoingWorkoutScreen(
        uiState = uiState.value,
        onBackClick = {
            viewModel.onEvent(OngoingWorkoutUiEvent.BackClick)
            onBackClick()
        },
        onEditClick = {
            viewModel.onEvent(OngoingWorkoutUiEvent.EditClick)
            onEditClick()
        },
        onWeightMinusClick = { viewModel.onEvent(OngoingWorkoutUiEvent.WeightMinusClick) },
        onWeightPlusClick = { viewModel.onEvent(OngoingWorkoutUiEvent.WeightPlusClick) },
        onRepsMinusClick = { viewModel.onEvent(OngoingWorkoutUiEvent.RepsMinusClick) },
        onRepsPlusClick = { viewModel.onEvent(OngoingWorkoutUiEvent.RepsPlusClick) },
        onWeightPresetClick = { viewModel.onEvent(OngoingWorkoutUiEvent.WeightPresetClick(it)) },
        onRepsPresetClick = { viewModel.onEvent(OngoingWorkoutUiEvent.RepsPresetClick(it)) },
        onRpeSelectClick = { viewModel.onEvent(OngoingWorkoutUiEvent.RpeSelectClick(it)) },
        onLogSetClick = {
            if (uiState.value.hasActiveSession) {
                viewModel.onEvent(OngoingWorkoutUiEvent.LogSetClick)
                onLogSetClick(
                    uiState.value.loggedWeightKg,
                    uiState.value.loggedReps,
                    uiState.value.selectedRpeIndex
                )
            }
        },
        onHomeClick = onHomeClick,
        onProgressClick = onProgressClick,
        onProfileClick = onProfileClick
    )
}

@Composable
fun OngoingWorkoutScreen(
    uiState: OngoingWorkoutUiState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onWeightMinusClick: () -> Unit,
    onWeightPlusClick: () -> Unit,
    onRepsMinusClick: () -> Unit,
    onRepsPlusClick: () -> Unit,
    onWeightPresetClick: (Int) -> Unit,
    onRepsPresetClick: (Int) -> Unit,
    onRpeSelectClick: (Int) -> Unit,
    onLogSetClick: () -> Unit,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = modifier,
        containerColor = WorkoutBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            OngoingBottomNavigationBar(
                onHomeClick = onHomeClick,
                onProgressClick = onProgressClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WorkoutTopBar(
                title = uiState.exerciseName,
                subtitle = "Set ${uiState.currentSet} of ${uiState.totalSets}",
                onBackClick = onBackClick,
                onEditClick = onEditClick
            )

            if (uiState.isLoading) {
                StatusMessageCard(
                    text = "Loading active workout..."
                )
            } else if (!uiState.hasActiveSession) {
                StatusMessageCard(
                    text = uiState.completionMessage ?: "No active workout session. Start one from Start Workout."
                )
            } else {
                TargetCard(
                    targetText = "${uiState.targetReps} reps @ ${uiState.targetWeightKg} KG",
                    guidance = uiState.exercisePositionText
                )

                uiState.transitionMessage?.let { message ->
                    StatusMessageCard(text = message)
                }

                LogSetSection(
                    weight = uiState.loggedWeightKg,
                    reps = uiState.loggedReps,
                    weightOptions = uiState.weightPresets,
                    repsOptions = uiState.repsPresets,
                    onWeightMinus = onWeightMinusClick,
                    onWeightPlus = onWeightPlusClick,
                    onRepsMinus = onRepsMinusClick,
                    onRepsPlus = onRepsPlusClick,
                    onWeightOptionClick = onWeightPresetClick,
                    onRepsOptionClick = onRepsPresetClick
                )

                RpeSection(
                    options = uiState.rpeOptions,
                    selectedIndex = uiState.selectedRpeIndex,
                    onOptionClick = onRpeSelectClick
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onLogSetClick),
                    color = WorkoutPrimary,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Log Set",
                        color = Color.White,
                        fontSize = 19.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    )
                }

                PreviousResultsCard(
                    lastSession = uiState.previousResults.lastSession,
                    personalBest = uiState.previousResults.personalBest,
                    dateLabel = uiState.previousResults.dateLabel
                )

                SessionProgressCard(
                    completedSets = uiState.completedSets,
                    totalSets = uiState.totalSessionSets,
                    remainingSets = uiState.remainingSets,
                    estimatedTimeRemaining = uiState.estimatedTimeRemaining
                )

                uiState.completionMessage?.let { message ->
                    StatusMessageCard(text = message)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun WorkoutTopBar(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 354.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBackClick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "‹",
                color = WorkoutText,
                fontSize = 22.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "back",
                color = WorkoutText,
                fontSize = 15.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = WorkoutText,
                fontSize = 24.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = WorkoutText,
                fontSize = 17.sp,
                lineHeight = 20.sp
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onEditClick)
                .padding(horizontal = 2.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.start_workout_streak_icon),
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = "Edit",
                color = WorkoutText,
                fontSize = 14.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun TargetCard(
    targetText: String,
    guidance: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 354.dp),
        shape = RoundedCornerShape(10.dp),
        color = WorkoutCard
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.start_workout_streak_icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your target",
                    color = WorkoutSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = targetText,
                    color = WorkoutText,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(WorkoutGreen),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.check_mark),
                    contentDescription = null,
                    modifier = Modifier.size(11.dp, 9.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = guidance,
                color = WorkoutSecondary,
                fontSize = 15.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun StatusMessageCard(
    text: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 354.dp),
        shape = RoundedCornerShape(10.dp),
        color = WorkoutCard
    ) {
        Text(
            text = text,
            color = WorkoutText,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun LogSetSection(
    weight: Int,
    reps: Int,
    weightOptions: List<Int>,
    repsOptions: List<Int>,
    onWeightMinus: () -> Unit,
    onWeightPlus: () -> Unit,
    onRepsMinus: () -> Unit,
    onRepsPlus: () -> Unit,
    onWeightOptionClick: (Int) -> Unit,
    onRepsOptionClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 354.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Log your set",
            color = WorkoutText,
            fontSize = 18.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ValueStepperCard(
                title = "Weight (KG)",
                value = weight,
                options = weightOptions,
                onMinus = onWeightMinus,
                onPlus = onWeightPlus,
                onOptionClick = onWeightOptionClick,
                modifier = Modifier.weight(1f)
            )
            ValueStepperCard(
                title = "Reps",
                value = reps,
                options = repsOptions,
                onMinus = onRepsMinus,
                onPlus = onRepsPlus,
                onOptionClick = onRepsOptionClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ValueStepperCard(
    title: String,
    value: Int,
    options: List<Int>,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onOptionClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            color = WorkoutText,
            fontSize = 16.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = WorkoutCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleAdjustButton(text = "-", onClick = onMinus)
                Text(
                    text = value.toString(),
                    color = WorkoutText,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                CircleAdjustButton(text = "+", onClick = onPlus)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                OptionChip(
                    value = option.toString(),
                    selected = option == value,
                    onClick = { onOptionClick(option) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CircleAdjustButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(0xFFEFD2C4))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = WorkoutPrimary,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OptionChip(
    value: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) WorkoutPrimary else WorkoutCard)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            color = if (selected) Color.White else WorkoutSecondary,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun RpeSection(
    options: List<OngoingRpeOptionUiModel>,
    selectedIndex: Int,
    onOptionClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 354.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "How did that feel? (RPE)",
            color = WorkoutText,
            fontSize = 18.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Help us give better suggestions",
            color = WorkoutSecondary,
            fontSize = 16.sp,
            lineHeight = 17.sp
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = WorkoutCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEachIndexed { index, option ->
                        RpeOptionItem(
                            option = option,
                            selected = index == selectedIndex,
                            onClick = { onOptionClick(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                RpeLine(selectedIndex = selectedIndex)
            }
        }
    }
}

@Composable
private fun RpeOptionItem(
    option: OngoingRpeOptionUiModel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) WorkoutGreenSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = option.range,
            color = if (selected) Color(0xFF377D42) else WorkoutText,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Image(
            painter = painterResource(
                id = if (selected) option.selectedIconRes else option.iconRes
            ),
            contentDescription = option.label,
            modifier = Modifier
                .size(25.dp)
        )
        Text(
            text = option.label,
            color = if (selected) Color(0xFF377D42) else WorkoutText,
            fontSize = 13.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RpeLine(selectedIndex: Int) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val y = size.height / 2f
        val left = 4f
        val right = size.width - 4f
        val step = (right - left) / 4f
        val selectedX = left + (step * selectedIndex.coerceIn(0, 4))

        drawLine(
            color = WorkoutLineTrack,
            start = Offset(left, y),
            end = Offset(right, y),
            strokeWidth = 6f
        )
        drawLine(
            color = WorkoutGreen,
            start = Offset(left, y),
            end = Offset(selectedX, y),
            strokeWidth = 6f
        )
        for (index in 0..4) {
            val x = left + (step * index)
            drawCircle(
                color = if (index <= selectedIndex) WorkoutGreen else WorkoutLineTrack,
                radius = if (index == selectedIndex) 7f else 5f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun PreviousResultsCard(
    lastSession: String,
    personalBest: String,
    dateLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 354.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Previous Results",
            color = WorkoutText,
            fontSize = 18.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = WorkoutCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                ResultRow(icon = "o", title = "Last Session", value = lastSession)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ResultRow(icon = "*", title = "Personal Best", value = personalBest, modifier = Modifier.weight(1f))
                    if (dateLabel.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEFD2C4))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dateLabel,
                                color = WorkoutSecondary,
                                fontSize = 12.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    icon: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = icon,
            color = WorkoutPrimary,
            fontSize = 16.sp,
            lineHeight = 16.sp
        )
        Text(
            text = title,
            color = WorkoutText,
            fontSize = 15.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            color = WorkoutSecondary,
            fontSize = 15.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun SessionProgressCard(
    completedSets: Int,
    totalSets: Int,
    remainingSets: Int,
    estimatedTimeRemaining: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 354.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Session Progress",
            color = WorkoutText,
            fontSize = 18.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = WorkoutCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = "$completedSets of $totalSets sets completed",
                    color = WorkoutText,
                    fontSize = 16.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                SetProgressLine(completedSets = completedSets, totalSets = totalSets)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Estimated remaining: $remainingSets sets ($estimatedTimeRemaining)",
                    color = WorkoutText,
                    fontSize = 16.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun SetProgressLine(
    completedSets: Int,
    totalSets: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..totalSets).forEach { index ->
            val nodeColor = when {
                index <= completedSets -> WorkoutGreen
                index == completedSets + 1 -> WorkoutLineTrack
                else -> Color(0xFFF0CCC0)
            }
            val textColor = if (index <= completedSets) Color.White else WorkoutText
            Box(
                modifier = Modifier
                    .size(19.dp)
                    .clip(CircleShape)
                    .background(nodeColor),
                contentAlignment = Alignment.Center
            ) {
                if (index <= completedSets) {
                    Image(
                        painter = painterResource(id = R.drawable.check_mark),
                        contentDescription = null,
                        modifier = Modifier.size(11.dp, 9.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = index.toString(),
                        color = textColor,
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (index < totalSets) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(
                            color = if (index < completedSets) WorkoutGreen else WorkoutLineTrack,
                            shape = RoundedCornerShape(999.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun OngoingBottomNavigationBar(
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(WorkoutNavBg)
            .border(width = 1.dp, color = Color(0x80D6AA98))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OngoingBottomNavItem(label = "Home", icon = R.drawable.home_nav_home, textColor = WorkoutInactiveIcon, onClick = onHomeClick)
        OngoingBottomNavItem(label = "Workout", icon = R.drawable.home_nav_workout_curr, textColor = Color(0xFFBF7E65), onClick = {})
        OngoingBottomNavItem(label = "Progress", icon = R.drawable.home_nav_progress, textColor = WorkoutInactiveIcon, onClick = onProgressClick)
        OngoingBottomNavItem(label = "Profile", icon = R.drawable.home_nav_profile, textColor = WorkoutInactiveIcon, onClick = onProfileClick)
    }
}

@Composable
private fun OngoingBottomNavItem(
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
            fontSize = 11.sp,
            lineHeight = 12.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 868)
@Composable
private fun OngoingWorkoutScreenPreview() {
    MaterialTheme {
        OngoingWorkoutScreen(
            uiState = OngoingWorkoutFakeStateProvider.contentState(),
            onBackClick = {},
            onEditClick = {},
            onWeightMinusClick = {},
            onWeightPlusClick = {},
            onRepsMinusClick = {},
            onRepsPlusClick = {},
            onWeightPresetClick = {},
            onRepsPresetClick = {},
            onRpeSelectClick = {},
            onLogSetClick = {},
            onHomeClick = {},
            onProgressClick = {},
            onProfileClick = {}
        )
    }
}
