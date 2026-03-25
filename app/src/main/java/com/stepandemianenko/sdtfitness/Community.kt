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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class Community : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CommunityOneScreen(
                    onHomeClick = {
                        startActivity(Intent(this, Home::class.java))
                    },
                    onWorkoutClick = {
                        startActivity(Intent(this, StartWorkout::class.java))
                    }
                )
            }
        }
    }
}

private val AppBackground = Color(0xFFEBC0B0)
private val PrimaryText = Color(0xFF4F2912)
private val SecondaryText = Color(0xFF6B4637)
private val BottomBarBg = Color(0xFFF5E5DA)
private val InactiveIcon = Color(0xFFC48778)
private val ActiveIcon = Color(0xFFBF7E65)

private val ScreenContentMaxWidth = 360.dp
private const val ReservedBottomFraction = 0.15f
private val ScreenHorizontalPadding = 20.dp
private val ScreenTopPadding = 30.dp

@Composable
fun CommunityOneScreen(
    onHomeClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val reservedBottomHeight = maxHeight * ReservedBottomFraction

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = ScreenContentMaxWidth)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = ScreenHorizontalPadding,
                                end = ScreenHorizontalPadding,
                                top = ScreenTopPadding,
                                bottom = reservedBottomHeight
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Community",
                            color = PrimaryText,
                            fontSize = 42.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Top friends / Leaderboard
                        SectionHeader("Leaderboard", "This Week")
                        LeaderboardCard()

                        // Weekly Community Challenge
                        SectionHeader("Community Goal", "10k XP")
                        CommunityChallengeCard()

                        // Feed
                        SectionHeader("Activity Feed", "Latest")
                        ActivityFeed()
                    }
                }

                CommunityBottomNavigationBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onHomeClick = onHomeClick,
                    onWorkoutClick = onWorkoutClick,
                    onProgressClick = onProgressClick,
                    onProfileClick = onProfileClick
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            color = PrimaryText,
            fontSize = 18.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = action,
            color = SecondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LeaderboardCard() {
    CommunityCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            PodiumBar(rank = 2, name = "Alex", xp = "3,200 XP", height = 80.dp, color = Color(0xFFC0C0C0))
            PodiumBar(rank = 1, name = "You", xp = "4,500 XP", height = 110.dp, color = Color(0xFFFFD700))
            PodiumBar(rank = 3, name = "Sam", xp = "2,900 XP", height = 60.dp, color = Color(0xFFCD7F32))
        }
    }
}

@Composable
private fun PodiumBar(rank: Int, name: String, xp: String, height: Dp, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar Placeholder
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFECC7B1)),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", fontWeight = FontWeight.Bold, color = PrimaryText)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(color),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(name, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(xp, color = PrimaryText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun CommunityChallengeCard() {
    CommunityCard {
        Column {
            Text(
                text = "Lift 10,000 kg as a team",
                color = PrimaryText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Reward: Exclusive Gold Badge",
                color = SecondaryText,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE6B8A5))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(8.dp)
                        .background(Color(0xFF69C47A))
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "6,520 / 10,000 kg",
                color = SecondaryText,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun ActivityFeed() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FeedItem(
            name = "Anna",
            action = "crushed Leg Day",
            time = "2h ago",
            points = "+120 XP"
        )
        FeedItem(
            name = "Ben",
            action = "reached a 7-day streak!",
            time = "5h ago",
            points = "+50 XP"
        )
    }
}

@Composable
private fun FeedItem(name: String, action: String, time: String, points: String) {
    CommunityCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFECC7B1)),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1), fontWeight = FontWeight.Bold, color = PrimaryText)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$name $action",
                    color = PrimaryText,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = time,
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = points,
                    color = Color(0xFFF08A67), // ActionColor
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReactionButton("👏")
                    ReactionButton("🔥")
                }
            }
        }
    }
}

@Composable
private fun ReactionButton(emoji: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEBC0B0)) // AppBackground
            .clickable { }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = emoji, fontSize = 12.sp)
    }
}

@Composable
private fun CommunityCard(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF4E3D7)) // CardBackground
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        content()
    }
}

@Composable
private fun CommunityBottomNavigationBar(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit,
    onWorkoutClick: () -> Unit,
    onProgressClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BottomBarBg)
            .border(width = 1.dp, color = Color(0x80D6AA98))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CommunityBottomNavItem(
            label = "Home",
            icon = R.drawable.home_nav_home,
            textColor = InactiveIcon,
            onClick = onHomeClick
        )
        CommunityBottomNavItem(
            label = "Workout",
            icon = R.drawable.home_nav_workout,
            textColor = InactiveIcon,
            onClick = onWorkoutClick
        )
        CommunityBottomNavItem(
            label = "Progress",
            icon = R.drawable.home_nav_progress,
            textColor = InactiveIcon,
            onClick = onProgressClick
        )
        CommunityBottomNavItem(
            label = "Community",
            icon = R.drawable.home_nav_community_curr,
            textColor = ActiveIcon,
            iconWidth = 28.dp,
            iconHeight = 24.dp,
            iconContentScale = ContentScale.FillBounds,
            onClick = {}
        )
        CommunityBottomNavItem(
            label = "Profile",
            icon = R.drawable.home_nav_profile,
            textColor = InactiveIcon,
            onClick = onProfileClick
        )
    }
}

@Composable
private fun CommunityBottomNavItem(
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
            fontSize = 10.sp,
            lineHeight = 10.sp
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun CommunityOneScreenPreview() {
    MaterialTheme {
        CommunityOneScreen()
    }
}
