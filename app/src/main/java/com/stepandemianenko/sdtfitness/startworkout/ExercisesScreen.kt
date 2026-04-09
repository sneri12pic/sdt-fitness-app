package com.stepandemianenko.sdtfitness.startworkout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepandemianenko.sdtfitness.R

private val ExercisesBackground = Color(0xFFF3C8B9)
private val ExercisesCardBackground = Color(0xFFF8E3D8)
private val ExercisesPrimary = Color(0xFFF28A61)
private val ExercisesPrimarySoft = Color(0xFFF2B79F)
private val ExercisesText = Color(0xFF6E4B40)
private val ExercisesDivider = Color(0xFFA78A80)
private val ExercisesNavBg = Color(0xFFF7E6DC)
private val ExercisesInactiveIcon = Color(0xFFC48778)

@Composable
fun ExercisesScreen(
    exercises: List<ExerciseCatalogItemUiModel>,
    selectedExerciseIds: Set<String>,
    onBackClick: () -> Unit,
    onExerciseToggle: (String) -> Unit,
    onAddSelectedClick: () -> Unit,
    onHomeClick: () -> Unit,
    onCommunityClick: () -> Unit,
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredExercises = remember(exercises, searchQuery) {
        if (searchQuery.isBlank()) {
            exercises
        } else {
            exercises.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }
    val selectedCount = selectedExerciseIds.size
    val addButtonText = if (selectedCount == 1) {
        "Add 1 exercise"
    } else {
        "Add $selectedCount exercises"
    }

    Scaffold(
        modifier = modifier,
        containerColor = ExercisesBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            ExercisesBottomNavigationBar(
                onHomeClick = onHomeClick,
                onCommunityClick = onCommunityClick,
                onProgressClick = onProgressClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 22.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = 12.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TopActionBar(
                onCancelClick = onBackClick,
                onCreateClick = {}
            )

            SearchExerciseField(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )

            FilterBar()

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = ExercisesCardBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 3.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(
                            count = filteredExercises.size,
                            key = { index -> filteredExercises[index].id }
                        ) { index ->
                            val item = filteredExercises[index]
                            val isSelected = selectedExerciseIds.contains(item.id)
                            ExerciseListRow(
                                title = item.title,
                                muscleGroup = item.muscleGroup,
                                isSelected = isSelected,
                                onClick = { onExerciseToggle(item.id) }
                            )
                            if (index < filteredExercises.lastIndex) {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(ExercisesDivider.copy(alpha = 0.75f))
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onAddSelectedClick),
                        shape = RoundedCornerShape(16.dp),
                        color = ExercisesPrimary
                    ) {
                        Text(
                            text = addButtonText,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                lineHeight = 14.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopActionBar(
    onCancelClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderPillButton(text = "Cancel", onClick = onCancelClick)
        Text(
            text = "Add Exercise",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge.copy(
                color = ExercisesText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        )
        HeaderPillButton(text = "Create", onClick = onCreateClick)
    }
}

@Composable
private fun HeaderPillButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = ExercisesPrimary
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun SearchExerciseField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF2DFD6)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchIcon()
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = ExercisesText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isBlank()) {
                        Text(
                            text = "Search exercise",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = ExercisesText.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun SearchIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 2.8f
        val radius = size.minDimension * 0.33f
        val center = Offset(size.width * 0.40f, size.height * 0.40f)
        drawCircle(
            color = ExercisesText.copy(alpha = 0.75f),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        drawLine(
            color = ExercisesText.copy(alpha = 0.75f),
            start = Offset(size.width * 0.65f, size.height * 0.65f),
            end = Offset(size.width * 0.93f, size.height * 0.93f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun FilterBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ExercisesPrimary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterTab(
                text = "All Equipment",
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(ExercisesCardBackground.copy(alpha = 0.75f))
            )
            FilterTab(
                text = "All Muscles",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FilterTab(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        )
    }
}

@Composable
private fun ExerciseListRow(
    title: String,
    muscleGroup: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.start_workout_dumbbell_orange),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium.copy(
                color = ExercisesText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = ExercisesPrimarySoft.copy(alpha = 0.45f)
        ) {
            Text(
                text = muscleGroup,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ExercisesText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (isSelected) ExercisesPrimary else Color.Transparent)
        )
    }
}

@Composable
private fun ExercisesBottomNavigationBar(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onCommunityClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ExercisesNavBg)
                .border(width = 1.dp, color = Color(0x80D6AA98))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ExercisesBottomNavItem(
                label = "Home",
                icon = R.drawable.home_nav_home,
                textColor = ExercisesInactiveIcon,
                onClick = onHomeClick
            )
            ExercisesBottomNavItem(
                label = "Workout",
                icon = R.drawable.home_nav_workout_curr,
                textColor = Color(0xFFBF7E65),
                onClick = {}
            )
            ExercisesBottomNavItem(
                label = "Progress",
                icon = R.drawable.home_nav_progress,
                textColor = ExercisesInactiveIcon,
                onClick = onProgressClick
            )
            ExercisesBottomNavItem(
                label = "Community",
                icon = R.drawable.home_nav_community,
                textColor = ExercisesInactiveIcon,
                iconWidth = 28.dp,
                iconHeight = 24.dp,
                iconContentScale = ContentScale.FillBounds,
                onClick = onCommunityClick
            )
            ExercisesBottomNavItem(
                label = "Profile",
                icon = R.drawable.home_nav_profile,
                textColor = ExercisesInactiveIcon,
                onClick = onProfileClick
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(ExercisesNavBg)
        )
    }
}

@Composable
private fun ExercisesBottomNavItem(
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
private fun ExercisesScreenPreview() {
    val exercises = StartWorkoutFakeStateProvider.defaultExerciseCatalog()
    val selected = StartWorkoutFakeStateProvider.defaultExerciseSelection()

    MaterialTheme {
        ExercisesScreen(
            exercises = exercises,
            selectedExerciseIds = selected,
            onBackClick = {},
            onExerciseToggle = {},
            onAddSelectedClick = {},
            onHomeClick = {},
            onCommunityClick = {},
            onProgressClick = {},
            onProfileClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun ExerciseListRowPreview() {
    MaterialTheme {
        Surface(color = ExercisesCardBackground) {
            Column(modifier = Modifier.padding(10.dp)) {
                ExerciseListRow(
                    title = "Bench Press (Barbell)",
                    muscleGroup = "Chest",
                    isSelected = true,
                    onClick = {}
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ExercisesDivider.copy(alpha = 0.75f))
                )
                ExerciseListRow(
                    title = "Deadlift (Barbell)",
                    muscleGroup = "Back",
                    isSelected = false,
                    onClick = {}
                )
            }
        }
    }
}
