package com.stepandemianenko.sdtfitness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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

    ProgressScreen(
        uiState = uiState,
        onHomeClick = onHomeClick,
        onWorkoutClick = onWorkoutClick,
        onCommunityClick = onCommunityClick,
        onProfileClick = onProfileClick
    )
}

@Composable
fun ProgressScreen(
    uiState: ProgressUiState = ProgressUiState(isLoading = false),
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
    modifier: Modifier = Modifier
) {
    var showImportedData by rememberSaveable { mutableStateOf(true) }

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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle(title = "Outcomes")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = showImportedData,
                    onCheckedChange = { showImportedData = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFDEDE7),
                        checkedTrackColor = ProgressAccent,
                        uncheckedThumbColor = Color(0xFFFDEDE7),
                        uncheckedTrackColor = ProgressDivider
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Show imported health data",
                    color = ProgressSecondaryText,
                    fontSize = 14.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutcomeCard(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.home_shoes,
                title = "Steps",
                value = "62,025",
                subtitle = "+8% vs last week",
                badge = "Imported"
            )
            OutcomeCard(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.orange_scales,
                title = "Weight",
                value = "85.6 kg",
                subtitle = "+1.0 kg vs last week",
                badge = "Imported"
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
