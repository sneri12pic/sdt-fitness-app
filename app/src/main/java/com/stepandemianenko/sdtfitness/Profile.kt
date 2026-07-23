package com.stepandemianenko.sdtfitness

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.stepandemianenko.sdtfitness.auth.ui.AuthGateActivity
import com.stepandemianenko.sdtfitness.BuildConfig
import com.stepandemianenko.sdtfitness.data.AppGraph
import com.stepandemianenko.sdtfitness.profile.ProfileOverview
import com.stepandemianenko.sdtfitness.profile.ProfileStats
import com.stepandemianenko.sdtfitness.profile.ProfileUiEvent
import com.stepandemianenko.sdtfitness.profile.ProfileViewModel
import com.stepandemianenko.sdtfitness.profile.QuestBarPoint
import com.stepandemianenko.sdtfitness.profile.QuestChartRange
import com.stepandemianenko.sdtfitness.profile.ReminderTimeTarget
import com.stepandemianenko.sdtfitness.profile.RoutineSettings
import com.stepandemianenko.sdtfitness.profile.parseReminderTime
import com.stepandemianenko.sdtfitness.ui.components.GlassBottomNav
import com.stepandemianenko.sdtfitness.ui.components.NavTab
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image

class Profile : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ProfileRoute(
                    onHomeClick = { openHomeWithoutAnimation() },
                    onWorkoutClick = { openWorkoutWithoutAnimation() },
                    onProgressClick = { openProgressWithoutAnimation() },
                    onSignOutClick = { signOutAndOpenAuthGate() }
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

    private fun signOutAndOpenAuthGate() {
        lifecycleScope.launch {
            AppGraph.authRepository(this@Profile).signOut()
            val intent = Intent(this@Profile, AuthGateActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }
}

private val ProfileBackground = Color(0xFFEBC0B0)

private val BottomBarBg = Color(0xFFF5E5DA)
private val ProfileCardBackground = Color(0xFFF5E5DA)
private val ProfilePrimaryText = Color(0xFF4F2912)
private val ProfileSecondaryText = Color(0xFF6B4637)
private val ProfileAccent = Color(0xFFF27F3E)
private val ProfileDanger = Color(0xFFC62828)
private val ProfileBottomBarBg = Color(0xFFF5E5DA)
private val ProfileInactiveIcon = Color(0xFFC48778)
// Warm, on-brand tints for the squircle/circle icon tiles (Apple-style grouped rows).
private val TileCoral = Color(0xFFF27F3E)
private val TileGold = Color(0xFFF2A93E)
private val TileGreen = Color(0xFF69C47A)
private val TileAqua = Color(0xFF3FB6DE)
private val TileIndigo = Color(0xFF8A6BC4)
// Hairline + soft-shadow tints derived from the brown text, so depth reads warm not grey.
private val CardHairline = Color(0x1A6B4637)
private val CardShadow = Color(0x33512E1C)
// Per-quest bar colors: Water reuses the water-jar aqua; the other two are picked to stay distinct.
private val QuestColorWeightIn = Color(0xFFF27F3E) // coral, the app accent
private val QuestColorCreatine = Color(0xFF69C47A) // green, same as Home's SoftGreen
private val QuestColorWater = Color(0xFF3FB6DE)     // aqua, from the water jar fill
private val ProfileContentMaxWidth = 360.dp

private fun questBarColor(label: String): Color = when (label) {
    "Creatine" -> QuestColorCreatine
    "Water" -> QuestColorWater
    else -> QuestColorWeightIn
}
private const val ProfileReservedBottomFraction = 0.15f
private val ProfileHorizontalPadding = 20.dp
private val ProfileTopPadding = 30.dp
private val ProfileBottomInsetCorner = 16.dp

private enum class ProfileScreen {
    Overview,
    YourRoutine,
    HealthConnect
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

private data class ReminderPreset(
    val time: String,
    val label: String
)

private val ReminderPresets = listOf(
    ReminderPreset(time = "07:00", label = "Morning"),
    ReminderPreset(time = "12:00", label = "Midday"),
    ReminderPreset(time = "18:00", label = "Evening"),
    ReminderPreset(time = "21:00", label = "Night")
)

private fun reminderPresetLabel(time: String): String {
    return ReminderPresets.firstOrNull { it.time == time }?.label ?: "Reminder"
}

private fun displayTime(time: String): String {
    val parsed = parseReminderTime(time) ?: return time
    val (hour, minute) = parsed
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return displayHour.toString() + ":" + minute.toString().padStart(2, '0') + " " + period
}

@Composable
fun ProfileRoute(
    onHomeClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}
    var activeScreen by rememberSaveable { mutableStateOf(ProfileScreen.Overview) }

    var isSettingsDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isSignOutDialogOpen by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = activeScreen != ProfileScreen.Overview) {
        activeScreen = ProfileScreen.Overview
    }

    LaunchedEffect(uiState.saveMessage) {
        val message = uiState.saveMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(ProfileUiEvent.ClearSaveMessage)
        }
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
                                overview = uiState.overview,
                                remindersEnabled = uiState.routine.reminderEnabled,
                                onYourRoutineClick = { activeScreen = ProfileScreen.YourRoutine },
                                onToggleReminders = {
                                    // ponytail: flips the saved routine's reminder flag in place; saves the
                                    // whole draft, fine since overview has no other pending edits.
                                    viewModel.onEvent(ProfileUiEvent.ToggleRemindersEnabled)
                                    viewModel.onEvent(ProfileUiEvent.SaveRoutine)
                                },
                                onSettingsClick = { isSettingsDialogOpen = true },
                                onSignOutClick = { isSignOutDialogOpen = true },
                                onQuestsClick = { viewModel.onEvent(ProfileUiEvent.OpenQuestChart) },
                                onHealthConnectClick = { activeScreen = ProfileScreen.HealthConnect }
                            )

                            ProfileScreen.YourRoutine -> RoutineSetupContent(
                                title = "Your routine",
                                subtitle = "Shape a plan that fits your life — you're free to change it anytime",
                                showBackButton = true,
                                onBackClick = { activeScreen = ProfileScreen.Overview },
                                routine = uiState.draftRoutine,
                                onGoalSelected = { viewModel.onEvent(ProfileUiEvent.SelectGoal(it)) },
                                onFrequencySelected = { viewModel.onEvent(ProfileUiEvent.SelectFrequency(it)) },
                                onDayToggled = { viewModel.onEvent(ProfileUiEvent.ToggleDay(it)) },
                                onRemindersEnabledToggled = { viewModel.onEvent(ProfileUiEvent.ToggleRemindersEnabled) },
                                onPresetReminderToggled = { viewModel.onEvent(ProfileUiEvent.TogglePresetReminder(it)) },
                                onPresetReminderLongPressed = { viewModel.onEvent(ProfileUiEvent.OpenPresetTimePicker(it)) },
                                onAddCustomReminderClick = { viewModel.onEvent(ProfileUiEvent.OpenCustomTimePicker) },
                                onRemoveCustomReminder = { viewModel.onEvent(ProfileUiEvent.RemoveCustomReminder(it)) },
                                onSaveRoutineClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        uiState.draftRoutine.reminderEnabled &&
                                        uiState.draftRoutine.allReminderTimes.isNotEmpty() &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.onEvent(ProfileUiEvent.SaveRoutine)
                                },
                                onSkipForNowClick = { activeScreen = ProfileScreen.Overview }
                            )

                            ProfileScreen.HealthConnect -> HealthConnectScreen(
                                onBackClick = { activeScreen = ProfileScreen.Overview }
                            )
                        }
                    }
                }

                GlassBottomNav(
                    active = NavTab.PROFILE,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onHomeClick = onHomeClick,
                    onWorkoutClick = onWorkoutClick,
                    onProgressClick = onProgressClick
                )

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = reservedBottomHeight + 8.dp)
                )
            }

            if (isSettingsDialogOpen) {
                ProfileSettingsDialog(
                    onDismiss = { isSettingsDialogOpen = false },
                    onSignOutClick = {
                        isSettingsDialogOpen = false
                        isSignOutDialogOpen = true
                    }
                )
            }

            if (isSignOutDialogOpen) {
                SignOutConfirmationDialog(
                    onDismiss = { isSignOutDialogOpen = false },
                    onConfirm = {
                        isSignOutDialogOpen = false
                        onSignOutClick()
                    }
                )
            }

            if (uiState.isTimePickerOpen) {
                ReminderTimePickerDialog(
                    initialHour = uiState.timePickerInitialHour,
                    initialMinute = uiState.timePickerInitialMinute,
                    isCustom = uiState.timePickerTarget is ReminderTimeTarget.Custom,
                    onDismiss = { viewModel.onEvent(ProfileUiEvent.DismissTimePicker) },
                    onConfirm = { hour, minute ->
                        viewModel.onEvent(ProfileUiEvent.SavePickedTime(hour = hour, minute = minute))
                    }
                )
            }

            if (uiState.isQuestChartOpen) {
                QuestChartDialog(
                    range = uiState.questChartRange,
                    points = uiState.questChart,
                    onRangeSelected = { viewModel.onEvent(ProfileUiEvent.SelectQuestChartRange(it)) },
                    onDismiss = { viewModel.onEvent(ProfileUiEvent.DismissQuestChart) }
                )
            }
        }
    }
}

