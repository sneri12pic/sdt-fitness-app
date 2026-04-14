package com.stepandemianenko.sdtfitness.startworkout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepandemianenko.sdtfitness.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private object LogWorkoutDimens {
    val HorizontalPadding = StartWorkoutDimens.HorizontalPadding
    val HeaderTopPadding = StartWorkoutDimens.HeaderTopPadding
    val TopPadding = 16.dp
    val BottomPadding = 16.dp
    val SectionSpacing = 12.dp
    val CardCorner = StartWorkoutDimens.CardCorner
    val InnerCardCorner = 12.dp
    val MiniButtonCorner = 10.dp
    val TopProgressHeight = 6.dp
    val SetRowVerticalPadding = 8.dp
    val NumericFieldWidth = 58.dp
    val NumericFieldHeight = 32.dp
    val FooterButtonHeight = 54.dp
    val TimerActionMinTouch = 44.dp
    val TimerChipHeight = 44.dp
}

private val LogWorkoutBackground = Color(0xFFEBC0B0)
private val LogWorkoutCardBackground = Color(0xFFFEEADC)
private val LogWorkoutSubtleBackground = Color(0xFFF7E6DC)
private val LogWorkoutPrimary = Color(0xFFF48863)
private val LogWorkoutText = Color(0xFF582C1F)
private val LogWorkoutSecondaryText = Color(0xFF8A5A49)
private val LogWorkoutDivider = Color(0xFFE2B7A5)
private val LogWorkoutSuccess = Color(0xFF70C97D)
private val LogWorkoutSuccessSoft = Color(0xFFDDEED8)
private val LogWorkoutMuted = Color(0xFFC48778)
private val LogWorkoutDeleteBackground = Color(0xFFD85C4A)
private val BottomBarBg = Color(0xFFF7E6DC)
private val InactiveIcon = Color(0xFFC48778)
private const val RestTimerPageIndex = 0
private const val WorkoutPageIndex = 1

