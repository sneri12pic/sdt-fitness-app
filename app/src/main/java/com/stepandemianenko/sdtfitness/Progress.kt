package com.stepandemianenko.sdtfitness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepandemianenko.sdtfitness.home.DailyStepsSourceType
import java.util.Locale

class Progress : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ProgressRoute(
                    onHomeClick = { openHomeWithoutAnimation() },
                    onWorkoutClick = { openWorkoutWithoutAnimation() }
                )
            }
        }
    }

    private fun openHomeWithoutAnimation() {
        startActivity(Intent(this, Home::class.java))
        overridePendingTransition(0, 0)
    }

    private fun openWorkoutWithoutAnimation() {
        startActivity(Intent(this, StartWorkout::class.java))
        overridePendingTransition(0, 0)
    }
}

private val ProgressBackground = Color(0xFFEBC0B0)
private val ProgressCardBackground = Color(0xFFF5E5DA)
private val ProgressTileBackground = Color(0xFFF1D5CB)
private val ProgressPrimaryText = Color(0xFF4F2912)
private val ProgressSecondaryText = Color(0xFF6B4637)
private val ProgressPositive = Color(0xFF69C47A)
private val ProgressAccent = Color(0xFFF08A67)
private val ProgressBadgeBg = Color(0xFFEBCBC0)
private val ProgressDivider = Color(0x40A67B6C)
private val ProgressBottomBarBg = Color(0xFFF5E5DA)
private val ProgressInactiveIcon = Color(0xFFC48778)
private val ProgressContentMaxWidth = 360.dp
private const val ProgressReservedBottomFraction = 0.15f
private const val HealthConnectProviderPackageName = "com.google.android.apps.healthdata"
private val ProgressHorizontalPadding = 20.dp
private val ProgressTopPadding = 30.dp
private val ProgressBottomInsetCorner = 16.dp

@Composable
fun ProgressRoute(
    onHomeClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onCommunityClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: ProgressViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(
            HealthConnectProviderPackageName
        )
    ) { grantedPermissions ->
        viewModel.onHealthConnectPermissionsResult(grantedPermissions)
    }

    val openHealthConnectSettings: () -> Unit = {
        runCatching {
            val manageDataIntent = HealthConnectClient.getHealthConnectManageDataIntent(
                context,
                HealthConnectProviderPackageName
            )
            context.startActivity(manageDataIntent)
        }.onFailure {
            runCatching {
                context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
            }.onFailure {
                viewModel.setHealthConnectError("Couldn't open Health Connect settings on this device.")
            }
        }
        Unit
    }

    ProgressScreen(
        uiState = uiState,
        onConnectHealthConnectClick = {
            runCatching {
                permissionsLauncher.launch(viewModel.requiredHealthConnectPermissions())
            }.onFailure {
                openHealthConnectSettings()
            }
        },
        onOpenHealthConnectSettingsClick = openHealthConnectSettings,
        onRefreshHealthConnectClick = viewModel::refreshHealthConnectData,
        onHomeClick = onHomeClick,
        onWorkoutClick = onWorkoutClick,
        onCommunityClick = onCommunityClick,
        onProfileClick = onProfileClick
    )
}

@Composable
fun ProgressScreen(
    uiState: ProgressUiState = ProgressUiState(isLoading = false),
    onConnectHealthConnectClick: () -> Unit = {},
    onOpenHealthConnectSettingsClick: () -> Unit = {},
    onRefreshHealthConnectClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onCommunityClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ProgressBackground
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val reservedBottomHeight = maxHeight * ProgressReservedBottomFraction

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = ProgressContentMaxWidth)
                        .fillMaxSize()
                ) {
                    ProgressContent(
                        uiState = uiState,
                        onConnectHealthConnectClick = onConnectHealthConnectClick,
                        onOpenHealthConnectSettingsClick = onOpenHealthConnectSettingsClick,
                        onRefreshHealthConnectClick = onRefreshHealthConnectClick,
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = ProgressHorizontalPadding,
                                end = ProgressHorizontalPadding,
                                top = ProgressTopPadding,
                                bottom = reservedBottomHeight
                            )
                    )
                }

                ProgressBottomNavigationBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onHomeClick = onHomeClick,
                    onWorkoutClick = onWorkoutClick,
                    onCommunityClick = onCommunityClick,
                    onProfileClick = onProfileClick
                )
            }
        }
    }
}