@Composable
private fun ProfileOverviewContent(
    overview: ProfileOverview,
    remindersEnabled: Boolean,
    onYourRoutineClick: () -> Unit,
    onToggleReminders: () -> Unit,
    onSettingsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onQuestsClick: () -> Unit,
    onHealthConnectClick: () -> Unit
) {
    // Large iOS-style title with a soft settings button.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Profile",
                color = ProfilePrimaryText,
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage your account & preferences",
                color = ProfileSecondaryText,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(2.dp, CircleShape, clip = false, ambientColor = CardShadow, spotColor = CardShadow)
                .clip(CircleShape)
                .background(ProfileCardBackground)
                .clickable(onClick = onSettingsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Profile settings",
                tint = ProfilePrimaryText,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    ProfileHeroCard(
        name = overview.displayName,
        isGuest = overview.isGuest
    )

    AchievementStrip(
        stats = overview.stats,
        onQuestsClick = onQuestsClick
    )

    SettingsSection(title = "Personal") {
        SettingsRow(
            icon = Icons.Rounded.CalendarMonth,
            tint = TileCoral,
            title = "Your routine",
            subtitle = "Goals, days & reminders",
            onClick = onYourRoutineClick
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Notifications,
            tint = TileGold,
            title = "Reminders",
            subtitle = "Nudges on training days",
            showChevron = false,
            onClick = onToggleReminders,
            trailing = {
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { onToggleReminders() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ProfileAccent,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = ProfileSecondaryText.copy(alpha = 0.35f)
                    )
                )
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.MonitorHeart,
            tint = TileGreen,
            title = "Health Connect",
            subtitle = "Sync steps & weight",
            onClick = onHealthConnectClick
        )
    }

    SettingsSection(title = "Preferences") {
        SettingsRow(
            icon = Icons.Rounded.Straighten,
            tint = TileAqua,
            title = "Units",
            valueText = "Kilograms",
            // ponytail: kg is the only unit today; wire onClick when lb conversion exists.
            onClick = {}
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.DarkMode,
            tint = TileIndigo,
            title = "Appearance",
            valueText = "System",
            // ponytail: single warm theme for now; wire onClick when theming lands.
            onClick = {}
        )
    }

    SettingsSection(title = "Account") {
        SettingsRow(
            icon = Icons.Rounded.Person,
            tint = TileCoral,
            title = "Edit profile",
            subtitle = "Name & avatar",
            // ponytail: no profile-edit screen yet; inert until it exists.
            onClick = {}
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Logout,
            tint = ProfileDanger,
            title = "Sign out",
            destructive = true,
            showChevron = false,
            onClick = onSignOutClick
        )
    }

    // ponytail: Support rows are structural placeholders — wire each onClick to its
    // real destination (browser/mailer) when those exist.
    SettingsSection(title = "Support") {
        SettingsRow(
            icon = Icons.Rounded.HelpOutline,
            tint = TileCoral,
            title = "Help & FAQ",
            onClick = {}
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.ChatBubbleOutline,
            tint = TileGreen,
            title = "Send feedback",
            onClick = {}
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Shield,
            tint = TileIndigo,
            title = "Privacy policy",
            onClick = {}
        )
    }

    AppVersionFooter()
}

/**
 * Identity banner: a gradient-ringed avatar, the account name, and a status pill.
 * Purely informational — editing lives in the Account section below.
 */
@Composable
private fun ProfileHeroCard(
    name: String,
    isGuest: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp), clip = false, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(ProfileCardBackground, lerp(ProfileCardBackground, ProfileAccent, 0.14f))
                )
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(ProfileAccent, TileGold))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.profile_avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(ProfileCardBackground)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = ProfilePrimaryText,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ProfileAccent.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isGuest) Icons.Rounded.Person else Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = ProfileAccent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isGuest) "Guest account" else "Member · Free",
                    color = ProfileAccent,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Three tappable stat tiles (workouts / streak / quests) with soft-tinted vector icons. */