@Composable
fun OngoingWorkoutRoute(
    initialSessionId: Long?,
    onBackClick: () -> Unit,
    onNavigateToStartWorkout: () -> Unit,
    onTimerClick: () -> Unit,
    onAddExerciseClick: () -> Unit,
    onLogSetClick: (weightKg: Int, reps: Int, rpeIndex: Int) -> Unit,
    onSessionCompleted: (Long) -> Unit = {},
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: LogWorkoutViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(
        initialPage = WorkoutPageIndex,
        pageCount = { 2 }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialSessionId) {
        viewModel.attachSession(initialSessionId)
    }

    LaunchedEffect(viewModel, pagerState) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LogWorkoutEffect.NavigateToProgress -> onSessionCompleted(effect.sessionId)
                LogWorkoutEffect.OpenAddExercise -> onAddExerciseClick()
                LogWorkoutEffect.OpenRestTimer -> pagerState.animateScrollToPage(RestTimerPageIndex)
                is LogWorkoutEffect.ShowSnackbar -> {
                    launch {
                        val snackbarResult = snackbarHostState.showSnackbar(
                            message = effect.message,
                            actionLabel = effect.actionLabel
                        )
                        val actionPerformed = snackbarResult == SnackbarResult.ActionPerformed
                        if (
                            effect.actionLabel != null &&
                            actionPerformed
                        ) {
                            viewModel.onEvent(LogWorkoutUiEvent.OnUndoLastDeletion)
                        }
                        if (effect.actionLabel != null) {
                            viewModel.onEvent(
                                LogWorkoutUiEvent.OnDeletionSnackbarResult(
                                    actionPerformed = actionPerformed
                                )
                            )
                        }
                    }
                }
                LogWorkoutEffect.CloseAfterDiscard -> onBackClick()
                LogWorkoutEffect.NavigateToStartWorkout -> onNavigateToStartWorkout()
            }
        }
    }

    BackHandler(enabled = pagerState.currentPage == RestTimerPageIndex) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(WorkoutPageIndex)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            RestTimerPageIndex -> RestTimerScreen(
                restTimer = uiState.value.restTimer,
                onBackToWorkout = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(WorkoutPageIndex)
                    }
                },
                onStartTimer = { durationSeconds ->
                    viewModel.onEvent(LogWorkoutUiEvent.OnStartRestTimer(durationSeconds))
                },
                onAddTenSeconds = {
                    viewModel.onEvent(LogWorkoutUiEvent.OnExtendRestTimerByTenSeconds)
                },
                onDismissTimer = {
                    viewModel.onEvent(LogWorkoutUiEvent.OnDismissRestTimer)
                },
                onHomeClick = onHomeClick,
                onProgressClick = onProgressClick,
                onProfileClick = onProfileClick
            )

            WorkoutPageIndex -> LogWorkoutScreen(
                uiState = uiState.value,
                snackbarHostState = snackbarHostState,
                enableBackHandler = pagerState.currentPage == WorkoutPageIndex,
                onBackClick = {
                    viewModel.onEvent(LogWorkoutUiEvent.OnBackClicked)
                    onBackClick()
                },
                onTimerClick = {
                    onTimerClick()
                    viewModel.onEvent(LogWorkoutUiEvent.OnHeaderTimerTapped)
                },
                onFinishWorkoutClick = { viewModel.onEvent(LogWorkoutUiEvent.OnFinishWorkoutClicked) },
                onToggleSetCompleted = { exerciseId, set ->
                    viewModel.onEvent(
                        LogWorkoutUiEvent.OnToggleSetCompleted(
                            exerciseId = exerciseId,
                            setId = set.id
                        )
                    )
                    if (!set.isCompleted) {
                        onLogSetClick(
                            set.weight.toIntOrNull() ?: 0,
                            set.reps.toIntOrNull() ?: 1,
                            ((set.selectedRpe ?: 5) - 1).coerceAtLeast(0)
                        )
                    }
                },
                onDeleteSet = { exerciseId, setId ->
                    viewModel.onEvent(
                        LogWorkoutUiEvent.OnDeleteSet(
                            exerciseId = exerciseId,
                            setId = setId
                        )
                    )
                },
                onDeleteExercise = { exerciseId ->
                    viewModel.onEvent(
                        LogWorkoutUiEvent.OnDeleteExercise(exerciseId = exerciseId)
                    )
                },
                onAddSet = { exerciseId ->
                    viewModel.onEvent(LogWorkoutUiEvent.OnAddSet(exerciseId = exerciseId))
                },
                onAddExercise = { viewModel.onEvent(LogWorkoutUiEvent.OnAddExercise) },
                onUpdateSetWeight = { exerciseId, setId, weight ->
                    viewModel.onEvent(
                        LogWorkoutUiEvent.OnUpdateSetWeight(
                            exerciseId = exerciseId,
                            setId = setId,
                            weight = weight
                        )
                    )
                },
                onUpdateSetReps = { exerciseId, setId, reps ->
                    viewModel.onEvent(
                        LogWorkoutUiEvent.OnUpdateSetReps(
                            exerciseId = exerciseId,
                            setId = setId,
                            reps = reps
                        )
                    )
                },
                onSelectRpe = { exerciseId, setId, rpe ->
                    viewModel.onEvent(
                        LogWorkoutUiEvent.OnSelectRpe(
                            exerciseId = exerciseId,
                            setId = setId,
                            rpe = rpe
                        )
                    )
                },
                onDismissFeedback = { exerciseId, setId ->
                    viewModel.onEvent(
                        LogWorkoutUiEvent.OnDismissFeedback(
                            exerciseId = exerciseId,
                            setId = setId
                        )
                    )
                },
                onToggleRestTimer = { exerciseId ->
                    viewModel.onEvent(LogWorkoutUiEvent.OnToggleRestTimer(exerciseId = exerciseId))
                },
                onDiscardWorkoutClick = { viewModel.onEvent(LogWorkoutUiEvent.OnDiscardWorkoutClicked) },
                onDismissDiscardDialog = { viewModel.onEvent(LogWorkoutUiEvent.OnDismissDiscardConfirmation) },
                onConfirmDiscardWorkout = { viewModel.onEvent(LogWorkoutUiEvent.OnConfirmDiscardWorkout) },
                onHomeClick = onHomeClick,
                onProgressClick = onProgressClick,
                onProfileClick = onProfileClick
            )

            else -> Unit
        }
    }
}

