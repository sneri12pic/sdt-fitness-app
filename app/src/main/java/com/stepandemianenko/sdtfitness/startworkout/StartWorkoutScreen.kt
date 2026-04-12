package com.stepandemianenko.sdtfitness.startworkout

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
//import com.stepandemianenko.sdtfitness.BottomBarBg
//import com.stepandemianenko.sdtfitness.InactiveIcon
import com.stepandemianenko.sdtfitness.R
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private val StartWorkoutBackground = Color(0xFFEBC0B0)
private val StartWorkoutCardBackground = Color(0xFFFEEADC)
private val StartWorkoutPrimary = Color(0xFFF48863)
private val StartWorkoutPrimaryText = Color(0xFF582C1F)
private val StartWorkoutSecondaryText = Color(0xFF8A5A49)
private val StartWorkoutDivider = Color(0xFFE2B7A5)
private val StartWorkoutBadge = Color(0xFFFC9502)
private val StartWorkoutBadgeText = Color(0xFF5C320A)
private val StartWorkoutSuccess = Color(0xFF70C97D)
private val BottomBarBg = Color(0xFFF7E6DC)
private val InactiveIcon = Color(0xFFC48778)

private enum class EditableTargetField {
    Weight,
    Reps
}

private data class ExerciseTargetEditorState(
    val exerciseId: String,
    val exerciseName: String,
    val field: EditableTargetField,
    val currentValue: Int
)

@Composable
fun StartWorkoutRoute(
    onBackClick: () -> Unit,
    onStartWorkoutClick: (Long) -> Unit,
    onShortenSessionClick: () -> Unit,
    onEditWorkoutClick: () -> Unit,
    onExerciseClick: (WorkoutExerciseUiModel) -> Unit,
    onAddExerciseClick: () -> Unit,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: StartWorkoutViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var targetEditor by remember { mutableStateOf<ExerciseTargetEditorState?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is StartWorkoutEffect.NavigateToOngoingWorkout -> onStartWorkoutClick(effect.sessionId)
            }
        }
    }

    if (uiState.isSelectingExercises) {
        ExercisesScreen(
            exercises = uiState.exerciseCatalog,
            selectedExerciseIds = uiState.selectedExerciseIds,
            onBackClick = { viewModel.onEvent(StartWorkoutUiEvent.CloseExercisePickerClick) },
            onExerciseToggle = { id ->
                viewModel.onEvent(StartWorkoutUiEvent.ToggleExerciseSelection(id))
            },
            onAddSelectedClick = {
                viewModel.onEvent(StartWorkoutUiEvent.ConfirmExerciseSelectionClick)
                onAddExerciseClick()
            },
            onHomeClick = onHomeClick,
            onProgressClick = onProgressClick,
            onProfileClick = onProfileClick
        )
    } else {
        StartWorkoutScreen(
            uiState = uiState,
            onBackClick = {
                viewModel.onEvent(StartWorkoutUiEvent.BackClick)
                onBackClick()
            },
            onStartWorkoutClick = {
                viewModel.onEvent(StartWorkoutUiEvent.StartWorkoutClick)
            },
            onShortenSessionClick = {
                viewModel.onEvent(StartWorkoutUiEvent.ShortenSessionClick)
                onShortenSessionClick()
            },
            onEditWorkoutClick = {
                viewModel.onEvent(StartWorkoutUiEvent.EditWorkoutClick)
                onEditWorkoutClick()
            },
            onExerciseClick = { exercise ->
                viewModel.onEvent(StartWorkoutUiEvent.ExerciseClick(exercise.id))
                onExerciseClick(exercise)
            },
            onExerciseWeightClick = { exercise ->
                targetEditor = ExerciseTargetEditorState(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    field = EditableTargetField.Weight,
                    currentValue = exercise.targetWeightKg
                )
            },
            onExerciseRepsClick = { exercise ->
                targetEditor = ExerciseTargetEditorState(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    field = EditableTargetField.Reps,
                    currentValue = exercise.targetReps
                )
            },
            onExerciseCompleteOrSkip = { exercise ->
                viewModel.onEvent(StartWorkoutUiEvent.CompleteOrSkipExercise(exercise.id))
            },
            onExerciseDelete = { exercise ->
                viewModel.onEvent(StartWorkoutUiEvent.DeleteExercise(exercise.id))
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "\"${exercise.name}\" removed",
                        actionLabel = "Undo",
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(StartWorkoutUiEvent.UndoDeleteExercise)
                    }
                }
            },
            onAddExerciseClick = {
                viewModel.onEvent(StartWorkoutUiEvent.AddExerciseClick)
            },
            onRetryClick = { viewModel.onEvent(StartWorkoutUiEvent.RetryLoad) },
            snackbarHostState = snackbarHostState,
            onHomeClick = onHomeClick,
            onProgressClick = onProgressClick,
            onProfileClick = onProfileClick
        )
    }

    targetEditor?.let { editor ->
        ExerciseTargetInputDialog(
            state = editor,
            onDismiss = { targetEditor = null },
            onSave = { newValue ->
                when (editor.field) {
                    EditableTargetField.Weight -> {
                        viewModel.onEvent(
                            StartWorkoutUiEvent.UpdateExerciseWeight(
                                exerciseId = editor.exerciseId,
                                weightKg = newValue
                            )
                        )
                    }

                    EditableTargetField.Reps -> {
                        viewModel.onEvent(
                            StartWorkoutUiEvent.UpdateExerciseReps(
                                exerciseId = editor.exerciseId,
                                reps = newValue
                            )
                        )
                    }
                }
                targetEditor = null
            }
        )
    }
}

