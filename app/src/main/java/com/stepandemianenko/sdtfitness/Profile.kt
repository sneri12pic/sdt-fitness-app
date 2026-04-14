package com.stepandemianenko.sdtfitness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.stepandemianenko.sdtfitness.data.AppGraph
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.Image

class Profile : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ProfileRoute(
                    onHomeClick = { openHomeWithoutAnimation() },
                    onWorkoutClick = { openWorkoutWithoutAnimation() },
                    onProgressClick = { openProgressWithoutAnimation() }
                )
            }
        }
    }

    private fun openHomeWithoutAnimation() {
        startActivity(Intent(this, Home::class.java))
        overridePendingTransition(0, 0)
    }

    private fun openWorkoutWithoutAnimation() {
        lifecycleScope.launch {
            val activeSessionId = AppGraph.workoutSessionRepository(this@Profile).getActiveSessionId()
            val intent = if (activeSessionId != null) {
                OngoingWorkout.createIntent(this@Profile, activeSessionId)
            } else {
                StartWorkout.createIntent(this@Profile)
            }
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun openProgressWithoutAnimation() {
        startActivity(Intent(this, Progress::class.java))
        overridePendingTransition(0, 0)
    }
}

private val ProfileBackground = Color(0xFFEBC0B0)
private val ProfileCardBackground = Color(0xFFF5E5DA)
private val ProfilePrimaryText = Color(0xFF4F2912)
private val ProfileSecondaryText = Color(0xFF6B4637)
private val ProfileAccent = Color(0xFFF27F3E)
private val ProfileBottomBarBg = Color(0xFFF5E5DA)
private val ProfileInactiveIcon = Color(0xFFC48778)
private val ProfileContentMaxWidth = 360.dp
private const val ProfileReservedBottomFraction = 0.15f
private val ProfileHorizontalPadding = 20.dp
private val ProfileTopPadding = 30.dp
private val ProfileBottomInsetCorner = 16.dp

private enum class ProfileScreen {
    Overview,
    YourRoutine
}

private data class RoutineGoalOption(
    val id: String,
    val title: String,
    val description: String,
    val placeholderSymbol: String
)

private data class RoutineSelectableOption(
    val id: String,
    val label: String
)

private val RoutineGoalOptions = listOf(
    RoutineGoalOption(
        id = "strength",
        title = "Strength",
        description = "Build muscle and track lifts",
        placeholderSymbol = "S"
    ),
    RoutineGoalOption(
        id = "general_fitness",
        title = "General Fitness",
        description = "Stay active with mixed sessions",
        placeholderSymbol = "G"
    ),
    RoutineGoalOption(
        id = "cardio_steps",
        title = "Cardio / Steps",
        description = "Focus on walking and activity goals",
        placeholderSymbol = "C"
    ),
    RoutineGoalOption(
        id = "staying_active",
        title = "Just Staying Active",
        description = "Keep a simple healthy routine",
        placeholderSymbol = "A"
    )
)

private val RoutineFrequencyOptions = listOf(
    RoutineSelectableOption(id = "2_days", label = "2 days / week"),
    RoutineSelectableOption(id = "3_days", label = "3 days / week"),
    RoutineSelectableOption(id = "4_days", label = "4 days / week"),
    RoutineSelectableOption(id = "5_plus_days", label = "5+ days / week")
)

private val RoutineDayOptions = listOf(
    RoutineSelectableOption(id = "mon", label = "Mon"),
    RoutineSelectableOption(id = "tue", label = "Tue"),
    RoutineSelectableOption(id = "wed", label = "Wed"),
    RoutineSelectableOption(id = "thu", label = "Thu"),
    RoutineSelectableOption(id = "fri", label = "Fri"),
    RoutineSelectableOption(id = "sat", label = "Sat"),
    RoutineSelectableOption(id = "sun", label = "Sun")
)

private val RoutineReminderOptions = listOf(
    RoutineSelectableOption(id = "morning", label = "Morning"),
    RoutineSelectableOption(id = "afternoon", label = "Afternoon"),
    RoutineSelectableOption(id = "evening", label = "Evening"),
    RoutineSelectableOption(id = "none", label = "No reminders")
)

@Composable
fun ProfileRoute(
    onHomeClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onProgressClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var activeScreen by rememberSaveable { mutableStateOf(ProfileScreen.Overview) }

    var selectedGoalId by remember { mutableStateOf(RoutineGoalOptions.first().id) }
    var selectedFrequencyId by remember { mutableStateOf("3_days") }
    var selectedDayIds by remember { mutableStateOf(setOf("wed")) }
    var selectedReminderId by remember { mutableStateOf("evening") }

    BackHandler(enabled = activeScreen != ProfileScreen.Overview) {
        activeScreen = ProfileScreen.Overview
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ProfileBackground
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val reservedBottomHeight = maxHeight * ProfileReservedBottomFraction

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = ProfileContentMaxWidth)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = ProfileHorizontalPadding,
                                end = ProfileHorizontalPadding,
                                top = ProfileTopPadding,
                                bottom = reservedBottomHeight
                            ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (activeScreen) {
                            ProfileScreen.Overview -> ProfileOverviewContent(
                                onYourRoutineClick = { activeScreen = ProfileScreen.YourRoutine }
                            )

                            ProfileScreen.YourRoutine -> RoutineSetupContent(
                                title = "Your routine",
                                subtitle = "Choose a plan that fits your week",
                                showBackButton = true,
                                onBackClick = { activeScreen = ProfileScreen.Overview },
                                selectedGoalId = selectedGoalId,
                                selectedFrequencyId = selectedFrequencyId,
                                selectedDayIds = selectedDayIds,
                                selectedReminderId = selectedReminderId,
                                onGoalSelected = { selectedGoalId = it },
                                onFrequencySelected = { selectedFrequencyId = it },
                                onDayToggled = { dayId ->
                                    selectedDayIds = if (selectedDayIds.contains(dayId)) {
                                        selectedDayIds - dayId
                                    } else {
                                        selectedDayIds + dayId
                                    }
                                },
                                onReminderSelected = { selectedReminderId = it },
                                onSaveRoutineClick = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Routine saved.")
                                    }
                                },
                                onSkipForNowClick = { activeScreen = ProfileScreen.Overview }
                            )
                        }
                    }
                }

                ProfileBottomNavigationBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onHomeClick = onHomeClick,
                    onWorkoutClick = onWorkoutClick,
                    onProgressClick = onProgressClick,
                    onProfileClick = {}
                )

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = reservedBottomHeight + 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileOverviewContent(
    onYourRoutineClick: () -> Unit
) {
    Text(
        text = "Profile",
        color = ProfilePrimaryText,
        fontSize = 34.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Settings and preferences",
        color = ProfileSecondaryText,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )

    SectionContainer {
        Text(
            text = "Personal setup",
            color = ProfilePrimaryText,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        ProfileMenuRow(
            title = "Your routine",
            subtitle = "Goals, days, and reminders",
            onClick = onYourRoutineClick
        )
    }
}

/**
 * Reusable routine setup content that can be embedded in Profile now and onboarding later.
 * Onboarding can customize the top title, back behavior, and action labels without layout changes.
 */
@Composable
fun RoutineSetupContent(
    title: String,
    subtitle: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    selectedGoalId: String,
    selectedFrequencyId: String,
    selectedDayIds: Set<String>,
    selectedReminderId: String,
    onGoalSelected: (String) -> Unit,
    onFrequencySelected: (String) -> Unit,
    onDayToggled: (String) -> Unit,
    onReminderSelected: (String) -> Unit,
    onSaveRoutineClick: () -> Unit,
    onSkipForNowClick: () -> Unit,
    saveActionLabel: String = "Save Routine",
    skipActionLabel: String = "Skip for now"
) {
    if (showBackButton) {
        BackRow(
            label = "Back",
            onClick = onBackClick
        )
    }

    Text(
        text = title,
        color = ProfilePrimaryText,
        fontSize = 34.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = subtitle,
        color = ProfileSecondaryText,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )

    SectionContainer {
        Text(
            text = "What are you mainly working on?",
            color = ProfilePrimaryText,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 420.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoutineGoalOptions.forEach { option ->
                        GoalSelectionCard(
                            title = option.title,
                            description = option.description,
                            placeholderSymbol = option.placeholderSymbol,
                            selected = selectedGoalId == option.id,
                            onClick = { onGoalSelected(option.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                val rows = RoutineGoalOptions.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    rows.forEach { rowOptions ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowOptions.forEach { option ->
                                GoalSelectionCard(
                                    title = option.title,
                                    description = option.description,
                                    placeholderSymbol = option.placeholderSymbol,
                                    selected = selectedGoalId == option.id,
                                    onClick = { onGoalSelected(option.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowOptions.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    SectionContainer {
        Text(
            text = "How often do you want to train?",
            color = ProfilePrimaryText,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoutineFrequencyOptions.take(3).forEach { option ->
                SelectionChip(
                    label = option.label,
                    selected = selectedFrequencyId == option.id,
                    onClick = { onFrequencySelected(option.id) },
                    modifier = Modifier.weight(1f),
                    role = Role.RadioButton
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SelectionChip(
            label = RoutineFrequencyOptions[3].label,
            selected = selectedFrequencyId == RoutineFrequencyOptions[3].id,
            onClick = { onFrequencySelected(RoutineFrequencyOptions[3].id) },
            role = Role.RadioButton
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Choose days for workouts",
            color = ProfileSecondaryText,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoutineDayOptions.take(4).forEach { option ->
                SelectionChip(
                    label = option.label,
                    selected = selectedDayIds.contains(option.id),
                    onClick = { onDayToggled(option.id) },
                    modifier = Modifier.weight(1f),
                    role = Role.Checkbox
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoutineDayOptions.drop(4).forEach { option ->
                SelectionChip(
                    label = option.label,
                    selected = selectedDayIds.contains(option.id),
                    onClick = { onDayToggled(option.id) },
                    modifier = Modifier.weight(1f),
                    role = Role.Checkbox
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You can change this later",
            color = ProfileSecondaryText,
            fontSize = 13.sp,
            lineHeight = 16.sp
        )
    }

    SectionContainer {
        Text(
            text = "When should we remind you?",
            color = ProfilePrimaryText,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoutineReminderOptions.forEach { option ->
                SelectionChip(
                    label = option.label,
                    selected = selectedReminderId == option.id,
                    onClick = { onReminderSelected(option.id) },
                    modifier = Modifier.weight(1f),
                    role = Role.RadioButton
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "We'll send at most one reminder on your planned days",
            color = ProfileSecondaryText,
            fontSize = 13.sp,
            lineHeight = 16.sp
        )
    }

    PrimaryActionButton(
        label = saveActionLabel,
        onClick = onSaveRoutineClick
    )
    TextButton(
        onClick = onSkipForNowClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = skipActionLabel,
            color = ProfileSecondaryText,
            fontSize = 18.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BackRow(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            color = ProfilePrimaryText,
            fontSize = 22.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = ProfileSecondaryText,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProfileMenuRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ProfileCardBackground.copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = ProfileSecondaryText.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = ProfilePrimaryText,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = ProfileSecondaryText,
                fontSize = 13.sp,
                lineHeight = 16.sp
            )
        }

        Image(
            painter = painterResource(id = R.drawable.home_icon_chevron),
            contentDescription = "Open",
            modifier = Modifier
                .width(16.dp)
                .height(16.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun GoalSelectionCard(
    title: String,
    description: String,
    placeholderSymbol: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) ProfileAccent else ProfileSecondaryText.copy(alpha = 0.18f)
    val cardColor = if (selected) Color(0xFFF8E8DE) else Color(0x33FFFFFF)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .background(cardColor)
            .clickable(onClick = onClick)
            .semantics {
                this.role = Role.RadioButton
                this.selected = selected
            }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GoalIconPlaceholder(symbol = placeholderSymbol)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = ProfilePrimaryText,
            fontSize = 18.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            color = ProfileSecondaryText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (selected) "Selected" else "Tap to select",
            color = ProfileSecondaryText,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GoalIconPlaceholder(symbol: String) {
    // Placeholder for goal icons; replace with final assets while keeping this fixed-size frame.
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(ProfileCardBackground)
            .border(
                width = 1.dp,
                color = ProfileSecondaryText.copy(alpha = 0.24f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = ProfileSecondaryText,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SelectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ProfileAccent else Color(0xFFF1D3C8))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) ProfilePrimaryText.copy(alpha = 0.34f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .semantics {
                this.role = role
                this.selected = selected
            }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFFFFF2E9) else ProfilePrimaryText,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PrimaryActionButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = ProfileAccent,
            contentColor = Color(0xFFFFF2E9)
        )
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SectionContainer(
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ProfileCardBackground)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content
        )
    }
}

@Composable
private fun ProfileBottomNavigationBar(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit,
    onWorkoutClick: () -> Unit,
    onProgressClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = ProfileBottomInsetCorner, topEnd = ProfileBottomInsetCorner))
            .background(ProfileBottomBarBg)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileBottomNavItem(
                    label = "Home",
                    icon = R.drawable.home_nav_home,
                    textColor = ProfileInactiveIcon,
                    onClick = onHomeClick
                )
                ProfileBottomNavItem(
                    label = "Workout",
                    icon = R.drawable.home_nav_workout,
                    textColor = ProfileInactiveIcon,
                    onClick = onWorkoutClick
                )
                ProfileBottomNavItem(
                    label = "Progress",
                    icon = R.drawable.home_nav_progress,
                    textColor = ProfileInactiveIcon,
                    onClick = onProgressClick
                )
                ProfileBottomNavItem(
                    label = "Profile",
                    icon = R.drawable.home_nav_profile,
                    textColor = ProfilePrimaryText,
                    onClick = onProfileClick
                )
            }
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun ProfileBottomNavItem(
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

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun ProfileOverviewPreview() {
    MaterialTheme {
        ProfileRoute()
    }
}