@Composable
private fun ProgressContent(
    uiState: ProgressUiState,
    onConnectHealthConnectClick: () -> Unit,
    onOpenHealthConnectSettingsClick: () -> Unit,
    onRefreshHealthConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProgressHeaderSection()
        if (uiState.isLoading) {
            Text(
                text = "Loading completed workout data...",
                color = ProgressSecondaryText,
                fontSize = 14.sp,
                lineHeight = 16.sp
            )
        }
        SectionTitle(title = "Consistency")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ConsistencyCard(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.start_workout_streak_icon,
                title = uiState.consistencyTitle,
                subtitle = uiState.consistencySubtitle,
                badge = "Completed"
            )
            ConsistencyCard(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.calendar_streak_consistency,
                title = "${uiState.workoutDays} Workout Days",
                subtitle = uiState.streakSubtitle
            )
        }

        SectionTitle(title = "Mastery")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MasteryCard(
                    modifier = Modifier.weight(1f),
                    title = "Best Lift",
                    value = uiState.bestLiftValue,
                    subtitle = uiState.bestLiftSubtitle,
                    badge = "Completed",
                    iconRes = R.drawable.trophy_star
                )
                MasteryCard(
                    modifier = Modifier.weight(1f),
                    title = "Volume",
                    value = uiState.volumeValue,
                    subtitle = uiState.volumeSubtitle,
                    badge = "Completed",
                    iconRes = R.drawable.medal
                )
            }
            MasteryCard(
                title = "Session Load",
                value = uiState.personalBestsValue,
                subtitle = uiState.personalBestsSubtitle,
                iconRes = R.drawable.medal
            )
        }

        SectionTitle(title = "Health Connect")
        HealthConnectCard(
            uiState = uiState,
            onConnectClick = onConnectHealthConnectClick,
            onOpenSettingsClick = onOpenHealthConnectSettingsClick,
            onRefreshClick = onRefreshHealthConnectClick
        )

        val hasImportedWeight = uiState.isHealthConnectAvailable &&
            uiState.isHealthConnectPermissionGranted &&
            !uiState.isHealthConnectLoading &&
            uiState.healthConnectError == null

        if (hasImportedWeight) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutcomeCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.home_shoes,
                    title = "Steps",
                    value = "%,d".format(uiState.displayedTodaySteps),
                    subtitle = "Today",
                    badge = formatStepsSourceLabel(uiState.displayedStepsSourceType)
                )
                OutcomeCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.orange_scales,
                    title = "Weight",
                    value = formatWeightKg(uiState.importedLatestWeightKg),
                    subtitle = "Latest",
                    badge = "Imported"
                )
            }
        } else {
            OutcomeCard(
                iconRes = R.drawable.home_shoes,
                title = "Steps",
                value = "%,d".format(uiState.displayedTodaySteps),
                subtitle = "Today",
                badge = formatStepsSourceLabel(uiState.displayedStepsSourceType)
            )
        }

        SectionTitle(title = "Achievements")
        AchievementsCard(
            items = listOf(
                AchievementItem(R.drawable.trophy_star, "${uiState.completedSessions} sessions completed"),
                AchievementItem(
                    R.drawable.calendar_streak_consistency,
                    if (uiState.streakDays > 0) "${uiState.streakDays}-day routine streak" else "Start your first workout streak"
                ),
                AchievementItem(R.drawable.trend, "Top volume: ${uiState.topExerciseText}")
            )
        )
    }
}