@Composable
fun StartWorkoutScreen(
    uiState: StartWorkoutUiState,
    onBackClick: () -> Unit,
    onStartWorkoutClick: () -> Unit,
    onShortenSessionClick: () -> Unit,
    onEditWorkoutClick: () -> Unit,
    onExerciseClick: (WorkoutExerciseUiModel) -> Unit,
    onExerciseWeightClick: (WorkoutExerciseUiModel) -> Unit = {},
    onExerciseRepsClick: (WorkoutExerciseUiModel) -> Unit = {},
    onExerciseCompleteOrSkip: (WorkoutExerciseUiModel) -> Unit,
    onExerciseDelete: (WorkoutExerciseUiModel) -> Unit,
    onAddExerciseClick: () -> Unit,
    onRetryClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    Scaffold(
        modifier = modifier,
        containerColor = StartWorkoutBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            BottomNavigationBar(
                onHomeClick = onHomeClick,
                onProgressClick = onProgressClick,
                onProfileClick = onProfileClick
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                StartWorkoutLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            uiState.isEmpty -> {
                StartWorkoutEmptyState(
                    onRetryClick = onRetryClick,
                    onAddExerciseClick = onAddExerciseClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = StartWorkoutDimens.HorizontalPadding)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                )
            }

            else -> {
                val plan = requireNotNull(uiState.workoutPlan)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = StartWorkoutDimens.HorizontalPadding,
                        end = StartWorkoutDimens.HorizontalPadding,
                        top = StartWorkoutDimens.HeaderTopPadding,
                        bottom = StartWorkoutDimens.ContentBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(StartWorkoutDimens.SectionSpacing)
                ) {
                    item {
                        HeaderSection(
                            title = plan.headerTitle,
                            subtitle = plan.headerSubtitle
                        )
                    }
                    item {
                        WorkoutStreakCard(
                            streakInfo = plan.streakCard,
                            basedOnPlanText = plan.basedOnPlanText
                        )
                    }
                    item {
                        SelectedWorkoutCard(
                            title = plan.selectedWorkoutsTitle,
                            durationText = plan.estimatedDurationText,
                            badge = plan.selectedWorkoutBadge,
                            exercises = plan.exercises,
                            onEditClick = onEditWorkoutClick,
                            onExerciseClick = onExerciseClick,
                            onExerciseWeightClick = onExerciseWeightClick,
                            onExerciseRepsClick = onExerciseRepsClick,
                            onExerciseCompleteOrSkip = onExerciseCompleteOrSkip,
                            onExerciseDelete = onExerciseDelete,
                            onAddExerciseClick = onAddExerciseClick
                        )
                    }
                    item {
                        SessionActionsRow(
                            swapCard = plan.swapExerciseCard,
                            shortenCard = plan.shortenSessionCard,
                            onSwapClick = onAddExerciseClick,
                            onShortenClick = onShortenSessionClick
                        )
                    }
                    item {
                        StartWorkoutPrimaryButton(
                            text = plan.primaryCtaText,
                            onClick = onStartWorkoutClick,
                            enabled = !uiState.isStartingWorkout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = StartWorkoutPrimaryText,
                fontSize = StartWorkoutDimens.HeaderTitleTextSize,
                lineHeight = StartWorkoutDimens.HeaderTitleLineHeight
            )
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = StartWorkoutSecondaryText,
                fontSize = StartWorkoutDimens.BodyTextSize,
                lineHeight = StartWorkoutDimens.BodyLineHeight
            ),
            modifier = Modifier.padding(top = StartWorkoutDimens.HeaderSubtitleTopPadding)
        )
    }
}

