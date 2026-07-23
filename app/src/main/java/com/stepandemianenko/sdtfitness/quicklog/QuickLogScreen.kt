package com.stepandemianenko.sdtfitness.quicklog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepandemianenko.sdtfitness.R
import com.stepandemianenko.sdtfitness.home.QuickLogType
import java.util.Locale

@Composable
fun QuickLogRoute(
    onBackClick: () -> Unit,
    onQuickLogSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    cardColor: Color,
    accentColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    viewModel: QuickLogViewModel = viewModel(factory = QuickLogViewModel.Factory)
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val selectedActivityName = quickLogActivityName(uiState.selectedType)
    val successMessage = stringResource(
        id = R.string.quick_log_saved_message,
        uiState.selectedDurationMinutes,
        selectedActivityName.lowercase(Locale.getDefault())
    )

    LaunchedEffect(Unit) {
        viewModel.onEvent(QuickLogEvent.InitializeDefaults)
    }

    LaunchedEffect(viewModel, successMessage) {
        viewModel.effects.collect { effect ->
            if (effect is QuickLogEffect.Saved) {
                onQuickLogSaved(successMessage)
            }
        }
    }

    QuickLogScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onActivitySelected = { viewModel.onEvent(QuickLogEvent.SelectType(it)) },
        onDurationSelected = { viewModel.onEvent(QuickLogEvent.SelectDuration(it)) },
        onSaveClick = { viewModel.onEvent(QuickLogEvent.SaveQuickLog) },
        modifier = modifier,
        cardColor = cardColor,
        accentColor = accentColor,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor
    )
}

@Composable
fun QuickLogScreen(
    uiState: QuickLogUiState,
    onBackClick: () -> Unit,
    onActivitySelected: (QuickLogType) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardColor: Color,
    accentColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    val selectedActivityName = quickLogActivityName(uiState.selectedType)
    val actionActivityName = selectedActivityName.lowercase(Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        QuickLogHeader(
            onCloseClick = onBackClick,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(id = R.string.quick_log_activity_label),
                color = primaryTextColor,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            QuickLogOptionRow(
                selectedType = uiState.selectedType,
                onTypeSelected = onActivitySelected,
                accentColor = accentColor,
                cardColor = cardColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(id = R.string.quick_log_duration_label),
                color = primaryTextColor,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            DurationChipRow(
                selectedDurationMinutes = uiState.selectedDurationMinutes,
                durations = uiState.availableDurations,
                onDurationSelected = onDurationSelected,
                accentColor = accentColor,
                cardColor = cardColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )
        }

        QuickLogSummary(
            activityName = selectedActivityName,
            durationMinutes = uiState.selectedDurationMinutes,
            accentColor = accentColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )

        SaveQuickLogButton(
            label = stringResource(
                id = R.string.quick_log_save_button,
                uiState.selectedDurationMinutes,
                actionActivityName
            ),
            onClick = onSaveClick,
            isSaving = uiState.isSaving,
            accentColor = accentColor,
            primaryTextColor = primaryTextColor
        )
    }
}

@Composable
private fun QuickLogHeader(
    onCloseClick: () -> Unit,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.quick_log_title),
                color = primaryTextColor,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(id = R.string.quick_log_subtitle),
                color = secondaryTextColor,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
        IconButton(
            onClick = onCloseClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(id = R.string.quick_log_close),
                tint = primaryTextColor
            )
        }
    }
}

@Composable
fun QuickLogOptionRow(
    selectedType: QuickLogType,
    onTypeSelected: (QuickLogType) -> Unit,
    accentColor: Color,
    cardColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickLogOptionItem(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.quick_log_option_walk),
            iconRes = R.drawable.home_shoes,
            isSelected = selectedType == QuickLogType.WALK,
            onClick = { onTypeSelected(QuickLogType.WALK) },
            accentColor = accentColor,
            cardColor = cardColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
        QuickLogOptionItem(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.quick_log_option_mobility),
            iconRes = R.drawable.stretch,
            isSelected = selectedType == QuickLogType.MOBILITY,
            onClick = { onTypeSelected(QuickLogType.MOBILITY) },
            accentColor = accentColor,
            cardColor = cardColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
    }
}

@Composable
private fun QuickLogOptionItem(
    modifier: Modifier,
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    cardColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    val selectedDescription = stringResource(id = R.string.quick_log_selected_state)
    val unselectedDescription = stringResource(id = R.string.quick_log_not_selected_state)
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .height(108.dp)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics {
                selected = isSelected
                stateDescription = if (isSelected) selectedDescription else unselectedDescription
            }
            .background(
                color = if (isSelected) accentColor.copy(alpha = 0.14f) else cardColor,
                shape = shape
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else secondaryTextColor.copy(alpha = 0.20f),
                shape = shape
            )
            .padding(12.dp)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .background(accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = primaryTextColor,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(accentColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Text(
                text = label,
                color = if (isSelected) primaryTextColor else secondaryTextColor,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun DurationChipRow(
    selectedDurationMinutes: Int,
    durations: List<Int>,
    onDurationSelected: (Int) -> Unit,
    accentColor: Color,
    cardColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        durations.forEach { minutes ->
            DurationChip(
                modifier = Modifier.weight(1f),
                minutes = minutes,
                isSelected = selectedDurationMinutes == minutes,
                onClick = { onDurationSelected(minutes) },
                accentColor = accentColor,
                cardColor = cardColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )
        }
    }
}

@Composable
private fun DurationChip(
    modifier: Modifier,
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    cardColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    val selectedDescription = stringResource(id = R.string.quick_log_selected_state)
    val unselectedDescription = stringResource(id = R.string.quick_log_not_selected_state)
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .height(48.dp)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics {
                selected = isSelected
                stateDescription = if (isSelected) selectedDescription else unselectedDescription
            }
            .background(
                color = if (isSelected) accentColor.copy(alpha = 0.16f) else cardColor,
                shape = shape
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) accentColor else secondaryTextColor.copy(alpha = 0.20f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.quick_log_duration_template, minutes),
            color = if (isSelected) primaryTextColor else secondaryTextColor,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuickLogSummary(
    activityName: String,
    durationMinutes: Int,
    accentColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accentColor.copy(alpha = 0.10f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.30f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = primaryTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(id = R.string.quick_log_summary_label),
                    color = secondaryTextColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        id = R.string.quick_log_summary_value,
                        activityName,
                        durationMinutes
                    ),
                    color = primaryTextColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SaveQuickLogButton(
    label: String,
    onClick: () -> Unit,
    isSaving: Boolean,
    accentColor: Color,
    primaryTextColor: Color
) {
    Button(
        onClick = onClick,
        enabled = !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = primaryTextColor,
            disabledContainerColor = accentColor.copy(alpha = 0.55f),
            disabledContentColor = primaryTextColor.copy(alpha = 0.75f)
        )
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = primaryTextColor,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = label,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun quickLogActivityName(type: QuickLogType): String {
    return when (type) {
        QuickLogType.WALK -> "Walk"
        QuickLogType.MOBILITY -> "Mobility"
        QuickLogType.CUSTOM -> "Activity"
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun QuickLogScreenPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF4E3D7)
        ) {
            QuickLogScreen(
                uiState = QuickLogUiState(),
                onBackClick = {},
                onActivitySelected = {},
                onDurationSelected = {},
                onSaveClick = {},
                cardColor = Color(0xFFF4E3D7),
                accentColor = Color(0xFFF08A67),
                primaryTextColor = Color(0xFF4F2912),
                secondaryTextColor = Color(0xFF6B4637)
            )
        }
    }
}