@Composable
private fun ProgressHeaderSection() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Progress Overview",
            color = ProgressPrimaryText,
            fontSize = 40.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Consistency, mastery, and outcomes in one place",
            color = ProgressSecondaryText,
            fontSize = 16.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = ProgressPrimaryText,
        fontSize = 22.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun HealthConnectCard(
    uiState: ProgressUiState,
    onConnectClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val status = when {
        uiState.healthConnectError != null -> "Error"
        !uiState.isHealthConnectAvailable -> "Not available"
        !uiState.isHealthConnectPermissionGranted -> "Permission required"
        else -> "Connected"
    }

    ProgressCard(minHeight = 140.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Status: $status",
                color = ProgressPrimaryText,
                fontSize = 15.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            val message = when {
                uiState.healthConnectError != null -> uiState.healthConnectError
                !uiState.isHealthConnectAvailable -> "Health Connect is unavailable on this device."
                !uiState.isHealthConnectPermissionGranted -> "Allow Steps and Weight permissions to import your data."
                uiState.isHealthConnectLoading -> "Importing latest data..."
                else -> {
                    val steps = "%,d".format(uiState.importedTodaySteps ?: 0L)
                    val weight = formatWeightKg(uiState.importedLatestWeightKg)
                    "Imported today: $steps steps, latest weight: $weight"
                }
            }

            Text(
                text = message,
                color = ProgressSecondaryText,
                fontSize = 14.sp,
                lineHeight = 18.sp
            )

            if (uiState.isHealthConnectAvailable && !uiState.isHealthConnectPermissionGranted) {
                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProgressAccent,
                        contentColor = Color(0xFFFDEDE7)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Connect Health Connect",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!uiState.isHealthConnectAvailable ||
                !uiState.isHealthConnectPermissionGranted ||
                uiState.healthConnectError != null
            ) {
                Button(
                    onClick = onOpenSettingsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProgressTileBackground,
                        contentColor = ProgressPrimaryText
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Manage access",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (uiState.isHealthConnectAvailable && uiState.isHealthConnectPermissionGranted) {
                Button(
                    onClick = onRefreshClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProgressTileBackground,
                        contentColor = ProgressPrimaryText
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Refresh import",
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsistencyCard(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    subtitle: String,
    badge: String? = null
) {
    ProgressCard(
        modifier = modifier,
        minHeight = 112.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconTile(iconRes = iconRes, tileSize = 60.dp, iconSize = 34.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = title, color = ProgressPrimaryText, fontSize = 13.sp, lineHeight = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = subtitle,
                    color = if (subtitle.startsWith("+")) ProgressPositive else ProgressSecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 13.sp
                )
                if (badge != null) {
                    BadgeChip(text = badge)
                }
            }
        }
    }
}

@Composable
private fun MasteryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    badge: String? = null,
    iconRes: Int
) {
    ProgressCard(
        modifier = modifier,
        minHeight = 146.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = ProgressPrimaryText,
                fontSize = 15.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                color = ProgressPrimaryText,
                fontSize = 15.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = if (subtitle.startsWith("+")) ProgressPositive else ProgressSecondaryText,
                fontSize = 14.sp,
                lineHeight = 16.sp
            )
            if (badge != null) {
                BadgeChip(text = badge)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(38.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun OutcomeCard(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    value: String,
    subtitle: String,
    badge: String
) {
    ProgressCard(
        modifier = modifier,
        minHeight = 124.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconTile(iconRes = iconRes, tileSize = 62.dp, iconSize = 42.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = ProgressPrimaryText,
                    fontSize = 15.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = value,
                    color = ProgressPrimaryText,
                    fontSize = 13.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = ProgressPositive,
                    fontSize = 13.sp,
                    lineHeight = 13.sp
                )
                BadgeChip(text = badge)
            }
        }
    }
}

private data class AchievementItem(
    val iconRes: Int,
    val title: String
)

private fun formatWeightKg(weightKg: Double?): String {
    if (weightKg == null) return "No data"
    return String.format(Locale.US, "%.1f kg", weightKg)
}

private fun formatStepsSourceLabel(sourceType: DailyStepsSourceType): String {
    return if (sourceType == DailyStepsSourceType.MANUAL) "Manual" else "Imported"
}

@Composable
private fun AchievementsCard(
    items: List<AchievementItem>
) {
    ProgressCard(minHeight = 152.dp) {
        Column {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconTile(
                        iconRes = item.iconRes,
                        tileSize = 46.dp,
                        iconSize = 25.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.title,
                        color = ProgressPrimaryText,
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.home_icon_chevron),
                        contentDescription = "Open achievement",
                        modifier = Modifier.size(11.dp)
                    )
                }
                if (index < items.lastIndex) {
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
private fun ProgressCard(
    modifier: Modifier = Modifier,
    minHeight: Dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(ProgressCardBackground)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        content()
    }
}

@Composable
private fun IconTile(
    iconRes: Int,
    tileSize: Dp,
    iconSize: Dp
) {
    Box(
        modifier = Modifier
            .size(tileSize)
            .clip(RoundedCornerShape(14.dp))
            .background(ProgressTileBackground),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun BadgeChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(ProgressBadgeBg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = ProgressSecondaryText,
            fontSize = 12.sp,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun ProgressBottomNavigationBar(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit,
    onWorkoutClick: () -> Unit,
    onCommunityClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ProgressBottomBarBg)
                .border(width = 1.dp, color = Color(0x80D6AA98))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProgressBottomNavItem(label = "Home", icon = R.drawable.home_nav_home, textColor = ProgressInactiveIcon, onClick = onHomeClick)
            ProgressBottomNavItem(label = "Workout", icon = R.drawable.home_nav_workout, textColor = ProgressInactiveIcon, onClick = onWorkoutClick)
            ProgressBottomNavItem(label = "Progress", icon = R.drawable.home_nav_progress_curr, textColor = Color(0xFFBF7E65), onClick = {})
            ProgressBottomNavItem(
                label = "Community",
                icon = R.drawable.home_nav_community,
                textColor = ProgressInactiveIcon,
                iconWidth = 28.dp,
                iconHeight = 24.dp,
                iconContentScale = ContentScale.FillBounds,
                onClick = onCommunityClick
            )
            ProgressBottomNavItem(label = "Profile", icon = R.drawable.home_nav_profile, textColor = ProgressInactiveIcon, onClick = onProfileClick)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(
                    color = ProgressBottomBarBg,
                    shape = RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = ProgressBottomInsetCorner,
                        bottomEnd = ProgressBottomInsetCorner
                    )
                )
        )
    }
}

@Composable
private fun ProgressBottomNavItem(
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

@Preview(showBackground = true, widthDp = 402, heightDp = 868)
@Composable
private fun ProgressScreenPreview() {
    MaterialTheme {
        ProgressScreen()
    }
}