@Composable
private fun WorkoutStreakCard(
    streakInfo: WorkoutInfoCardUiModel,
    basedOnPlanText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(StartWorkoutDimens.CardCorner),
        color = StartWorkoutCardBackground
    ) {
        Column(modifier = Modifier.padding(StartWorkoutDimens.CardContentPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(StartWorkoutPrimary.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.start_workout_streak_icon),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = streakInfo.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = StartWorkoutPrimaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = StartWorkoutDimens.BodyTextSize
                        )
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = streakInfo.subtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = StartWorkoutSecondaryText,
                                fontSize = StartWorkoutDimens.BodyTextSize
                            )
                        )
                    }
                }
                streakInfo.badge?.let {
                    BadgeChip(
                        badge = it,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(StartWorkoutDivider)
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.start_workout_icon_empty_ellipse),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = basedOnPlanText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = StartWorkoutPrimaryText,
                        fontSize = StartWorkoutDimens.BodyTextSize
                    )
                )
            }
        }
    }
}

@Composable
private fun SelectedWorkoutCard(
    title: String,
    durationText: String,
    badge: WorkoutBadgeUiModel,
    exercises: List<WorkoutExerciseUiModel>,
    onEditClick: () -> Unit,
    onExerciseClick: (WorkoutExerciseUiModel) -> Unit,
    onExerciseWeightClick: (WorkoutExerciseUiModel) -> Unit = {},
    onExerciseRepsClick: (WorkoutExerciseUiModel) -> Unit = {},
    onExerciseCompleteOrSkip: (WorkoutExerciseUiModel) -> Unit,
    onExerciseDelete: (WorkoutExerciseUiModel) -> Unit,
    onAddExerciseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(StartWorkoutDimens.CardCorner),
        color = StartWorkoutCardBackground
    ) {
        Column(modifier = Modifier.padding(StartWorkoutDimens.CardContentPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$title •",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = StartWorkoutPrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = StartWorkoutDimens.BodyTextSize
                    )
                )
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = StartWorkoutPrimaryText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = StartWorkoutDimens.BodyTextSize
                    ),
                    modifier = Modifier.weight(1f)
                )
                BadgeChip(badge = badge)
            }

            if (exercises.isEmpty()) {
                Text(
                    text = "No exercises selected yet.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = StartWorkoutSecondaryText,
                        fontSize = StartWorkoutDimens.SecondaryBodyTextSize
                    ),
                    modifier = Modifier.padding(top = 14.dp)
                )
                AddExerciseAction(
                    onClick = onAddExerciseClick,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                exercises.forEachIndexed { index, exercise ->
                    if (index > 0) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .height(1.dp)
                                .background(StartWorkoutDivider)
                        )
                    }
                    SwipeableExerciseRow(
                        exercise = exercise,
                        onClick = { onExerciseClick(exercise) },
                        onEditClick = onEditClick,
                        onWeightClick = { onExerciseWeightClick(exercise) },
                        onRepsClick = { onExerciseRepsClick(exercise) },
                        onCompleteOrSkip = { onExerciseCompleteOrSkip(exercise) },
                        onDelete = { onExerciseDelete(exercise) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableExerciseRow(
    exercise: WorkoutExerciseUiModel,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onWeightClick: () -> Unit,
    onRepsClick: () -> Unit,
    onCompleteOrSkip: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
        positionalThreshold = { fullWidth -> fullWidth * 0.35f },
        confirmValueChange = { target ->
            when (target) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onCompleteOrSkip()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            SwipeActionBackground(dismissDirection = dismissState.dismissDirection)
        }
    ) {
        ExerciseRow(
            exercise = exercise,
            onClick = onClick,
            onEditClick = onEditClick,
            onWeightClick = onWeightClick,
            onRepsClick = onRepsClick
        )
    }
}

@Composable
private fun SwipeActionBackground(
    dismissDirection: SwipeToDismissBoxValue,
    modifier: Modifier = Modifier
) {
    val isCompleteAction = dismissDirection == SwipeToDismissBoxValue.StartToEnd
    val isDeleteAction = dismissDirection == SwipeToDismissBoxValue.EndToStart
    val background = when {
        isCompleteAction -> StartWorkoutSuccess.copy(alpha = 0.22f)
        isDeleteAction -> StartWorkoutPrimary.copy(alpha = 0.22f)
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isDeleteAction) Arrangement.End else Arrangement.Start
    ) {
        if (isCompleteAction) {
            Text(
                text = "Complete",
                color = Color(0xFF377D42),
                fontWeight = FontWeight.SemiBold,
                fontSize = StartWorkoutDimens.BodyTextSize
            )
        }
        if (isDeleteAction) {
            Text(
                text = "Remove",
                color = StartWorkoutPrimaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = StartWorkoutDimens.BodyTextSize
            )
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: WorkoutExerciseUiModel,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onWeightClick: () -> Unit,
    onRepsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(StartWorkoutPrimary.copy(alpha = 0.32f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = exercise.iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = StartWorkoutPrimaryText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = StartWorkoutDimens.BodyTextSize
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EditableTargetChip(
                    label = "Weight",
                    value = "${exercise.targetWeightKg} kg",
                    onClick = onWeightClick
                )
                EditableTargetChip(
                    label = "Reps",
                    value = exercise.targetReps.toString(),
                    onClick = onRepsClick
                )
            }
            Text(
                text = exercise.prescription,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = StartWorkoutSecondaryText,
                    fontSize = StartWorkoutDimens.CaptionTextSize
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = exercise.setsText,
                style = MaterialTheme.typography.titleSmall.copy(
                    color = StartWorkoutSecondaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = StartWorkoutDimens.BodyTextSize
                )
            )
            Text(
                text = exercise.estimatedTimeText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = StartWorkoutSecondaryText,
                    fontSize = StartWorkoutDimens.CaptionTextSize
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, StartWorkoutDivider, RoundedCornerShape(10.dp))
                .clickable(onClick = onEditClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.start_workout_icon_edit),
                contentDescription = "Edit exercise",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun EditableTargetChip(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, StartWorkoutDivider, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall.copy(
                color = StartWorkoutPrimaryText,
                fontWeight = FontWeight.Medium,
                fontSize = StartWorkoutDimens.CaptionTextSize
            )
        )
    }
}

@Composable
private fun ExerciseTargetInputDialog(
    state: ExerciseTargetEditorState,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val minimumValue = if (state.field == EditableTargetField.Reps) 1 else 0
    val fieldLabel = if (state.field == EditableTargetField.Weight) "Weight (kg)" else "Reps"
    val dialogTitle = if (state.field == EditableTargetField.Weight) {
        "Set weight for ${state.exerciseName}"
    } else {
        "Set reps for ${state.exerciseName}"
    }

    var input by remember(state) { mutableStateOf(state.currentValue.toString()) }
    val parsedValue = input.toIntOrNull()
    val isValid = parsedValue != null && parsedValue >= minimumValue

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = StartWorkoutPrimaryText,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { newValue ->
                    input = newValue.filter { it.isDigit() }
                },
                label = { Text(fieldLabel) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(requireNotNull(parsedValue)) },
                enabled = isValid
            ) {
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
private fun SessionActionsRow(
    swapCard: WorkoutInfoCardUiModel,
    shortenCard: WorkoutInfoCardUiModel,
    onSwapClick: () -> Unit,
    onShortenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SecondaryActionCard(
            title = swapCard.title,
            subtitle = swapCard.subtitle,
            onClick = onSwapClick,
            modifier = Modifier.weight(1f),
            trailing = {
                Image(
                    painter = painterResource(id = R.drawable.start_workout_icon_green_arrow),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                )
            }
        )

        SecondaryActionCard(
            title = shortenCard.title,
            subtitle = shortenCard.subtitle,
            onClick = onShortenClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SecondaryActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(StartWorkoutDimens.CardCorner))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(StartWorkoutDimens.CardCorner),
        color = StartWorkoutCardBackground
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 15.dp)) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = StartWorkoutPrimaryText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = StartWorkoutDimens.BodyTextSize
                )
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = StartWorkoutSecondaryText,
                        fontWeight = FontWeight.Medium,
                        fontSize = StartWorkoutDimens.CaptionTextSize
                    ),
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .fillMaxWidth(), textAlign = TextAlign.Center
                )
            }
            trailing?.let {
                Spacer(modifier = Modifier.height(3.dp))
                it()
            }
        }
    }
}

