package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.notifications.NotificationHelper
import com.example.ui.viewmodel.PulseGoalUiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    uiState: PulseGoalUiState,
    onToggleEnabled: (Boolean) -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onToggleDay: (Int) -> Unit,
    onCustomMessageChange: (String) -> Unit,
    onIncludeGoalSummaryChange: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    var showTimePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted || NotificationHelper.hasNotificationPermission(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = NotificationHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val settings = uiState.reminderSettings
    val formattedTime = String.format(
        Locale.getDefault(),
        "%d:%02d %s",
        if (settings.hour % 12 == 0) 12 else settings.hour % 12,
        settings.minute,
        if (settings.hour < 12) "AM" else "PM"
    )

    val dayLabels = listOf(
        1 to "M",
        2 to "T",
        3 to "W",
        4 to "T",
        5 to "F",
        6 to "S",
        7 to "S"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reminders_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Text(
                    text = stringResource(R.string.reminders_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = stringResource(R.string.reminders_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Permission Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .testTag("notification_permission_card"),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (hasPermission) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (hasPermission) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Filled.NotificationsOff
                            },
                            contentDescription = null,
                            tint = if (hasPermission) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                        Text(
                            text = if (hasPermission) {
                                stringResource(R.string.permission_granted_title)
                            } else {
                                stringResource(R.string.permission_needed_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = if (hasPermission) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                    Text(
                        text = if (hasPermission) {
                            stringResource(R.string.permission_granted_desc)
                        } else {
                            stringResource(R.string.permission_needed_desc)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasPermission) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                    if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.testTag("request_notification_permission_button")
                        ) {
                            Text(stringResource(R.string.enable_notifications_btn))
                        }
                    }
                }
            }
        }

        // Master Schedule Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.daily_reminder_switch_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.daily_reminder_switch_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isEnabled,
                            onCheckedChange = onToggleEnabled,
                            modifier = Modifier.testTag("daily_reminder_switch")
                        )
                    }

                    // Time Picker Row
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(enabled = settings.isEnabled) { showTimePicker = true }
                            .testTag("reminder_time_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.reminder_time_label),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Active Days
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.reminder_days_label),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dayLabels.forEach { (isoDay, shortChar) ->
                                val selected = isoDay in settings.activeDays
                                Box(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .clickable(enabled = settings.isEnabled) {
                                            onToggleDay(isoDay)
                                        }
                                        .testTag("reminder_day_$isoDay"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shortChar,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Dynamic Goal Summary toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.smart_summary_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource(R.string.smart_summary_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.includeGoalSummary,
                            onCheckedChange = onIncludeGoalSummaryChange,
                            modifier = Modifier.testTag("include_goal_summary_switch")
                        )
                    }

                    OutlinedTextField(
                        value = settings.customMessage,
                        onValueChange = onCustomMessageChange,
                        label = { Text(stringResource(R.string.custom_message_label)) },
                        placeholder = { Text(stringResource(R.string.custom_message_hint)) },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_reminder_message_input")
                    )
                }
            }
        }

        // Preview & Test Push Card
        item {
            val baseMessage = settings.customMessage.trim().ifBlank {
                stringResource(R.string.default_notification_body)
            }
            val remaining = (uiState.weeklySummary.totalActiveGoals - uiState.weeklySummary.completedTodayCount)
                .coerceAtLeast(0)
            val total = uiState.weeklySummary.totalActiveGoals
            val previewBody = if (settings.includeGoalSummary && total > 0) {
                if (remaining == 0) {
                    "All $total goals completed today! Keep your momentum going."
                } else {
                    "$remaining of $total goals remaining today. $baseMessage"
                }
            } else {
                baseMessage
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.notification_preview_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.default_notification_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = previewBody,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onSendTestNotification()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_test_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.send_test_notification))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = settings.hour,
            initialMinute = settings.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.reminder_time_label)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                Button(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                    modifier = Modifier.testTag("confirm_time_picker_button")
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
