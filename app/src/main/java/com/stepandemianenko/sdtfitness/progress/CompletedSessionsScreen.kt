package com.stepandemianenko.sdtfitness.progress

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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.stepandemianenko.sdtfitness.R

private val ProgressBackground = Color(0xFFEBC0B0)
private val ProgressCardBackground = Color(0xFFF5E5DA)
private val ProgressTileBackground = Color(0xFFF1D5CB)
private val ProgressPrimaryText = Color(0xFF4F2912)
private val ProgressSecondaryText = Color(0xFF6B4637)
private val ProgressAccent = Color(0xFFF08A67)
private val ProgressBottomBarBg = Color(0xFFF5E5DA)
private val ProgressInactiveIcon = Color(0xFFC48778)
private val ProgressBottomInsetCorner = 16.dp

@Composable
fun CompletedSessionsRoute(
    onBackClick: () -> Unit,
    onOpenSessionReview: (Long) -> Unit,
    onHomeClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: CompletedSessionsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CompletedSessionsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onRefresh = viewModel::refresh,
        onOpenSessionReview = onOpenSessionReview,
        onHomeClick = onHomeClick,
        onWorkoutClick = onWorkoutClick,
        onProgressClick = onProgressClick,
        onProfileClick = onProfileClick
    )
}

@Composable
fun CompletedSessionsScreen(
    uiState: CompletedSessionsUiState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSessionReview: (Long) -> Unit,
    onHomeClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ProgressBackground,
        bottomBar = {
            CompletedSessionsBottomNavigationBar(
                onHomeClick = onHomeClick,
                onWorkoutClick = onWorkoutClick,
                onProgressClick = onProgressClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackRow(
                text = "Back to Progress",
                onClick = onBackClick
            )

            Text(
                text = "Completed Sessions",
                color = ProgressPrimaryText,
                fontSize = 34.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Review your consistency and revisit finished workouts",
                color = ProgressSecondaryText,
                fontSize = 14.sp,
                lineHeight = 16.sp
            )

            HistoryCard {
                WeeklySessionsComparisonGraph(
                    thisWeekSessions = uiState.sessionsThisWeek,
                    lastWeekSessions = uiState.sessionsLastWeek
                )
            }

            when {
                uiState.isLoading -> {
                    Text(
                        text = "Loading completed sessions...",
                        color = ProgressSecondaryText,
                        fontSize = 14.sp,
                        lineHeight = 16.sp
                    )
                }

                uiState.errorMessage != null -> {
                    HistoryCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = uiState.errorMessage,
                                color = ProgressSecondaryText,
                                fontSize = 14.sp,
                                lineHeight = 16.sp
                            )
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ProgressAccent,
                                    contentColor = Color(0xFFFDEDE7)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Retry",
                                    fontSize = 14.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                uiState.sessions.isEmpty() -> {
                    HistoryCard {
                        Text(
                            text = "No completed sessions yet. Finish a workout and it will appear here.",
                            color = ProgressSecondaryText,
                            fontSize = 14.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                else -> {
                    val thisWeekCount = uiState.sessionsThisWeek.coerceIn(0, uiState.sessions.size)
                    val thisWeekSessions = uiState.sessions.take(thisWeekCount)

                    val remainingSessions = uiState.sessions.drop(thisWeekCount)
                    val lastWeekCount = uiState.sessionsLastWeek.coerceIn(0, remainingSessions.size)
                    val lastWeekSessions = remainingSessions.take(lastWeekCount)
                    val earlierSessions = remainingSessions.drop(lastWeekCount)

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (thisWeekSessions.isNotEmpty()) {
                            ListSectionLabel(
                                title = "This Week",
                                subtitle = "Workouts logged this week"
                            )
                            thisWeekSessions.forEach { session ->
                                CompletedSessionListItem(
                                    item = session,
                                    onClick = { onOpenSessionReview(session.sessionId) }
                                )
                            }
                        }

                        if (lastWeekSessions.isNotEmpty()) {
                            ListSectionLabel(
                                title = "Last Week"
                            )
                            lastWeekSessions.forEach { session ->
                                CompletedSessionListItem(
                                    item = session,
                                    onClick = { onOpenSessionReview(session.sessionId) }
                                )
                            }
                        }

                        earlierSessions.forEach { session ->
                            CompletedSessionListItem(
                                item = session,
                                onClick = { onOpenSessionReview(session.sessionId) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ListSectionLabel(
    title: String,
    subtitle: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            color = ProgressPrimaryText,
            fontSize = 16.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        subtitle?.let { caption ->
            Text(
                text = caption,
                color = ProgressSecondaryText,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun CompletedSessionsBottomNavigationBar(
    onHomeClick: () -> Unit,
    onWorkoutClick: () -> Unit,
    onProgressClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ProgressBottomBarBg)
                .border(width = 1.dp, color = Color(0x80D6AA98))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CompletedSessionsBottomNavItem(
                label = "Home",
                icon = R.drawable.home_nav_home,
                textColor = ProgressInactiveIcon,
                onClick = onHomeClick
            )
            CompletedSessionsBottomNavItem(
                label = "Workout",
                icon = R.drawable.home_nav_workout,
                textColor = ProgressInactiveIcon,
                onClick = onWorkoutClick
            )
            CompletedSessionsBottomNavItem(
                label = "Progress",
                icon = R.drawable.home_nav_progress_curr,
                textColor = Color(0xFFBF7E65),
                onClick = onProgressClick
            )
            CompletedSessionsBottomNavItem(
                label = "Profile",
                icon = R.drawable.home_nav_profile,
                textColor = ProgressInactiveIcon,
                onClick = onProfileClick
            )
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
private fun CompletedSessionsBottomNavItem(
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
        androidx.compose.foundation.Image(
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
private fun CompletedSessionListItem(
    item: CompletedSessionItemUiModel,
    onClick: () -> Unit
) {
    HistoryCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ProgressTileBackground),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.calendar_streak_consistency),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    color = ProgressPrimaryText,
                    fontSize = 15.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.dateLabel,
                    color = ProgressSecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 14.sp
                )
                Text(
                    text = item.exerciseCountLabel,
                    color = ProgressSecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 14.sp
                )
                item.summaryLabel?.let { summary ->
                    Text(
                        text = summary,
                        color = ProgressSecondaryText,
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.home_icon_chevron),
                contentDescription = "Open session",
                modifier = Modifier
                    .width(10.dp)
                    .height(14.dp)
            )
        }
    }
}

@Composable
private fun BackRow(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            color = ProgressPrimaryText,
            fontSize = 22.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = ProgressSecondaryText,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun HistoryCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ProgressCardBackground)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun CompletedSessionsScreenPreview() {
    MaterialTheme {
        CompletedSessionsScreen(
            uiState = CompletedSessionsUiState(
                isLoading = false,
                sessionsThisWeek = 3,
                sessionsLastWeek = 2,
                sessions = listOf(
                    CompletedSessionItemUiModel(
                        sessionId = 1,
                        title = "Upper Body Session",
                        dateLabel = "Sun, Apr 12",
                        exerciseCountLabel = "5 exercises",
                        summaryLabel = "16 sets • 122 reps • 2,880 kg volume"
                    )
                )
            ),
            onBackClick = {},
            onRefresh = {},
            onOpenSessionReview = {},
            onHomeClick = {},
            onWorkoutClick = {},
            onProgressClick = {},
            onProfileClick = {}
        )
    }
}