@Composable
private fun StartWorkoutPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(StartWorkoutDimens.PrimaryButtonCorner))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(StartWorkoutDimens.PrimaryButtonCorner),
        color = if (enabled) StartWorkoutPrimary else StartWorkoutPrimary.copy(alpha = 0.55f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = StartWorkoutCardBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = StartWorkoutDimens.PrimaryButtonTextSize
                )
            )
        }
    }
}

@Composable
private fun AddExerciseAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = StartWorkoutPrimary.copy(alpha = 0.14f)
    ) {
        Text(
            text = "Add Exercise",
            style = MaterialTheme.typography.titleMedium.copy(
                color = StartWorkoutPrimaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = StartWorkoutDimens.BodyTextSize
            ),
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp)
        )
    }
}
@Composable
private fun StartWorkoutLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = StartWorkoutPrimary,
                trackColor = StartWorkoutCardBackground
            )
            Text(
                text = "Preparing your workout...",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = StartWorkoutPrimaryText,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun StartWorkoutEmptyState(
    onRetryClick: () -> Unit,
    onAddExerciseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(StartWorkoutDimens.EmptyStateCorner),
            color = StartWorkoutCardBackground,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "No workout plan available",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = StartWorkoutPrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = StartWorkoutDimens.PrimaryButtonTextSize
                    )
                )
                Text(
                    text = "You can add exercises and build a quick session to get moving.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = StartWorkoutSecondaryText,
                        fontSize = StartWorkoutDimens.BodyTextSize
                    )
                )
                StartWorkoutPrimaryButton(
                    text = "Add Exercise",
                    onClick = onAddExerciseClick,
                    enabled = true
                )
            }
        }
    }
}

