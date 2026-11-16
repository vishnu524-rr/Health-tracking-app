package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.FitnessGoal
import com.example.data.model.GoalCategory
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateOrEditGoalDialog(
    initialGoal: FitnessGoal? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        category: GoalCategory,
        target: Int,
        unit: String,
        increment: Int,
        startDate: Long,
        endDate: Long?
    ) -> Unit
) {
    val initialCategory = initialGoal?.let { GoalCategory.fromString(it.category) } ?: GoalCategory.CARDIO
    var title by remember { mutableStateOf(initialGoal?.title ?: "") }
    var description by remember { mutableStateOf(initialGoal?.description ?: "") }
    var category by remember { mutableStateOf(initialCategory) }
    var targetText by remember { mutableStateOf((initialGoal?.targetValue ?: initialCategory.defaultTarget).toString()) }
    var unit by remember { mutableStateOf(initialGoal?.unit ?: initialCategory.defaultUnit) }
    var incrementText by remember { mutableStateOf((initialGoal?.quickIncrement ?: initialCategory.defaultIncrement).toString()) }
    val initialDurationDays = remember(initialGoal) {
        val end = initialGoal?.endDate
        val start = initialGoal?.startDate
        if (end == null || start == null) {
            if (initialGoal == null) 30 else 0
        } else {
            val diff = TimeUnit.MILLISECONDS.toDays((end - start).coerceAtLeast(0L)).toInt()
            when {
                diff <= 10 -> 7
                diff <= 45 -> 30
                else -> 90
            }
        }
    }
    var durationDays by remember { mutableIntStateOf(initialDurationDays) } // 0 = Ongoing
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialGoal == null) {
                    stringResource(R.string.dialog_new_goal_title)
                } else {
                    stringResource(R.string.dialog_edit_goal_title)
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text(stringResource(R.string.goal_title_label)) },
                    placeholder = { Text(stringResource(R.string.goal_title_hint)) },
                    singleLine = true,
                    isError = titleError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Goal Description (Optional)") },
                    placeholder = { Text("e.g. Build aerobic base 5 days a week") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_description_input")
                )

                Text(
                    text = stringResource(R.string.goal_category_label),
                    style = MaterialTheme.typography.labelLarge
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GoalCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                if (initialGoal == null) {
                                    targetText = cat.defaultTarget.toString()
                                    unit = cat.defaultUnit
                                    incrementText = cat.defaultIncrement.toString()
                                }
                            },
                            label = { Text(cat.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.goal_target_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_target_input")
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(stringResource(R.string.goal_unit_label)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_unit_input")
                    )
                }

                OutlinedTextField(
                    value = incrementText,
                    onValueChange = { incrementText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.goal_increment_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_increment_input")
                )

                Text(
                    text = "Goal Duration",
                    style = MaterialTheme.typography.labelLarge
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        7 to "7 Days",
                        30 to "30 Days",
                        90 to "90 Days",
                        0 to "Ongoing"
                    ).forEach { (days, label) ->
                        FilterChip(
                            selected = durationDays == days,
                            onClick = { durationDays = days },
                            label = { Text(label) },
                            modifier = Modifier.testTag("duration_chip_$days")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = title.trim()
                    if (trimmed.isEmpty()) {
                        titleError = true
                        return@Button
                    }
                    val target = targetText.toIntOrNull()?.coerceAtLeast(1) ?: category.defaultTarget
                    val increment = incrementText.toIntOrNull()?.coerceAtLeast(1) ?: category.defaultIncrement
                    val finalUnit = unit.trim().ifBlank { category.defaultUnit }
                    val startDate = initialGoal?.startDate ?: System.currentTimeMillis()
                    val endDate = if (durationDays > 0) {
                        startDate + TimeUnit.DAYS.toMillis(durationDays.toLong())
                    } else {
                        null
                    }
                    onConfirm(
                        trimmed,
                        description.trim(),
                        category,
                        target,
                        finalUnit,
                        increment,
                        startDate,
                        endDate
                    )
                },
                modifier = Modifier.testTag("save_goal_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_goal_button")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun CustomLogDialog(
    goalTitle: String,
    unit: String,
    defaultAmount: Int,
    onDismiss: () -> Unit,
    onConfirm: (amount: Int, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf(defaultAmount.toString()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${stringResource(R.string.dialog_log_title)}: $goalTitle",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.log_amount_label, unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("log_amount_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.log_note_label)) },
                    placeholder = { Text(stringResource(R.string.log_note_hint)) },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("log_note_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toIntOrNull() ?: 0
                    if (amount > 0) {
                        onConfirm(amount, note)
                    }
                },
                modifier = Modifier.testTag("confirm_log_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_log_button")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
