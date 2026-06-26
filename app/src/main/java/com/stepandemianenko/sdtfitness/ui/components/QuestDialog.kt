package com.stepandemianenko.sdtfitness.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stepandemianenko.sdtfitness.ui.theme.QuestDialogContainer
import com.stepandemianenko.sdtfitness.ui.theme.QuestDialogPrimaryText

/**
 * Canonical shell for every Home "quest" pop-up (Weight-In, Creatine, Steps, …).
 *
 * It is intentionally a bare window: the app palette, an optional [title], a content slot you fill,
 * and a single **"Close"** button pinned to the bottom-right. Build all new quest dialogs on top of
 * this instead of declaring a fresh [AlertDialog]. That keeps the palette and the close-button
 * placement identical across quests — a quest can add its own actions inside [content], but it can
 * never move or restyle the close affordance, so dialogs can't drift apart again.
 *
 * @param onDismiss invoked by the Close button and by outside/back dismissal.
 * @param title optional bold heading; pass null for a truly empty window.
 * @param closeLabel text for the close button (defaults to "Close").
 * @param content the quest's body, laid out in a vertical [ColumnScope].
 */
@Composable
fun QuestDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    closeLabel: String = "Close",
    content: @Composable ColumnScope.() -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = QuestDialogContainer,
        title = title?.let {
            {
                Text(
                    text = it,
                    color = QuestDialogPrimaryText,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = closeLabel,
                    color = QuestDialogPrimaryText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Preview
@Composable
private fun QuestDialogPreview() {
    MaterialTheme {
        QuestDialog(
            onDismiss = {},
            title = "Quest Template",
            content = {
                Text(
                    text = "Quest content goes here.",
                    color = QuestDialogPrimaryText
                )
            }
        )
    }
}
