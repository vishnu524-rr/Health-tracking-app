package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.DateKeyUtils
import com.example.data.model.GoalCategory
import com.example.data.model.GoalWithProgress
import com.example.ui.components.DailyOverviewRingCard
import com.example.ui.components.GoalProgressCard
import com.example.ui.viewmodel.PulseGoalUiState
import java.util.Locale

@Composable
fun TodayScreen(
    uiState: PulseGoalUiState,
    onSelectCategory: (GoalCategory?) -> Unit,
    onQuickIncrement: (GoalWithProgress) -> Unit,
    onOpenCustomLog: (GoalWithProgress) -> Unit,
    onGoalClick: (String) -> Unit,
    onOpenCreateGoal: () -> Unit,
    onLoadStarterGoals: () -> Unit,
    onOpenReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.reminderSettings
    val formattedReminderTime = String.format(
        Locale.getDefault(),
        "%d:%02d %s",
        if (settings.hour % 12 == 0) 12 else settings.hour % 12,
        settings.minute,
        if (settings.hour < 12) "AM" else "PM"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DateKeyUtils.formatHeaderDate().uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.today_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Surface(
                        onClick = onOpenReminders,
                        shape = MaterialTheme.shapes.small,
                        color = if (settings.isEnabled) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.testTag("reminder_status_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Daily reminder schedule",
                                modifier = Modifier.size(16.dp),
                                tint = if (settings.isEnabled) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = if (settings.isEnabled) formattedReminderTime else "Paused",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (settings.isEnabled) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = uiState.goals.isNotEmpty()) {
                    DailyOverviewRingCard(summary = uiState.weeklySummary)
                }
            }
        }

        if (uiState.goals.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategory == null,
                            onClick = { onSelectCategory(null) },
                            label = { Text(stringResource(R.string.filter_all)) },
                            modifier = Modifier.testTag("filter_chip_all")
                        )
                    }
                    items(GoalCategory.entries) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = {
                                onSelectCategory(
                                    if (uiState.selectedCategory == category) null else category
                                )
                            },
                            label = { Text(category.displayName) },
                            modifier = Modifier.testTag("filter_chip_${category.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (uiState.goals.isEmpty() && !uiState.isLoading) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .testTag("empty_goals_card"),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AsyncImage(
                            model = R.drawable.img_empty_goals,
                            contentDescription = "Fitness goals illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Text(
                            text = stringResource(R.string.empty_goals_title),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.empty_goals_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onLoadStarterGoals,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("load_starter_goals_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.add_starter_goals))
                        }
                        OutlinedButton(
                            onClick = onOpenCreateGoal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_custom_goal_empty_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.create_custom_goal))
                        }
                    }
                }
            }
        } else if (uiState.filteredGoals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No goals in this category",
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedButton(onClick = { onSelectCategory(null) }) {
                            Text("Show All Goals")
                        }
                    }
                }
            }
        } else {
            items(
                items = uiState.filteredGoals,
                key = { it.goal.id }
            ) { goalProgress ->
                GoalProgressCard(
                    item = goalProgress,
                    onQuickIncrement = { onQuickIncrement(goalProgress) },
                    onCustomLog = { onOpenCustomLog(goalProgress) },
                    onCardClick = { onGoalClick(goalProgress.goal.id) },
                    modifier = Modifier.widthIn(max = 720.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