@Composable
private fun AchievementStrip(
    stats: ProfileStats,
    onQuestsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AchievementTile(
            icon = Icons.Rounded.FitnessCenter,
            tint = TileCoral,
            value = stats.workouts,
            label = "Workouts",
            modifier = Modifier.weight(1f)
        )
        AchievementTile(
            icon = Icons.Rounded.LocalFireDepartment,
            tint = TileGold,
            value = stats.streakDays,
            label = "Day streak",
            modifier = Modifier.weight(1f)
        )
        AchievementTile(
            icon = Icons.Rounded.EmojiEvents,
            tint = TileGreen,
            value = stats.questsDone,
            label = "Quests",
            modifier = Modifier.weight(1f),
            onClick = onQuestsClick
        )
    }
}

@Composable
private fun AchievementTile(
    icon: ImageVector,
    tint: Color,
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .shadow(3.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(RoundedCornerShape(18.dp))
            .background(ProfileCardBackground)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value.toString(),
            color = ProfilePrimaryText,
            fontSize = 22.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = ProfileSecondaryText,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A titled block: small uppercase header tight above one grouped inset card. */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.uppercase(),
            color = ProfileSecondaryText.copy(alpha = 0.75f),
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 6.dp, top = 4.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = CardShadow, spotColor = CardShadow)
                .clip(RoundedCornerShape(18.dp))
                .background(ProfileCardBackground),
            content = content
        )
    }
}