@Composable
private fun BadgeChip(
    badge: WorkoutBadgeUiModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(StartWorkoutDimens.BadgeCorner))
            .background(StartWorkoutBadge)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = badge.text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = StartWorkoutBadgeText,
                fontWeight = FontWeight.Bold,
                fontSize = StartWorkoutDimens.CaptionTextSize
            )
        )
    }
}

@Composable
private fun MiniPlayerBar(
    miniPlayer: WorkoutMiniPlayerUiModel,
    modifier: Modifier = Modifier,
    visible: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = StartWorkoutCardBackground
    ) {
        // Do no need it now INVISIBLE
        if (!visible) return@Surface

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.start_workout_icon_more),
                contentDescription = "Expand mini player",
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(StartWorkoutSuccess)
                    )
                    Text(
                        text = miniPlayer.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = StartWorkoutPrimaryText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                    )
                }
                Text(
                    text = miniPlayer.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = StartWorkoutSecondaryText,
                        fontSize = StartWorkoutDimens.SecondaryBodyTextSize,
                        lineHeight = StartWorkoutDimens.BodyLineHeight
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.start_workout_icon_bin),
                    contentDescription = "Remove mini player",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit,
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
            BottomNavItem(label = "Home", icon = R.drawable.home_nav_home, textColor = InactiveIcon, onClick = onHomeClick)
            BottomNavItem(label = "Workout", icon = R.drawable.home_nav_workout_curr, textColor = Color(0xFFBF7E65), onClick = {})
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
                        bottomStart = StartWorkoutDimens.BottomInsetCorner,
                        bottomEnd = StartWorkoutDimens.BottomInsetCorner
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
            fontSize = StartWorkoutDimens.BottomNavTextSize,
            lineHeight = StartWorkoutDimens.BottomNavLineHeight
        )
    }
}


