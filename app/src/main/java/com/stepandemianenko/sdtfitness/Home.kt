package com.example.fitnessapp.workouts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import com.example.fitnessapp.ExerciseActivity
//import com.example.fitnessapp.ProfileActivity
import com.stepandemianenko.sdtfitness.R
//import com.example.fitnessapp.SettingsActivity

class homeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HomeOneScreen(
                    /*onStartWorkoutClick = {
                        startActivity(Intent(this, WorkoutSessionActivity::class.java))
                    },
                    onWorkoutClick = {
                        startActivity(Intent(this, ExerciseActivity::class.java))
                    },
                    onProgressClick = {
                        startActivity(Intent(this, ProfileActivity::class.java))
                    },
                    onCommunityClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onProfileClick = {
                        startActivity(Intent(this, ProfileActivity::class.java))
                    }*/
                )
            }
        }
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

@Composable
fun HomeOneScreen(
    onStartWorkoutClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onCommunityClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeaderSection()
                DailyProgressCard()
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
                    title = "Stretch & Mobility",
                    subtitle = "10 min  •  Post-workout",
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.home_icon_stretch),
                            contentDescription = "Stretch icon",
                            modifier = Modifier.size(46.dp)
                        )
                    },
                    onClick = onWorkoutClick
                )
                SectionHeader(title = "Daily Quest", action = "Optional")
                DailyQuestCard()
                AddTile(onClick = onWorkoutClick)
                SectionHeader(title = "Friends Activity", action = "See all")
                FriendsActivityCard()
            }
            BottomNavigationBar(
                onWorkoutClick = onWorkoutClick,
                onProgressClick = onProgressClick,
                onCommunityClick = onCommunityClick,
                onProfileClick = onProfileClick
            )
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Good afternoon",
            color = PrimaryText,
            fontSize = 22.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Let's keep the momentum going",
            color = SecondaryText,
            fontSize = 11.sp,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun DailyProgressCard() {
    HomeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoalProgressRing(
                progress = 0.68f,
                modifier = Modifier.size(92.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "7,532 / 10,000 steps",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "2 / 3 workouts",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
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
                        text = "85 / 120 active min",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
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
                fontSize = 15.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Daily Goal",
                color = SecondaryText,
                fontSize = 11.sp,
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
            fontSize = 14.sp,
            lineHeight = 14.sp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "Start Workout",
            color = Color(0xFFFCE8DA),
            fontSize = 14.sp,
            lineHeight = 15.sp
        )
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(id = R.drawable.home_icon_chevron),
            contentDescription = "Go",
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, action: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            color = PrimaryText,
            fontSize = 16.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = action,
            color = SecondaryText,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
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
                    fontSize = 15.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = subtitle,
                    color = SecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 12.sp
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
            .background(Color(0xFFECC7B1)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun DailyQuestCard() {
    HomeCard(horizontalPadding = 10.dp, verticalPadding = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIconContainer(iconRes = R.drawable.home_shoes)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Walk 5,000 steps",
                    color = PrimaryText,
                    fontSize = 15.sp,
                    lineHeight = 16.sp
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
                            .fillMaxWidth(0.7f)
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
                        text = "+ 50 xp",
                        color = PrimaryText,
                        fontSize = 11.sp,
                        lineHeight = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.home_icon_chevron),
                    contentDescription = "Open quest",
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
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
private fun FriendsActivityCard() {
    HomeCard(verticalPadding = 10.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FriendActivityRow(
                message = "Anna completed a Gym workout",
                badgeIcon = R.drawable.home_biceps,
                time = "5h"
            )
            FriendActivityRow(
                message = "Ben reached a 7-day streak",
                badgeIcon = R.drawable.home_fire,
                time = "2h"
            )
        }
    }
}

@Composable
private fun FriendActivityRow(message: String, badgeIcon: Int, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = PrimaryText,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFFF4C89B)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = badgeIcon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "•  $time",
            color = SecondaryText,
            fontSize = 12.sp,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun BottomNavigationBar(
    onWorkoutClick: () -> Unit,
    onProgressClick: () -> Unit,
    onCommunityClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BottomBarBg)
            .border(width = 1.dp, color = Color(0x80D6AA98))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavItem(label = "Home", icon = R.drawable.home_nav_home, textColor = Color(0xFFBF7E65), onClick = {})
        BottomNavItem(label = "Workout", icon = R.drawable.home_nav_workout, textColor = InactiveIcon, onClick = onWorkoutClick)
        BottomNavItem(label = "Progress", icon = R.drawable.home_nav_progress, textColor = InactiveIcon, onClick = onProgressClick)
        BottomNavItem(label = "Community", icon = R.drawable.home_nav_community, textColor = InactiveIcon, onClick = onCommunityClick)
        BottomNavItem(label = "Profile", icon = R.drawable.home_nav_profile, textColor = InactiveIcon, onClick = onProfileClick)
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: Int,
    textColor: Color,
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
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
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

@Preview(showBackground = true, widthDp = 380, heightDp = 840)
@Composable
private fun HomeOneScreenPreview() {
    MaterialTheme {
        HomeOneScreen()
    }
}
