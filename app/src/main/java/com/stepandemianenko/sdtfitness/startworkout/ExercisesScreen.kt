package com.stepandemianenko.sdtfitness.startworkout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
private val ExercisesInfoMessageBg = Color(0xFFFFBE91).copy(alpha = 0.9f)

@Composable
fun ExercisesScreen(
    exercises: List<ExerciseCatalogItemUiModel>,
    customExerciseSets: List<CustomExerciseSetUiModel>,
    selectedCustomSetId: String?,
    selectedExerciseIds: Set<String>,
    onBackClick: () -> Unit,
    onExerciseToggle: (String) -> Unit,
    onSaveCustomSet: (String?, String, Set<String>) -> Unit,
    onCustomSetSelect: (String) -> Unit,
    onAddSelectedClick: () -> Unit,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedMuscleGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var showMuscleGroups by rememberSaveable { mutableStateOf(false) }
    var showSetNamingPanel by rememberSaveable { mutableStateOf(false) }
    var namingSetId by rememberSaveable { mutableStateOf<String?>(null) }
    var customSetName by rememberSaveable { mutableStateOf("") }
    var showSelectExercisesMessage by rememberSaveable { mutableStateOf(false) }

    val muscleGroups = remember(exercises) {
        exercises
            .map { it.muscleGroup }
            .distinct()
            .sortedBy { it.lowercase() }
    }
    val filteredExercises = remember(exercises, searchQuery, selectedMuscleGroup) {
        exercises
            .filter { item ->
                selectedMuscleGroup == null || item.muscleGroup.equals(selectedMuscleGroup, ignoreCase = true)
            }
            .filter { item ->
                searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true)
            }
    }
    val selectedCount = selectedExerciseIds.size
    LaunchedEffect(selectedCount) {
        if (selectedCount > 0) {
            showSelectExercisesMessage = false
        } else {
            showSetNamingPanel = false
            customSetName = ""
            namingSetId = null
        }
    }
    val activeCustomSet = customExerciseSets.find { it.id == selectedCustomSetId }
    val addButtonText = if (activeCustomSet != null) {
        val addedCount = (selectedExerciseIds - activeCustomSet.exerciseIds).size
        val removedCount = (activeCustomSet.exerciseIds - selectedExerciseIds).size
        when {
            addedCount == 0 && removedCount == 0 -> "Add ${activeCustomSet.name}"
            removedCount == 0 -> "Add ${activeCustomSet.name} +$addedCount"
            addedCount == 0 -> "Add ${activeCustomSet.name} -$removedCount"
            else -> "Add ${activeCustomSet.name} (modified)"
        }
    } else if (selectedCount == 1) {
        "Add 1 exercise"
    } else {
        "Add $selectedCount exercises"
    }
    val listBottomPadding = if (selectedCount > 0) 76.dp else 10.dp

    Scaffold(
        modifier = modifier,
        containerColor = ExercisesBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            ExercisesBottomNavigationBar(
                onHomeClick = onHomeClick,
                onProgressClick = onProgressClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
                contentPadding = PaddingValues(top = 12.dp, bottom = listBottomPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    TopActionBar(onBackClick = onBackClick)
                }

                item {
                    SearchExerciseField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )
                }

                item {
                    FilterBar(
                        selectedMuscleGroup = selectedMuscleGroup,
                        onMusclesClick = { showMuscleGroups = !showMuscleGroups }
                    )
                }

                if (showMuscleGroups) {
                    item {
                        MuscleGroupsBlock(
                            muscleGroups = muscleGroups,
                            selectedMuscleGroup = selectedMuscleGroup,
                            onMuscleGroupClick = { group ->
                                selectedMuscleGroup = group
                                showMuscleGroups = false
                            }
                        )
                    }
                }

                item {
                    if (showSetNamingPanel) {
                        CustomSetNamingPanel(
                            selectedCount = selectedCount,
                            setName = customSetName,
                            onSetNameChange = { customSetName = it },
                            onSetNameDone = {
                                val normalizedSetName = customSetName.trim()
                                if (normalizedSetName.isNotBlank()) {
                                    customSetName = normalizedSetName
                                    showSetNamingPanel = false
                                    onSaveCustomSet(
                                        namingSetId,
                                        normalizedSetName,
                                        selectedExerciseIds
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    customExerciseSets.forEach { customSet ->
                        CreatedSetBlock(
                            setName = customSet.name,
                            isSelected = customSet.id == selectedCustomSetId,
                            onSetClick = { onCustomSetSelect(customSet.id) },
                            onEditClick = {
                                namingSetId = customSet.id
                                customSetName = customSet.name
                                showSetNamingPanel = true
                                showSelectExercisesMessage = false
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    CustomSetActionBlock(
                        text = "Create Custom Set of Exercises",
                        onClick = {
                            if (selectedCount > 0) {
                                namingSetId = selectedCustomSetId
                                customSetName = activeCustomSet?.name.orEmpty()
                                showSetNamingPanel = true
                                showSelectExercisesMessage = false
                            } else {
                                showSetNamingPanel = false
                                showSelectExercisesMessage = true
                            }
                        }
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = ExercisesCardBackground
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            if (filteredExercises.isEmpty()) {
                                Text(
                                    text = "No exercises found",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = ExercisesText.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    modifier = Modifier.padding(vertical = 20.dp, horizontal = 8.dp)
                                )
                            }
                            filteredExercises.forEachIndexed { index, item ->
                                ExerciseListRow(
                                    title = item.title,
                                    muscleGroup = item.muscleGroup,
                                    isSelected = selectedExerciseIds.contains(item.id),
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
                    }
                }

            }

            if (selectedCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                        .fillMaxWidth()
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

            if (showSelectExercisesMessage) {
                InfoMessageBlock(
                    text = "To create Custom Set of Exercises select exercises first",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 2.dp, vertical = 48.dp)
                )
            }
        }
    }
}

@Composable
private fun TopActionBar(
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        BackToWorkoutButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            text = "Add Exercise",
            modifier = Modifier.align(Alignment.Center),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge.copy(
                color = ExercisesText,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
private fun BackToWorkoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "‹",
            color = ExercisesText,
            fontSize = 22.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "back",
            style = MaterialTheme.typography.titleSmall.copy(
                color = ExercisesText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
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
private fun FilterBar(
    selectedMuscleGroup: String?,
    onMusclesClick: () -> Unit
) {
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
                isSelected = true,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(ExercisesCardBackground.copy(alpha = 0.75f))
            )
            FilterTab(
                text = selectedMuscleGroup ?: "All Muscles",
                isSelected = true,
                modifier = Modifier.weight(1f),
                onClick = onMusclesClick
            )
        }
    }
}

@Composable
private fun FilterTab(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 14.sp
            )
        )
    }
}

@Composable
private fun CustomSetActionBlock(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = ExercisesCardBackground
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                color = ExercisesText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun CustomSetNamingPanel(
    selectedCount: Int,
    setName: String,
    onSetNameChange: (String) -> Unit,
    onSetNameDone: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ExercisesCardBackground
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Name Your Set of Exercises",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = ExercisesText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            )
            Text(
                text = "Create a name for this set of $selectedCount exercises.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ExercisesText.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF2DFD6)
            ) {
                BasicTextField(
                    value = setName,
                    onValueChange = onSetNameChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = ExercisesText,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            onSetNameDone()
                            focusManager.clearFocus()
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    decorationBox = { innerTextField ->
                        if (setName.isBlank()) {
                            Text(
                                text = "e.g. Upper Body A",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = ExercisesText.copy(alpha = 0.5f),
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
}

@Composable
private fun CreatedSetBlock(
    setName: String,
    isSelected: Boolean,
    onSetClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSetClick),
        shape = RoundedCornerShape(12.dp),
        color = ExercisesCardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = setName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = ExercisesText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onEditClick),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.start_workout_icon_edit),
                    contentDescription = "Edit custom set name",
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(26.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) ExercisesPrimary else Color.Transparent)
            )
        }
    }
}

@Composable
private fun InfoMessageBlock(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ExercisesInfoMessageBg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = ExercisesText.copy(alpha = 0.95f),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun MuscleGroupsBlock(
    muscleGroups: List<String>,
    selectedMuscleGroup: String?,
    onMuscleGroupClick: (String?) -> Unit
) {
    if (muscleGroups.isEmpty()) return

    val horizontalScroll = rememberScrollState()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ExercisesCardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MuscleGroupChip(
                label = "All Muscles",
                isSelected = selectedMuscleGroup == null,
                onClick = { onMuscleGroupClick(null) }
            )
            muscleGroups.forEach { group ->
                MuscleGroupChip(
                    label = group,
                    isSelected = selectedMuscleGroup == group,
                    onClick = { onMuscleGroupClick(group) }
                )
            }
        }
    }
}

@Composable
private fun MuscleGroupChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (isSelected) ExercisesPrimary else ExercisesPrimary.copy(alpha = 0.22f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isSelected) Color.White else ExercisesText,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
            customExerciseSets = emptyList(),
            selectedCustomSetId = null,
            selectedExerciseIds = selected,
            onBackClick = {},
            onExerciseToggle = {},
            onSaveCustomSet = { _, _, _ -> },
            onCustomSetSelect = {},
            onAddSelectedClick = {},
            onHomeClick = {},
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
