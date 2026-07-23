package com.stepandemianenko.sdtfitness

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepandemianenko.sdtfitness.data.health.HealthShareMode
import com.stepandemianenko.sdtfitness.profile.HealthConnectStatus
import com.stepandemianenko.sdtfitness.profile.HealthConnectUiState
import com.stepandemianenko.sdtfitness.profile.HealthConnectViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Same warm palette the rest of Profile uses.
private val HcCardBackground = Color(0xFFF5E5DA)
private val HcPrimaryText = Color(0xFF4F2912)
private val HcSecondaryText = Color(0xFF6B4637)
private val HcAccent = Color(0xFFF27F3E)
private val HcIconCircle = Color(0xBBF88863)
private const val HC_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

/**
 * Health Connect hub. Rendered inside Profile's scrolling column, so it emits content only
 * (no Surface/Scaffold of its own), like RoutineSetupContent. Lets the user connect, see imported
 * steps/weight + their app data, choose how Water/Workouts are shared (Auto / Manual / Off), and
 * disconnect.
 */
@Composable
fun HealthConnectScreen(
    onBackClick: () -> Unit,
    viewModel: HealthConnectViewModel = viewModel(factory = HealthConnectViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = remember { viewModel.permissionContract() }
    ) { viewModel.refresh() }

    Row(
        modifier = Modifier
            .clickable(onClick = onBackClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "‹", color = HcPrimaryText, fontSize = 22.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Back to profile",
            color = HcSecondaryText,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(HcIconCircle),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.profile_row_health_connect),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "Health Connect",
                color = HcPrimaryText,
                fontSize = 26.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sync your health data in one place",
                color = HcSecondaryText,
                fontSize = 14.sp,
                lineHeight = 16.sp
            )
        }
    }

    ConnectionCard(
        uiState = uiState,
        onConnectClick = { permissionLauncher.launch(viewModel.requestedPermissions) },
        onUpdateClick = { openHealthConnectInStore(context) }
    )

    if (uiState.isConnected) {
        ImportedCard(uiState = uiState)
    }

    InAppDataCard(uiState = uiState)

    SharingCard(
        uiState = uiState,
        onModeSelected = viewModel::setShareMode,
        onSyncNowClick = viewModel::syncNow,
        onGrantWriteClick = { permissionLauncher.launch(viewModel.requestedPermissions) }
    )

    if (uiState.isConnected) {
        TextButton(onClick = viewModel::disconnect, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Disconnect from Health Connect",
                color = HcAccent,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ConnectionCard(
    uiState: HealthConnectUiState,
    onConnectClick: () -> Unit,
    onUpdateClick: () -> Unit
) {
    val (headline, body) = when (uiState.status) {
        HealthConnectStatus.UNKNOWN -> "Checking…" to "Looking for Health Connect."
        HealthConnectStatus.UNAVAILABLE -> "Not available" to "Health Connect isn't available on this device."
        HealthConnectStatus.UPDATE_REQUIRED -> "Update needed" to "Update Health Connect to continue."
        HealthConnectStatus.NOT_CONNECTED -> "Not connected" to "Connect to read your steps and weight."
        HealthConnectStatus.CONNECTED -> "Connected" to "Reading steps and weight from Health Connect."
    }

    HcCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape)
                    .background(if (uiState.isConnected) Color(0xFF69C47A) else HcSecondaryText.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = headline, color = HcPrimaryText, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = body, color = HcSecondaryText, fontSize = 14.sp, lineHeight = 18.sp)

        when (uiState.status) {
            HealthConnectStatus.NOT_CONNECTED -> {
                Spacer(modifier = Modifier.height(12.dp))
                HcPrimaryButton(label = "Connect", onClick = onConnectClick)
            }
            HealthConnectStatus.UPDATE_REQUIRED -> {
                Spacer(modifier = Modifier.height(12.dp))
                HcPrimaryButton(label = "Update Health Connect", onClick = onUpdateClick)
            }
            else -> Unit
        }
    }
}

