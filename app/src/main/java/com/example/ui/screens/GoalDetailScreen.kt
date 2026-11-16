package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DateKeyUtils
import com.example.data.model.GoalCategory
import com.example.data.model.GoalWithProgress
import com.example.data.remote.FirestoreFitnessGoal
import com.example.data.remote.FirestoreGoalLog
import com.example.ui.components.CreateOrEditGoalDialog
import com.example.ui.components.CustomLogDialog
import com.example.ui.components.Goal7DayMiniChart
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalProgress: GoalWithProgress?,
    onBack: () -> Unit,
    onQuickIncrement: (String, Int) -> Unit,
    onCustomLog: (String, Int, String) -> Unit,
    onUpdateGoal: (FirestoreFitnessGoal) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onDeleteLog: (FirestoreGoalLog) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showEditDialog by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(goalProgress?.goal?.title ?: stringResource(R.string.goal_detail_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (goalProgress != null) {
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.testTag("edit_goal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.edit_goal)
                            )
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_goal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete_goal),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (goalProgress == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Goal not found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            val numberFmt = NumberFormat.getIntegerInstance()
            val accent = goalProgress.category.accentColor

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = goalProgress.category.icon,
                                        contentDescription = goalProgress.category.displayName,
                                        tint = accent
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = goalProgress.category.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = accent
                                    )
                                    Text(
                                        text = "${numberFmt.format(goalProgress.todayLogged)} / ${numberFmt.format(goalProgress.goal.targetValue)} ${goalProgress.goal.unit}",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                            }

                            if (goalProgress.goal.description.isNotBlank()) {
                                Text(
                                    text = goalProgress.goal.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val endLabel = goalProgress.goal.endDate?.let { DateKeyUtils.formatShortDate(it) } ?: "Ongoing"
                            Text(
                                text = "Schedule: ${DateKeyUtils.formatShortDate(goalProgress.goal.startDate)} – $endLabel",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatPill(
                                    label = "Current Streak",
                                    value = "${goalProgress.currentStreakDays}d",
                                    modifier = Modifier.weight(1f)
                                )
                                StatPill(
                                    label = "Best Streak",
                                    value = "${goalProgress.bestStreakDays}d",
                                    modifier = Modifier.weight(1f)
                                )
                                StatPill(
                                    label = "7d Total",
                                    value = numberFmt.format(goalProgress.weeklyTotal),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onQuickIncrement(goalProgress.goal.id, goalProgress.goal.quickIncrement.toInt())
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_quick_add_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("+${numberFmt.format(goalProgress.goal.quickIncrement)} ${goalProgress.goal.unit}")
                                }
                                Button(
                                    onClick = { showLogDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("detail_custom_log_button")
                                ) {
                                    Text("Custom Log")
                                }
                            }
                        }
                    }
                }

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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.last_7_days),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Goal7DayMiniChart(
                                days = goalProgress.last7Days,
                                accentColor = accent
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.recent_check_ins),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                    )
                }

                if (goalProgress.recentLogs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = stringResource(R.string.no_logs_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(
                        items = goalProgress.recentLogs,
                        key = { it.id }
                    ) { log ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "+${numberFmt.format(log.amount)} ${goalProgress.goal.unit}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    val timeMillis = log.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                                    val subtitle = buildString {
                                        append(log.dateKey)
                                        append(" • ")
                                        append(DateKeyUtils.formatLogTime(timeMillis))
                                        if (log.note.isNotBlank()) {
                                            append(" — ")
                                            append(log.note)
                                        }
                                    }
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteLog(log) },
                                    modifier = Modifier.testTag("delete_log_${log.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete check-in",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog && goalProgress != null) {
        CreateOrEditGoalDialog(
            initialGoal = goalProgress.goal,
            onDismiss = { showEditDialog = false },
            onConfirm = { title, description, category: GoalCategory, target, unit, increment, startDate, endDate ->
                onUpdateGoal(
                    goalProgress.goal.copy(
                        title = title,
                        description = description,
                        category = category.name,
                        targetValue = target.toLong(),
                        unit = unit,
                        quickIncrement = increment.toLong(),
                        startDate = startDate,
                        endDate = endDate
                    )
                )
                showEditDialog = false
            }
        )
    }

    if (showLogDialog && goalProgress != null) {
        CustomLogDialog(
            goalTitle = goalProgress.goal.title,
            unit = goalProgress.goal.unit,
            defaultAmount = goalProgress.goal.quickIncrement.toInt(),
            onDismiss = { showLogDialog = false },
            onConfirm = { amount, note ->
                onCustomLog(goalProgress.goal.id, amount, note)
                showLogDialog = false
            }
        )
    }

    if (showDeleteConfirm && goalProgress != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_goal_confirm_title)) },
            text = { Text(stringResource(R.string.delete_goal_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteGoal(goalProgress.goal.id)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_goal_button")
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
