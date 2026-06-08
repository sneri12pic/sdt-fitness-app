package com.stepandemianenko.sdtfitness.startworkout

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
//import com.stepandemianenko.sdtfitness.BottomBarBg
//import com.stepandemianenko.sdtfitness.InactiveIcon
import com.stepandemianenko.sdtfitness.R
import com.stepandemianenko.sdtfitness.noRippleClickable
import kotlinx.coroutines.flow.collect

private val StartWorkoutBackground = Color(0xFFEBC0B0)
private val StartWorkoutCardBackground = Color(0xFFFEEADC)
private val StartWorkoutPrimary = Color(0xFFF48863)
private val StartWorkoutPrimaryText = Color(0xFF582C1F)
private val StartWorkoutSecondaryText = Color(0xFF8A5A49)
private val StartWorkoutDivider = Color(0xFFE2B7A5)
private val BottomBarBg = Color(0xFFF7E6DC)
private val InactiveIcon = Color(0xFFC48778)

@Composable
fun StartWorkoutRoute(
    onBackClick: () -> Unit,
    onStartWorkoutClick: (Long) -> Unit,
    openAddExerciseOnStart: Boolean = false,
    appendToSessionId: Long? = null,
    onAddExerciseClick: () -> Unit,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: StartWorkoutViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var hasTriggeredOpenAddExercise by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is StartWorkoutEffect.NavigateToOngoingWorkout -> onStartWorkoutClick(effect.sessionId)
            }
        }
    }

    LaunchedEffect(openAddExerciseOnStart, appendToSessionId) {
        viewModel.setAppendToSessionId(appendToSessionId)
        viewModel.setAppendModeEnabled(openAddExerciseOnStart)
        if (openAddExerciseOnStart && !hasTriggeredOpenAddExercise) {
            viewModel.onEvent(StartWorkoutUiEvent.AddExerciseClick)
            hasTriggeredOpenAddExercise = true
        }
    }

    if (openAddExerciseOnStart || uiState.isSelectingExercises) {
        ExercisesScreen(
            uiState = uiState.selectExercises,
            onBackClick = {
                if (openAddExerciseOnStart) {
                    onBackClick()
                } else {
                    viewModel.onEvent(StartWorkoutUiEvent.CloseExercisePickerClick)
                }
            },
            onSearchQueryChange = { query ->
                viewModel.onEvent(StartWorkoutUiEvent.SearchQueryChanged(query))
            },
            onMuscleGroupSelect = { muscleGroup ->
                viewModel.onEvent(StartWorkoutUiEvent.MuscleGroupSelected(muscleGroup))
            },
            onExerciseToggle = { id ->
                viewModel.onEvent(StartWorkoutUiEvent.ToggleExerciseSelection(id))
            },
            onSaveCustomSet = { setId, name, exerciseIds ->
                viewModel.onEvent(
                    StartWorkoutUiEvent.SaveCustomExerciseSet(
                        setId = setId,
                        name = name,
                        exerciseIds = exerciseIds
                    )
                )
            },
            onCustomSetSelect = { setId ->
                viewModel.onEvent(StartWorkoutUiEvent.SelectCustomExerciseSet(setId))
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
            onBackClick = onBackClick,
            onAddExerciseClick = {
                viewModel.onEvent(StartWorkoutUiEvent.AddExerciseClick)
            },
            onPlansClick = {
                viewModel.onEvent(StartWorkoutUiEvent.PlansClick)
            },
            snackbarHostState = snackbarHostState,
            onHomeClick = onHomeClick,
            onProgressClick = onProgressClick,
            onProfileClick = onProfileClick
        )
    }
}

@Composable
fun StartWorkoutScreen(
    onBackClick: () -> Unit,
    onAddExerciseClick: () -> Unit,
    onPlansClick: () -> Unit,
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
        StartWorkoutEmptyState(
            onAddExerciseClick = onAddExerciseClick,
            onPlansClick = onPlansClick,
            onBackClick = onBackClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = StartWorkoutDimens.HorizontalPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
        )
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
private fun StartWorkoutEmptyState(
    onAddExerciseClick: () -> Unit,
    onPlansClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .clickable(onClick = onBackClick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                color = StartWorkoutPrimaryText,
                fontSize = 22.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Back to Home",
                color = StartWorkoutSecondaryText,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Surface(
            shape = RoundedCornerShape(StartWorkoutDimens.EmptyStateCorner),
            color = StartWorkoutCardBackground,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Workout",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = StartWorkoutPrimaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = StartWorkoutDimens.PrimaryButtonTextSize
                    )
                )
                Text(
                    text = "Choose a saved plan or start from an empty workout.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = StartWorkoutSecondaryText,
                        fontSize = StartWorkoutDimens.BodyTextSize
                    )
                )
                StartWorkoutSecondaryButton(
                    text = "Plans",
                    onClick = onPlansClick
                )
                StartWorkoutPrimaryButton(
                    text = "Start Empty Workout",
                    onClick = onAddExerciseClick,
                    enabled = true
                )
            }
        }
    }
}

@Composable
private fun StartWorkoutSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(StartWorkoutDimens.PrimaryButtonCorner))
            .border(
                width = 1.dp,
                color = StartWorkoutDivider,
                shape = RoundedCornerShape(StartWorkoutDimens.PrimaryButtonCorner)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(StartWorkoutDimens.PrimaryButtonCorner),
        color = StartWorkoutCardBackground
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
                    color = StartWorkoutPrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = StartWorkoutDimens.PrimaryButtonTextSize
                )
            )
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
            .noRippleClickable(onClick)
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
private fun StartWorkoutScreenEmptyPreview() {
    StartWorkoutScreen(
        onBackClick = {},
        onAddExerciseClick = {},
        onPlansClick = {},
        snackbarHostState = SnackbarHostState(),
        onHomeClick = {},
        onProgressClick = {},
        onProfileClick = {}
    )
}