@Composable
private fun ImportedCard(uiState: HealthConnectUiState) {
    HcCard {
        HcCardTitle("Imported from Health Connect")
        Spacer(modifier = Modifier.height(8.dp))
        HcStatRow(
            label = "Steps today",
            value = uiState.todaySteps?.let { "%,d".format(it) } ?: "—"
        )
        HcStatRow(
            label = "Latest weight",
            value = uiState.latestWeightKg?.let { String.format(Locale.US, "%.1f kg", it) } ?: "No data"
        )
    }
}

@Composable
private fun InAppDataCard(uiState: HealthConnectUiState) {
    HcCard {
        HcCardTitle("Your app data")
        Spacer(modifier = Modifier.height(8.dp))
        HcStatRow(label = "Workouts completed", value = uiState.inApp.workoutsCompleted.toString())
        HcStatRow(label = "Water today", value = "${uiState.inApp.waterMlToday} ml")
    }
}

@Composable
private fun SharingCard(
    uiState: HealthConnectUiState,
    onModeSelected: (HealthShareMode) -> Unit,
    onSyncNowClick: () -> Unit,
    onGrantWriteClick: () -> Unit
) {
    HcCard {
        HcCardTitle("Share your data")
        Text(
            text = "Choose how Water and Workouts are pushed to Health Connect.",
            color = HcSecondaryText,
            fontSize = 13.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HealthShareMode.entries.forEach { mode ->
                val selected = mode == uiState.shareMode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) HcAccent else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (selected) HcAccent else HcSecondaryText.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(enabled = uiState.isConnected) { onModeSelected(mode) }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = if (selected) Color.White else HcSecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        when {
            !uiState.isConnected -> HcSharingFooter("Connect to Health Connect to share your data.")
            uiState.needsWriteAccess -> {
                HcSharingFooter("Health Connect needs write access before anything can be shared.")
                Spacer(modifier = Modifier.height(8.dp))
                HcPrimaryButton(label = "Allow write access", onClick = onGrantWriteClick)
            }
            uiState.shareMode == HealthShareMode.AUTO ->
                HcSharingFooter("Water and workouts are pushed automatically when you log them.")
            uiState.shareMode == HealthShareMode.MANUAL -> {
                HcPrimaryButton(
                    label = if (uiState.isSyncing) "Syncing…" else "Sync now",
                    onClick = onSyncNowClick,
                    enabled = !uiState.isSyncing
                )
                uiState.lastSyncedAt?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    HcSharingFooter("Last synced ${formatSyncTime(it)}")
                }
            }
            else -> HcSharingFooter("Nothing is shared with Health Connect.")
        }
    }
}

@Composable
private fun HcSharingFooter(text: String) {
    Text(text = text, color = HcSecondaryText, fontSize = 12.sp, lineHeight = 15.sp)
}

private fun formatSyncTime(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
}

@Composable
private fun HcCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HcCardBackground)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun HcCardTitle(text: String) {
    Text(text = text, color = HcPrimaryText, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun HcStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = HcSecondaryText, fontSize = 14.sp, lineHeight = 16.sp)
        Text(text = value, color = HcPrimaryText, fontSize = 15.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HcPrimaryButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = HcAccent, contentColor = Color.White)
    ) {
        Text(text = label, fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun openHealthConnectInStore(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=$HC_PROVIDER_PACKAGE")
        setPackage("com.android.vending")
    }
    runCatching { context.startActivity(intent) }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HealthConnectScreenPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ConnectionCard(
                uiState = HealthConnectUiState(status = HealthConnectStatus.NOT_CONNECTED, isLoading = false),
                onConnectClick = {},
                onUpdateClick = {}
            )
            InAppDataCard(uiState = HealthConnectUiState())
            SharingCard(
                uiState = HealthConnectUiState(
                    status = HealthConnectStatus.CONNECTED,
                    isLoading = false,
                    shareMode = HealthShareMode.MANUAL,
                    hasWritePermissions = true
                ),
                onModeSelected = {},
                onSyncNowClick = {},
                onGrantWriteClick = {}
            )
        }
    }
}
