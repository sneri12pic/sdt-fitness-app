package com.stepandemianenko.sdtfitness.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepandemianenko.sdtfitness.R
import com.stepandemianenko.sdtfitness.noRippleClickable

/** The four primary destinations. The active one renders highlighted and its callback is ignored. */
enum class NavTab { HOME, WORKOUT, PROGRESS, PROFILE }

// Warm translucent "glass" so the screen behind tints through, with a bright rim + soft lift.
private val GlassFill = Brush.verticalGradient(listOf(Color(0xD9FCF1EA), Color(0xBFF3E0D4)))
private val GlassRim = Color(0x66FFFFFF)
private val GlassShadow = Color(0x33512E1C)
private val NavAccent = Color(0xFFF27F3E)
private val NavInactive = Color(0xFF9A6B58)

/**
 * Floating liquid-glass bottom navigation shared by every top-level screen.
 * Manages its own navigation-bar inset, so callers just drop it in a Scaffold `bottomBar`
 * or align it to the bottom of an overlay Box.
 */
@Composable
fun GlassBottomNav(
    active: NavTab,
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(12.dp, RoundedCornerShape(26.dp), clip = false, ambientColor = GlassShadow, spotColor = GlassShadow)
                .clip(RoundedCornerShape(26.dp))
                .background(GlassFill)
                .border(1.dp, GlassRim, RoundedCornerShape(26.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassNavItem(R.drawable.home_nav_home, "Home", active == NavTab.HOME, onHomeClick)
            GlassNavItem(R.drawable.home_nav_workout, "Workout", active == NavTab.WORKOUT, onWorkoutClick)
            GlassNavItem(R.drawable.home_nav_progress, "Progress", active == NavTab.PROGRESS, onProgressClick)
            GlassNavItem(R.drawable.home_nav_profile, "Profile", active == NavTab.PROFILE, onProfileClick)
        }
        // Gap between the floating pill and the gesture area, then clear the nav-bar inset.
        Spacer(modifier = Modifier.height(6.dp))
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun GlassNavItem(
    icon: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Inactive icons sit back at reduced opacity; the active one lifts to full-strength accent.
    val color = if (selected) NavAccent else NavInactive.copy(alpha = 0.65f)
    Column(
        modifier = Modifier
            .noRippleClickable(onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) NavAccent.copy(alpha = 0.14f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(color)
        )
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
