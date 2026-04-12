package com.stepandemianenko.sdtfitness.quicklog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepandemianenko.sdtfitness.R
import com.stepandemianenko.sdtfitness.home.QuickLogType

@Composable
fun QuickLogRoute(
    onBackClick: () -> Unit,
    onQuickLogSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
    cardColor: Color,
    accentColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    viewModel: QuickLogViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val successMessage = androidx.compose.ui.res.stringResource(id = R.string.quick_log_saved_message)

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
    val contentSpacing = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_content_spacing)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(contentSpacing)
    ) {
        BackToHomeRow(
            onClick = onBackClick,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
        QuickLogCard(
            uiState = uiState,
            onActivitySelected = onActivitySelected,
            onDurationSelected = onDurationSelected,
            onSaveClick = onSaveClick,
            cardColor = cardColor,
            accentColor = accentColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
    }
}

@Composable
fun BackToHomeRow(
    onClick: () -> Unit,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "‹",
            color = primaryTextColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = androidx.compose.ui.res.stringResource(id = R.string.rest_day_back_to_home),
            color = secondaryTextColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun QuickLogCard(
    uiState: QuickLogUiState,
    onActivitySelected: (QuickLogType) -> Unit,
    onDurationSelected: (Int) -> Unit,
    onSaveClick: () -> Unit,
    cardColor: Color,
    accentColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    val cardCornerRadius = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_card_corner_radius)
    val cardHorizontalPadding = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_card_horizontal_padding)
    val cardVerticalPadding = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_card_vertical_padding)
    val contentSpacing = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_content_spacing)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(cardCornerRadius),
        color = cardColor
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = cardHorizontalPadding,
                vertical = cardVerticalPadding
            ),
            verticalArrangement = Arrangement.spacedBy(contentSpacing)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.quick_log_title),
                    color = primaryTextColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.quick_log_subtitle),
                    color = secondaryTextColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            QuickLogOptionRow(
                selectedType = uiState.selectedType,
                onTypeSelected = onActivitySelected,
                accentColor = accentColor,
                cardColor = cardColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
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

            SaveQuickLogButton(
                onClick = onSaveClick,
                isSaving = uiState.isSaving,
                accentColor = accentColor,
                cardColor = cardColor
            )

            Text(
                text = androidx.compose.ui.res.stringResource(id = R.string.quick_log_helper_text),
                color = secondaryTextColor,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
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
    val optionSpacing = androidx.compose.ui.res.dimensionResource(id = R.dimen.rest_day_option_spacing)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(optionSpacing)
    ) {
        QuickLogOptionItem(
            modifier = Modifier.weight(1f),
            type = QuickLogType.WALK,
            label = androidx.compose.ui.res.stringResource(id = R.string.quick_log_option_walk),
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.home_shoes),
                    contentDescription = androidx.compose.ui.res.stringResource(id = R.string.quick_log_walk_icon_cd),
                    modifier = Modifier.size(34.dp)
                )
            },
            isSelected = selectedType == QuickLogType.WALK,
            onClick = { onTypeSelected(QuickLogType.WALK) },
            accentColor = accentColor,
            cardColor = cardColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
        QuickLogOptionItem(
            modifier = Modifier.weight(1f),
            type = QuickLogType.MOBILITY,
            label = androidx.compose.ui.res.stringResource(id = R.string.quick_log_option_mobility),
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.stretch),
                    contentDescription = androidx.compose.ui.res.stringResource(id = R.string.quick_log_mobility_icon_cd),
                    modifier = Modifier.size(34.dp)
                )
            },
            isSelected = selectedType == QuickLogType.MOBILITY,
            onClick = { onTypeSelected(QuickLogType.MOBILITY) },
            accentColor = accentColor,
            cardColor = cardColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
        QuickLogOptionItem(
            modifier = Modifier.weight(1f),
            type = QuickLogType.CUSTOM,
            label = androidx.compose.ui.res.stringResource(id = R.string.quick_log_option_custom),
            icon = {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = accentColor.copy(alpha = if (selectedType == QuickLogType.CUSTOM) 1f else 0.22f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = androidx.compose.ui.res.stringResource(id = R.string.quick_log_custom_icon_cd),
                        tint = if (selectedType == QuickLogType.CUSTOM) cardColor else accentColor
                    )
                }
            },
            isSelected = selectedType == QuickLogType.CUSTOM,
            onClick = { onTypeSelected(QuickLogType.CUSTOM) },
            accentColor = accentColor,
            cardColor = cardColor,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor
        )
    }
}

@Composable
fun QuickLogOptionItem(
    modifier: Modifier = Modifier,
    type: QuickLogType,
    label: String,
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    cardColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    val optionCornerRadius = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_option_corner_radius)
    val optionHeight = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_option_height)
    val optionInnerSpacing = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_option_inner_spacing)
    val selectedDescription = androidx.compose.ui.res.stringResource(id = R.string.quick_log_selected_state)
    val unselectedDescription = androidx.compose.ui.res.stringResource(id = R.string.quick_log_not_selected_state)
    val borderColor = if (isSelected) accentColor else secondaryTextColor.copy(alpha = 0.20f)
    val containerColor = if (isSelected) accentColor.copy(alpha = 0.13f) else cardColor.copy(alpha = 0.72f)

    Box(
        modifier = modifier
            .height(optionHeight)
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics {
                selected = isSelected
                stateDescription = if (isSelected) selectedDescription else unselectedDescription
            }
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(optionCornerRadius))
            .background(color = containerColor, shape = RoundedCornerShape(optionCornerRadius))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.TopEnd)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(optionInnerSpacing)
        ) {
            icon()
            Text(
                text = label,
                color = if (isSelected) primaryTextColor else secondaryTextColor,
                style = MaterialTheme.typography.bodySmall,
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
    val chipSpacing = androidx.compose.ui.res.dimensionResource(id = R.dimen.rest_day_option_spacing)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(chipSpacing)
    ) {
        durations.forEach { minutes ->
            DurationChip(
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
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    cardColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    val chipCornerRadius = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_chip_corner_radius)
    val chipHorizontalPadding = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_chip_horizontal_padding)
    val chipVerticalPadding = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_chip_vertical_padding)
    val selectedDescription = androidx.compose.ui.res.stringResource(id = R.string.quick_log_selected_state)
    val unselectedDescription = androidx.compose.ui.res.stringResource(id = R.string.quick_log_not_selected_state)

    Box(
        modifier = Modifier
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .semantics {
                selected = isSelected
                stateDescription = if (isSelected) selectedDescription else unselectedDescription
            }
            .border(
                width = 1.dp,
                color = if (isSelected) accentColor else secondaryTextColor.copy(alpha = 0.20f),
                shape = RoundedCornerShape(chipCornerRadius)
            )
            .background(
                color = if (isSelected) accentColor.copy(alpha = 0.14f) else cardColor.copy(alpha = 0.72f),
                shape = RoundedCornerShape(chipCornerRadius)
            )
            .padding(horizontal = chipHorizontalPadding, vertical = chipVerticalPadding)
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(id = R.string.quick_log_duration_template, minutes),
            color = if (isSelected) primaryTextColor else secondaryTextColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
fun SaveQuickLogButton(
    onClick: () -> Unit,
    isSaving: Boolean,
    accentColor: Color,
    cardColor: Color
) {
    val buttonHeight = androidx.compose.ui.res.dimensionResource(id = R.dimen.quick_log_primary_button_height)

    Button(
        onClick = onClick,
        enabled = !isSaving,
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonHeight),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = cardColor
        )
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(id = R.string.quick_log_save_button),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun QuickLogScreenPreview() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFEBC0B0)
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