@Preview(showBackground = true, widthDp = 402, heightDp = 868)
@Composable
private fun StartWorkoutScreenPreview() {
    StartWorkoutScreen(
        uiState = StartWorkoutFakeStateProvider.contentState(),
        onBackClick = {},
        onStartWorkoutClick = {},
        onShortenSessionClick = {},
        onEditWorkoutClick = {},
        onExerciseClick = {},
        onExerciseCompleteOrSkip = {},
        onExerciseDelete = {},
        onAddExerciseClick = {},
        onRetryClick = {},
        snackbarHostState = SnackbarHostState(),
        onHomeClick = {},
        onProgressClick = {},
        onProfileClick = {}
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 868)
@Composable
private fun StartWorkoutScreenLoadingPreview() {
    StartWorkoutScreen(
        uiState = StartWorkoutFakeStateProvider.loadingState(),
        onBackClick = {},
        onStartWorkoutClick = {},
        onShortenSessionClick = {},
        onEditWorkoutClick = {},
        onExerciseClick = {},
        onExerciseCompleteOrSkip = {},
        onExerciseDelete = {},
        onAddExerciseClick = {},
        onRetryClick = {},
        snackbarHostState = SnackbarHostState(),
        onHomeClick = {},
        onProgressClick = {},
        onProfileClick = {}
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 868)
@Composable
private fun StartWorkoutScreenEmptyPreview() {
    StartWorkoutScreen(
        uiState = StartWorkoutFakeStateProvider.emptyState(),
        onBackClick = {},
        onStartWorkoutClick = {},
        onShortenSessionClick = {},
        onEditWorkoutClick = {},
        onExerciseClick = {},
        onExerciseCompleteOrSkip = {},
        onExerciseDelete = {},
        onAddExerciseClick = {},
        onRetryClick = {},
        snackbarHostState = SnackbarHostState(),
        onHomeClick = {},
        onProgressClick = {},
        onProfileClick = {}
    )
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun WorkoutStreakCardPreview() {
    WorkoutStreakCard(
        streakInfo = StartWorkoutFakeStateProvider.defaultPlan(isShortened = false).streakCard,
        basedOnPlanText = "Based on your Upper Body plan"
    )
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun SelectedWorkoutCardPreview() {
    val plan = StartWorkoutFakeStateProvider.defaultPlan(isShortened = false)
    SelectedWorkoutCard(
        title = plan.selectedWorkoutsTitle,
        durationText = plan.estimatedDurationText,
        badge = plan.selectedWorkoutBadge,
        exercises = plan.exercises,
        onEditClick = {},
        onExerciseClick = {},
        onExerciseCompleteOrSkip = {},
        onExerciseDelete = {},
        onAddExerciseClick = {}
    )
}