@Composable
fun LogWorkoutScreen(
    uiState: LogWorkoutUiState,
    snackbarHostState: SnackbarHostState,
    enableBackHandler: Boolean,
    onBackClick: () -> Unit,
    onTimerClick: () -> Unit,
    onFinishWorkoutClick: () -> Unit,
    onToggleSetCompleted: (Long, WorkoutSetUiModel) -> Unit,
    onDeleteSet: (Long, String) -> Unit,
    onDeleteExercise: (Long) -> Unit,
    onAddSet: (Long) -> Unit,
    onAddExercise: () -> Unit,
    onUpdateSetWeight: (Long, String, String) -> Unit,
    onUpdateSetReps: (Long, String, String) -> Unit,
    onSelectRpe: (Long, String, Int) -> Unit,
    onDismissFeedback: (Long, String) -> Unit,
    onToggleRestTimer: (Long) -> Unit,
    onDiscardWorkoutClick: () -> Unit,
    onDismissDiscardDialog: () -> Unit,
    onConfirmDiscardWorkout: () -> Unit,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = enableBackHandler, onBack = onBackClick)

    val session = uiState.session
    Scaffold(
        modifier = modifier,
        containerColor = LogWorkoutBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            if (!uiState.isLoading && session != null) {
                Surface(
                    color = LogWorkoutBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                ) {
                    Box(
                        modifier = Modifier.padding(
                            start = LogWorkoutDimens.HorizontalPadding,
                            end = LogWorkoutDimens.HorizontalPadding,
                            top = LogWorkoutDimens.HeaderTopPadding,
                            bottom = 10.dp
                        )
                    ) {
                        LogWorkoutTopBar(
                            title = "Log Workout",
                            progress = session.progress,
                            restTimer = uiState.restTimer,
                            onBackClick = onBackClick,
                            onTimerClick = onTimerClick,
                            onFinishClick = onFinishWorkoutClick
                        )
                    }
                }
            }
        },
        bottomBar = {
            OngoingBottomNavigationBar(
                onHomeClick = onHomeClick,
                onProgressClick = onProgressClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            StatusMessageLayout(
                text = "Loading active workout...",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        if (session == null) {
            StatusMessageLayout(
                text = uiState.message ?: "No active workout session. Start one from Start Workout.",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = LogWorkoutDimens.HorizontalPadding,
                end = LogWorkoutDimens.HorizontalPadding,
                top = 6.dp,
                bottom = LogWorkoutDimens.BottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(LogWorkoutDimens.SectionSpacing)
        ) {
            item(key = "session_summary") {
                SessionSummaryRow(
                    duration = session.durationText,
                    volume = session.volumeText,
                    sets = session.setsText
                )
            }

            items(
                items = session.exercises,
                key = { exercise -> exercise.id }
            ) { exercise ->
                ExerciseLogCard(
                    exercise = exercise,
                    rpeOptions = defaultLogWorkoutRpeOptions(),
                    onUpdateSetWeight = { setId, weight ->
                        onUpdateSetWeight(exercise.id, setId, weight)
                    },
                    onUpdateSetReps = { setId, reps ->
                        onUpdateSetReps(exercise.id, setId, reps)
                    },
                    onToggleSetCompleted = { set ->
                        onToggleSetCompleted(exercise.id, set)
                    },
                    onDeleteSet = { setId ->
                        onDeleteSet(exercise.id, setId)
                    },
                    onDeleteExercise = {
                        onDeleteExercise(exercise.id)
                    },
                    onSelectRpe = { setId, rpe ->
                        onSelectRpe(exercise.id, setId, rpe)
                    },
                    onDismissFeedback = { setId ->
                        onDismissFeedback(exercise.id, setId)
                    },
                    onAddSet = { onAddSet(exercise.id) },
                    onToggleRestTimer = { onToggleRestTimer(exercise.id) }
                )
            }

            item(key = "bottom_actions") {
                AddExerciseButton(onAddExercise = onAddExercise)
            }

            item(key = "discard_action") {
                DiscardWorkoutAction(
                    enabled = !uiState.isDiscarding,
                    onClick = onDiscardWorkoutClick
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }

    if (uiState.showDiscardConfirmation) {
        DiscardWorkoutConfirmationDialog(
            isDiscarding = uiState.isDiscarding,
            onDismiss = onDismissDiscardDialog,
            onConfirmDiscard = onConfirmDiscardWorkout
        )
    }
}

@Composable
fun LogWorkoutTopBar(
    title: String,
    progress: Float,
    restTimer: RestTimerUiState,
    onBackClick: () -> Unit,
    onTimerClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                color = LogWorkoutText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBackClick)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            Text(
                text = title,
                color = LogWorkoutText,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            AnimatedContent(targetState = restTimer.state, label = "header_rest_timer_state") { timerState ->
                when (timerState) {
                    RestTimerState.Running -> {
                        ActiveRestTimerChip(
                            countdown = restTimer.countdownText,
                            onClick = onTimerClick
                        )
                    }

                    RestTimerState.Finished -> {
                        FinishedRestTimerChip(
                            countdown = restTimer.countdownText,
                            onClick = onTimerClick
                        )
                    }

                    RestTimerState.Inactive,
                    RestTimerState.Dismissed -> {
                        HeaderTimerIconButton(
                            onClick = onTimerClick
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(LogWorkoutDimens.MiniButtonCorner),
                color = LogWorkoutPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(LogWorkoutDimens.MiniButtonCorner))
                    .clickable(onClick = onFinishClick)
            ) {
                Text(
                    text = "Finish",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(LogWorkoutDimens.TopProgressHeight)
                .clip(RoundedCornerShape(999.dp)),
            color = LogWorkoutPrimary,
            trackColor = LogWorkoutDivider.copy(alpha = 0.65f)
        )
    }
}

@Composable
private fun HeaderTimerIconButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .sizeIn(
                minWidth = LogWorkoutDimens.TimerActionMinTouch,
                minHeight = LogWorkoutDimens.TimerActionMinTouch
            )
            .clip(CircleShape)
            .background(LogWorkoutSubtleBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.start_workout_streak_icon),
            contentDescription = "Open rest timer",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ActiveRestTimerChip(
    countdown: String,
    onClick: () -> Unit
) {
    Surface(
        color = LogWorkoutCardBackground,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .height(LogWorkoutDimens.TimerChipHeight)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .sizeIn(minHeight = LogWorkoutDimens.TimerChipHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = null,
                tint = LogWorkoutPrimary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = countdown,
                color = LogWorkoutText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FinishedRestTimerChip(
    countdown: String,
    onClick: () -> Unit
) {
    val pulse = rememberInfiniteTransition(label = "rest_timer_finished_pulse")
    val pulseScale = pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rest_timer_pulse_scale"
    )
    val pulseAlpha = pulse.animateFloat(
        initialValue = 0.26f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rest_timer_pulse_alpha"
    )

    Surface(
        color = LogWorkoutCardBackground,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .height(LogWorkoutDimens.TimerChipHeight)
            .graphicsLayer {
                scaleX = pulseScale.value
                scaleY = pulseScale.value
            }
            .border(
                width = 1.5.dp,
                color = Color(0xFFD06A6A).copy(alpha = pulseAlpha.value),
                shape = RoundedCornerShape(999.dp)
            )
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .sizeIn(minHeight = LogWorkoutDimens.TimerChipHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = null,
                tint = Color(0xFFB84A4A),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = countdown,
                color = LogWorkoutText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RestTimerScreen(
    restTimer: RestTimerUiState,
    onBackToWorkout: () -> Unit,
    onStartTimer: (Int) -> Unit,
    onAddTenSeconds: () -> Unit,
    onDismissTimer: () -> Unit,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Scaffold(
        containerColor = LogWorkoutBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Surface(
                color = LogWorkoutBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = LogWorkoutDimens.HorizontalPadding,
                            end = LogWorkoutDimens.HorizontalPadding,
                            top = LogWorkoutDimens.HeaderTopPadding,
                            bottom = 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "‹",
                        color = LogWorkoutText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onBackToWorkout)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "Rest Timer",
                        color = LogWorkoutText,
                        fontSize = 30.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
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
                .padding(horizontal = LogWorkoutDimens.HorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LogWorkoutCardBackground,
                shape = RoundedCornerShape(LogWorkoutDimens.CardCorner)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Take your rest without leaving this workout.",
                        color = LogWorkoutText,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Swipe left anytime to get back to logging.",
                        color = LogWorkoutSecondaryText,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LogWorkoutCardBackground,
                shape = RoundedCornerShape(LogWorkoutDimens.CardCorner)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = restTimer.countdownText,
                        color = LogWorkoutText,
                        fontSize = 42.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    when (restTimer.state) {
                        RestTimerState.Running -> {
                            Text(
                                text = "Rest timer is running.",
                                color = LogWorkoutSecondaryText,
                                fontSize = 14.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RestTimerActionButton(
                                    label = "+10 sec",
                                    onClick = onAddTenSeconds
                                )
                                RestTimerActionButton(
                                    label = "End timer",
                                    onClick = onDismissTimer
                                )
                            }
                        }

                        RestTimerState.Finished -> {
                            Text(
                                text = "Time's up. Ready for the next set?",
                                color = LogWorkoutPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RestTimerActionButton(
                                    label = "Dismiss",
                                    onClick = onDismissTimer
                                )
                                RestTimerActionButton(
                                    label = "+10 sec",
                                    onClick = onAddTenSeconds
                                )
                            }
                        }

                        RestTimerState.Inactive,
                        RestTimerState.Dismissed -> {
                            Text(
                                text = "Pick a quick rest length.",
                                color = LogWorkoutSecondaryText,
                                fontSize = 14.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RestTimerActionButton(
                                    label = "60 sec",
                                    onClick = { onStartTimer(60) }
                                )
                                RestTimerActionButton(
                                    label = "90 sec",
                                    onClick = { onStartTimer(90) }
                                )
                                RestTimerActionButton(
                                    label = "120 sec",
                                    onClick = { onStartTimer(120) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestTimerActionButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = LogWorkoutSubtleBackground,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .sizeIn(minHeight = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = LogWorkoutText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun SessionSummaryRow(
    duration: String,
    volume: String,
    sets: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LogWorkoutCardBackground,
        shape = RoundedCornerShape(LogWorkoutDimens.CardCorner)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(LogWorkoutSubtleBackground),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_workout_thumb),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }

            SessionMetricCell(label = "Duration", value = duration, modifier = Modifier.weight(1f))
            SessionMetricCell(label = "Volume", value = volume, modifier = Modifier.weight(1f))
            SessionMetricCell(label = "Sets", value = sets, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SessionMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = LogWorkoutSecondaryText,
            fontSize = 12.sp,
            lineHeight = 14.sp
        )
        Text(
            text = value,
            color = LogWorkoutText,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ExerciseLogCard(
    exercise: ExerciseUiModel,
    rpeOptions: List<LogWorkoutRpeOptionUiModel>,
    onUpdateSetWeight: (String, String) -> Unit,
    onUpdateSetReps: (String, String) -> Unit,
    onToggleSetCompleted: (WorkoutSetUiModel) -> Unit,
    onDeleteSet: (String) -> Unit,
    onDeleteExercise: () -> Unit,
    onSelectRpe: (String, Int) -> Unit,
    onDismissFeedback: (String) -> Unit,
    onAddSet: () -> Unit,
    onToggleRestTimer: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LogWorkoutCardBackground,
        shape = RoundedCornerShape(LogWorkoutDimens.CardCorner)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SwipeToDeleteContainer(
                rowKey = "exercise_header_${exercise.id}",
                onDelete = onDeleteExercise,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = exercise.thumbnailRes),
                        contentDescription = exercise.name,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LogWorkoutSubtleBackground)
                            .padding(6.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = exercise.name,
                        color = LogWorkoutText,
                        fontSize = 24.sp,
                        lineHeight = 26.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    Image(
                        painter = painterResource(id = R.drawable.start_workout_icon_more),
                        contentDescription = "More",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = if (exercise.notes.isBlank()) "Add notes here..." else exercise.notes,
                color = LogWorkoutSecondaryText,
                fontSize = 14.sp,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.restTimerStatusText,
                    color = LogWorkoutSecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (exercise.isRestTimerOn) "Turn off" else "Turn on",
                    color = LogWorkoutPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleRestTimer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Surface(
                color = LogWorkoutSubtleBackground,
                shape = RoundedCornerShape(LogWorkoutDimens.InnerCardCorner)
            ) {
                if (exercise.sets.isEmpty()) {
                    Text(
                        text = "Add a set to start this exercise",
                        color = LogWorkoutSecondaryText,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SetTableHeader()
                        exercise.sets.forEachIndexed { index, set ->
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(LogWorkoutDivider.copy(alpha = 0.55f))
                                )
                            }
                            WorkoutSetRow(
                                set = set,
                                onWeightChanged = { onUpdateSetWeight(set.id, it) },
                                onRepsChanged = { onUpdateSetReps(set.id, it) },
                                onToggleCompleted = { onToggleSetCompleted(set) },
                                onDelete = { onDeleteSet(set.id) }
                            )
                        }
                    }
                }
            }

            Text(
                text = "+ Add Set",
                color = LogWorkoutPrimary,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAddSet)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            exercise.sets
                .firstOrNull { it.activeFeedbackVisible }
                ?.let { activeSet ->
                    PostSetFeedbackCard(
                        set = activeSet,
                        rpeOptions = rpeOptions,
                        onSelectRpe = { rpe -> onSelectRpe(activeSet.id, rpe) },
                        onDismiss = { onDismissFeedback(activeSet.id) }
                    )
                }
        }
    }
}

@Composable
fun SetTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SetCellText(text = "SET", modifier = Modifier.width(36.dp), center = false)
        SetCellText(text = "PREVIOUS", modifier = Modifier.weight(1f), center = false)
        SetCellText(text = "KG", modifier = Modifier.width(64.dp), center = true)
        SetCellText(text = "REPS", modifier = Modifier.width(64.dp), center = true)
        SetCellText(text = "✓", modifier = Modifier.width(44.dp), center = true)
    }
}

@Composable
private fun SetCellText(
    text: String,
    modifier: Modifier,
    center: Boolean
) {
    Text(
        text = text,
        color = LogWorkoutSecondaryText,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        textAlign = if (center) TextAlign.Center else TextAlign.Start,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteContainer(
    rowKey: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    key(rowKey) {
        val dismissState = rememberSwipeToDismissBoxState(
            positionalThreshold = { fullWidth -> fullWidth * 0.35f },
            confirmValueChange = { targetValue ->
                if (targetValue == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    false
                } else {
                    false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = false,
            enableDismissFromEndToStart = true,
            modifier = modifier,
            backgroundContent = {
                val isDeleteDirection =
                    dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (isDeleteDirection) {
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LogWorkoutDeleteBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.start_workout_icon_bin),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        ) {
            content()
        }
    }
}

@Composable
fun WorkoutSetRow(
    set: WorkoutSetUiModel,
    onWeightChanged: (String) -> Unit,
    onRepsChanged: (String) -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit
) {
    SwipeToDeleteContainer(
        rowKey = "set_row_${set.id}",
        onDelete = onDelete,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(
                    if (set.isCompleted) {
                        LogWorkoutSuccessSoft.copy(alpha = 0.32f)
                    } else {
                        Color.Transparent
                    }
                )
                .padding(horizontal = 10.dp, vertical = LogWorkoutDimens.SetRowVerticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = set.setNumber.toString(),
                color = LogWorkoutText,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(36.dp)
            )

            Text(
                text = set.previousPerformance,
                color = LogWorkoutSecondaryText,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )

            EditableSetValueField(
                value = set.weight,
                enabled = true,
                onValueChange = onWeightChanged,
                modifier = Modifier.width(LogWorkoutDimens.NumericFieldWidth)
            )

            Spacer(modifier = Modifier.width(6.dp))

            EditableSetValueField(
                value = set.reps,
                enabled = true,
                onValueChange = onRepsChanged,
                modifier = Modifier.width(LogWorkoutDimens.NumericFieldWidth)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = 1.dp,
                        color = if (set.isCompleted) {
                            Color.Transparent
                        } else if (set.isCompletionEnabled) {
                            LogWorkoutPrimary.copy(alpha = 0.75f)
                        } else {
                            LogWorkoutDivider
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    .background(
                        when {
                            set.isCompleted -> LogWorkoutSuccess
                            else -> LogWorkoutSubtleBackground
                        }
                    )
                    .clickable(
                        enabled = set.isCompleted || set.isCompletionEnabled,
                        onClick = onToggleCompleted
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (set.isCompleted) {
                    Image(
                        painter = painterResource(id = R.drawable.check_mark),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
private fun EditableSetValueField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(LogWorkoutDimens.NumericFieldHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) Color.White.copy(alpha = 0.65f) else LogWorkoutDivider.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            enabled = enabled,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = LogWorkoutText,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun PostSetFeedbackCard(
    set: WorkoutSetUiModel,
    rpeOptions: List<LogWorkoutRpeOptionUiModel>,
    onSelectRpe: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LogWorkoutSubtleBackground,
        shape = RoundedCornerShape(LogWorkoutDimens.InnerCardCorner)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = set.feedbackMessage ?: "Set logged successfully",
                    color = LogWorkoutPrimary,
                    fontSize = 18.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "X",
                    color = LogWorkoutSecondaryText,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Text(
                text = "How did that feel?",
                color = LogWorkoutText,
                fontSize = 18.sp,
                lineHeight = 20.sp
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rpeOptions, key = { option -> option.rpeValue }) { option ->
                    val selected = set.selectedRpe == option.rpeValue
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) LogWorkoutSuccessSoft else LogWorkoutCardBackground,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = LogWorkoutDivider.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(onClick = { onSelectRpe(option.rpeValue) })
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Image(
                                painter = painterResource(
                                    id = if (selected) option.selectedIconRes else option.iconRes
                                ),
                                contentDescription = option.label,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = option.label,
                                color = LogWorkoutText,
                                fontSize = 13.sp,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = option.range,
                                color = LogWorkoutSecondaryText,
                                fontSize = 11.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddExerciseButton(onAddExercise: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(LogWorkoutDimens.FooterButtonHeight)
            .clip(RoundedCornerShape(StartWorkoutDimens.PrimaryButtonCorner))
            .clickable(onClick = onAddExercise),
        color = LogWorkoutPrimary,
        shape = RoundedCornerShape(StartWorkoutDimens.PrimaryButtonCorner)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "+ Add Exercise",
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DiscardWorkoutAction(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LogWorkoutDivider.copy(alpha = 0.8f))
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LogWorkoutDimens.CardCorner))
                .clickable(enabled = enabled, onClick = onClick),
            color = LogWorkoutCardBackground,
            shape = RoundedCornerShape(LogWorkoutDimens.CardCorner)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.start_workout_icon_bin),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Discard workout",
                    color = if (enabled) MaterialTheme.colorScheme.error else LogWorkoutSecondaryText,
                    fontSize = 15.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DiscardWorkoutConfirmationDialog(
    isDiscarding: Boolean,
    onDismiss: () -> Unit,
    onConfirmDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isDiscarding) {
                onDismiss()
            }
        },
        title = {
            Text(text = "Discard current workout?")
        },
        text = {
            Text(
                text = "This will remove the current in-progress workout and any unsaved set logs."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmDiscard,
                enabled = !isDiscarding
            ) {
                Text(
                    text = if (isDiscarding) "Discarding..." else "Discard",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDiscarding
            ) {
                Text(text = "Keep workout")
            }
        }
    )
}

@Composable
private fun StatusMessageLayout(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LogWorkoutDimens.HorizontalPadding)
            .padding(top = LogWorkoutDimens.TopPadding),
        verticalArrangement = Arrangement.Top
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LogWorkoutCardBackground,
            shape = RoundedCornerShape(LogWorkoutDimens.CardCorner)
        ) {
            Text(
                text = text,
                color = LogWorkoutText,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(14.dp)
            )
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
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal))
            .background(BottomBarBg)
            .border(width = 1.dp, color = Color(0x80D6AA98))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OngoingBottomNavItem(label = "Home", icon = R.drawable.home_nav_home, textColor = InactiveIcon, onClick = onHomeClick)
        OngoingBottomNavItem(label = "Workout", icon = R.drawable.home_nav_workout_curr, textColor = LogWorkoutMuted, onClick = {})
        OngoingBottomNavItem(label = "Progress", icon = R.drawable.home_nav_progress, textColor = InactiveIcon, onClick = onProgressClick)
        OngoingBottomNavItem(label = "Profile", icon = R.drawable.home_nav_profile, textColor = InactiveIcon, onClick = onProfileClick)
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
private fun LogWorkoutScreenPreview() {
    MaterialTheme {
        LogWorkoutScreen(
            uiState = LogWorkoutFakeStateProvider.contentState(),
            snackbarHostState = SnackbarHostState(),
            enableBackHandler = true,
            onBackClick = {},
            onTimerClick = {},
            onFinishWorkoutClick = {},
            onToggleSetCompleted = { _, _ -> },
            onDeleteSet = { _, _ -> },
            onDeleteExercise = {},
            onAddSet = {},
            onAddExercise = {},
            onUpdateSetWeight = { _, _, _ -> },
            onUpdateSetReps = { _, _, _ -> },
            onSelectRpe = { _, _, _ -> },
            onDismissFeedback = { _, _ -> },
            onToggleRestTimer = {},
            onDiscardWorkoutClick = {},
            onDismissDiscardDialog = {},
            onConfirmDiscardWorkout = {},
            onHomeClick = {},
            onProgressClick = {},
            onProfileClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 402, heightDp = 868)
@Composable
private fun LogWorkoutScreenFeedbackPreview() {
    MaterialTheme {
        LogWorkoutScreen(
            uiState = LogWorkoutFakeStateProvider.contentStateWithFeedback(),
            snackbarHostState = SnackbarHostState(),
            enableBackHandler = true,
            onBackClick = {},
            onTimerClick = {},
            onFinishWorkoutClick = {},
            onToggleSetCompleted = { _, _ -> },
            onDeleteSet = { _, _ -> },
            onDeleteExercise = {},
            onAddSet = {},
            onAddExercise = {},
            onUpdateSetWeight = { _, _, _ -> },
            onUpdateSetReps = { _, _, _ -> },
            onSelectRpe = { _, _, _ -> },
            onDismissFeedback = { _, _ -> },
            onToggleRestTimer = {},
            onDiscardWorkoutClick = {},
            onDismissDiscardDialog = {},
            onConfirmDiscardWorkout = {},
            onHomeClick = {},
            onProgressClick = {},
            onProfileClick = {}
        )
    }
}