/** Hairline between rows, inset past the icon so it aligns iOS-style with the text. */
@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp)
            .height(1.dp)
            .background(CardHairline)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    valueText: String? = null,
    destructive: Boolean = false,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(icon = icon, tint = if (destructive) ProfileDanger else tint)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (destructive) ProfileDanger else ProfilePrimaryText,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = ProfileSecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            if (valueText != null) {
                Text(
                    text = valueText,
                    color = ProfileSecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                )
            }
            if (showChevron) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = ProfileSecondaryText.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** iOS-style rounded-square icon chip: solid tint with a white glyph. */
@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(tint),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AppVersionFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SDT Fitness",
            color = ProfileSecondaryText.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Version " + BuildConfig.VERSION_NAME,
            color = ProfileSecondaryText.copy(alpha = 0.6f),
            fontSize = 12.sp,
            lineHeight = 14.sp
        )
    }
}

/**
 * Quest-completion chart popup. Same shape as Progress' steps dialog, but the bars are scaled up
 * (taller box, wider columns) and a range dropdown re-queries Week / Month / 3 Months / Year.
 */
@Composable
private fun QuestChartDialog(
    range: QuestChartRange,
    points: List<QuestBarPoint>,
    onRangeSelected: (QuestChartRange) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ProfileCardBackground,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quests done",
                    color = ProfilePrimaryText,
                    fontWeight = FontWeight.Bold
                )
                QuestRangeDropdown(range = range, onRangeSelected = onRangeSelected)
            }
        },
        text = { QuestBarChart(points = points) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = ProfilePrimaryText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun QuestRangeDropdown(
    range: QuestChartRange,
    onRangeSelected: (QuestChartRange) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(ProfileAccent.copy(alpha = 0.16f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = range.label,
                color = ProfileAccent,
                fontSize = 13.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "▾", color = ProfileAccent, fontSize = 12.sp, lineHeight = 12.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ProfileCardBackground)
        ) {
            QuestChartRange.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, color = ProfilePrimaryText) },
                    onClick = {
                        onRangeSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun QuestBarChart(points: List<QuestBarPoint>) {
    val safe = points.ifEmpty {
        listOf(
            QuestBarPoint("Weight-In", 0),
            QuestBarPoint("Creatine", 0),
            QuestBarPoint("Water", 0)
        )
    }
    val maxCount = safe.maxOf { it.count }.coerceAtLeast(1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(ProfileBackground.copy(alpha = 0.35f), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            safe.forEachIndexed { index, point ->
                QuestBar(
                    modifier = Modifier.weight(1f),
                    point = point,
                    maxCount = maxCount,
                    barColor = questBarColor(point.label),
                    index = index
                )
            }
        }
    }
}

@Composable
private fun QuestBar(
    modifier: Modifier,
    point: QuestBarPoint,
    maxCount: Int,
    barColor: Color,
    index: Int
) {
    val target = (point.count.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
    // Grow each bar in from the floor, staggered left-to-right.
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val fraction by animateFloatAsState(
        targetValue = if (started) target else 0f,
        animationSpec = tween(durationMillis = 650, delayMillis = index * 90, easing = FastOutSlowInEasing),
        label = "questBar"
    )
    val barBrush = Brush.verticalGradient(
        colors = listOf(lerp(barColor, Color.White, 0.35f), barColor)
    )

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(barColor.copy(alpha = 0.16f))
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text(
                text = point.count.toString(),
                color = barColor,
                fontSize = 13.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 10.dp)
                .fillMaxWidth(0.62f)
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.4f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    // Round only the top; the track's clip rounds the base, so the bar sits flush.
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(barBrush)
            )
        }
        Text(
            text = point.label,
            color = ProfileSecondaryText,
            fontSize = 12.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ProfileSettingsDialog(
    onDismiss: () -> Unit,
    onSignOutClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ProfileCardBackground,
        title = {
            Text(
                text = "Settings",
                color = ProfilePrimaryText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Account",
                    color = ProfileSecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = onSignOutClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProfileDanger,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Sign Out",
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ProfileSecondaryText)
            }
        }
    )
}

@Composable
private fun SignOutConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ProfileCardBackground,
        title = {
            Text(
                text = "Sign out?",
                color = ProfilePrimaryText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "You can sign back in anytime. Local guest data will stay on this device.",
                color = ProfileSecondaryText
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Sign Out",
                    color = ProfileDanger,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ProfileSecondaryText)
            }
        }
    )
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
    routine: RoutineSettings,
    onGoalSelected: (String) -> Unit,
    onFrequencySelected: (String) -> Unit,
    onDayToggled: (String) -> Unit,
    onRemindersEnabledToggled: () -> Unit,
    onPresetReminderToggled: (String) -> Unit,
    onPresetReminderLongPressed: (String) -> Unit,
    onAddCustomReminderClick: () -> Unit,
    onRemoveCustomReminder: (String) -> Unit,
    onSaveRoutineClick: () -> Unit,
    onSkipForNowClick: () -> Unit,
    saveActionLabel: String = "Save Routine",
    skipActionLabel: String = "Skip for now"
) {
    val selectedGoalId = routine.goalId
    val selectedFrequencyId = routine.frequencyId
    val selectedDayIds = routine.dayIds
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Want a gentle nudge?",
                    color = ProfilePrimaryText,
                    fontSize = 22.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reminders are entirely optional — turn them on or off whenever it suits you",
                    color = ProfileSecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }
            Switch(
                checked = routine.reminderEnabled,
                onCheckedChange = { onRemindersEnabledToggled() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFF2E9),
                    checkedTrackColor = ProfileAccent,
                    uncheckedTrackColor = Color(0xFFF1D3C8)
                )
            )
        }

        if (routine.reminderEnabled) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Pick the times that work for you",
                color = ProfileSecondaryText,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            val displayedPresetTimes = (ReminderPresets.map { it.time } + routine.reminderTimes)
                .distinct()
                .sorted()
            displayedPresetTimes.chunked(2).forEach { rowTimes ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowTimes.forEach { time ->
                        ReminderTimeChip(
                            label = reminderPresetLabel(time),
                            timeText = displayTime(time),
                            selected = routine.reminderTimes.contains(time),
                            onClick = { onPresetReminderToggled(time) },
                            onLongClick = { onPresetReminderLongPressed(time) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowTimes.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = "Tap a time to use it, or hold it down to set your own",
                color = ProfileSecondaryText,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your own custom times",
                color = ProfileSecondaryText,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (routine.customReminderTimes.isEmpty()) {
                Text(
                    text = "No custom times yet — add one if the presets don't fit your day",
                    color = ProfileSecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    routine.customReminderTimes.forEach { time ->
                        CustomReminderRow(
                            timeText = displayTime(time),
                            onRemoveClick = { onRemoveCustomReminder(time) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            AddCustomReminderButton(onClick = onAddCustomReminderClick)

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "We'll send at most one reminder on your planned days — you're always in control",
                color = ProfileSecondaryText,
                fontSize = 13.sp,
                lineHeight = 16.sp
            )
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReminderTimeChip(
    label: String,
    timeText: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ProfileAccent else Color(0xFFF1D3C8))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) ProfilePrimaryText.copy(alpha = 0.34f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics {
                this.role = Role.Checkbox
                this.selected = selected
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFFFFF2E9) else ProfilePrimaryText,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = timeText,
            color = if (selected) Color(0xFFFFF2E9) else ProfileSecondaryText,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CustomReminderRow(
    timeText: String,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1D3C8))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timeText,
            color = ProfilePrimaryText,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onRemoveClick) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove this reminder time",
                tint = ProfileSecondaryText
            )
        }
    }
}

@Composable
private fun AddCustomReminderButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = ProfileAccent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = ProfileAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Add a time that works for you",
            color = ProfileAccent,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    isCustom: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by rememberSaveable(initialHour) { mutableStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by rememberSaveable(initialMinute) { mutableStateOf(initialMinute.coerceIn(0, 59)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ProfileCardBackground,
        title = {
            Text(
                text = if (isCustom) "Add your own reminder time" else "Set this reminder's time",
                color = ProfilePrimaryText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Scroll to choose a time that fits — it's your call",
                    color = ProfileSecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TimePickerColumn(
                        title = "Hour",
                        values = (0..23).toList(),
                        selectedValue = selectedHour,
                        valueFormatter = { it.toString().padStart(2, '0') },
                        onValueSelected = { selectedHour = it },
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerColumn(
                        title = "Minute",
                        values = (0..59).toList(),
                        selectedValue = selectedMinute,
                        valueFormatter = { it.toString().padStart(2, '0') },
                        onValueSelected = { selectedMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                Text(
                    text = "Use this time",
                    color = ProfileAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ProfileSecondaryText)
            }
        }
    )
}

@Composable
private fun TimePickerColumn(
    title: String,
    values: List<Int>,
    selectedValue: Int,
    valueFormatter: (Int) -> String,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = ProfileSecondaryText,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFEFE5)),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(values) { value ->
                val selected = value == selectedValue
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) ProfileAccent else Color.Transparent)
                        .clickable { onValueSelected(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = valueFormatter(value),
                        color = if (selected) Color(0xFFFFF2E9) else ProfilePrimaryText,
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
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

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun ProfileOverviewPreview() {
    MaterialTheme {
        ProfileRoute()
    }
}
