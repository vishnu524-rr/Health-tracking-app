package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.example.R
import com.example.data.local.FitnessGoal
import com.example.data.local.PulseGoalDatabase
import com.example.data.model.DateKeyUtils
import com.example.data.model.GoalCategory
import com.example.data.remote.FirestoreFitnessGoal
import com.example.data.repository.FitnessRepository
import com.example.ui.components.CreateOrEditGoalDialog
import com.example.ui.components.CustomLogDialog
import com.example.ui.viewmodel.RoomDashboardUiState
import com.example.ui.viewmodel.RoomDashboardViewModel
import com.example.ui.viewmodel.RoomGoalDashboardItem
import java.text.NumberFormat

@Composable
fun RoomDashboardRoute(
    isStandalone: Boolean = false,
    onRequestCloudSignIn: (() -> Unit)? = null,
    externalShowCreateGoal: Boolean = false,
    onDismissExternalCreateGoal: () -> Unit = {},
    onShowSnackbar: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: RoomDashboardViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val db = PulseGoalDatabase.getInstance(app)
                val repo = FitnessRepository(db.fitnessGoalDao(), db.fitnessDao())
                RoomDashboardViewModel(repo)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val localSnackbarHostState = remember { SnackbarHostState() }

    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<FitnessGoal?>(null) }
    var customLogGoal by remember { mutableStateOf<FitnessGoal?>(null) }
    var pendingDeleteGoal by remember { mutableStateOf<FitnessGoal?>(null) }

    if (isStandalone && onRequestCloudSignIn != null) {
        BackHandler(onBack = onRequestCloudSignIn)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            if (onShowSnackbar != null) {
                onShowSnackbar(message)
            } else {
                localSnackbarHostState.showSnackbar(message)
            }
            viewModel.clearSnackbar()
        }
    }

    val content: @Composable (Modifier) -> Unit = { contentModifier ->
        RoomDashboardScreen(
            uiState = uiState,
            onSelectCategory = viewModel::selectCategory,
            onQuickIncrement = { goal ->
                viewModel.incrementProgress(goal.id, goal.quickIncrement)
            },
            onOpenCustomLog = { customLogGoal = it },
            onEditGoal = { editingGoal = it },
            onDeleteGoal = { pendingDeleteGoal = it },
            onOpenCreateGoal = { showCreateGoalDialog = true },
            onLoadStarterGoals = viewModel::loadStarterGoals,
            onRequestCloudSignIn = onRequestCloudSignIn,
            modifier = contentModifier
        )
    }

    if (isStandalone) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = localSnackbarHostState) },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showCreateGoalDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_goal_fab)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("room_new_goal_fab")
                )
            }
        ) { innerPadding ->
            content(Modifier.padding(innerPadding))
        }
    } else {
        content(modifier)
    }

    if (showCreateGoalDialog || externalShowCreateGoal) {
        CreateOrEditGoalDialog(
            onDismiss = {
                showCreateGoalDialog = false
                onDismissExternalCreateGoal()
            },
            onConfirm = { title, description, category, target, unit, increment, startDate, endDate ->
                viewModel.createGoal(
                    title = title,
                    description = description,
                    category = category,
                    targetValue = target,
                    unit = unit,
                    quickIncrement = increment,
                    startDate = startDate,
                    endDate = endDate
                )
                showCreateGoalDialog = false
                onDismissExternalCreateGoal()
            }
        )
    }

    editingGoal?.let { targetGoal ->
        val mapped = FirestoreFitnessGoal(
            id = targetGoal.id.toString(),
            title = targetGoal.title,
            description = targetGoal.description,
            category = targetGoal.category,
            targetValue = targetGoal.targetValue.toLong(),
            currentProgress = targetGoal.currentProgress.toLong(),
            unit = targetGoal.unit,
            quickIncrement = targetGoal.quickIncrement.toLong(),
            startDate = targetGoal.startDate,
            endDate = targetGoal.endDate
        )
        CreateOrEditGoalDialog(
            initialGoal = mapped,
            onDismiss = { editingGoal = null },
            onConfirm = { title, description, category, target, unit, increment, startDate, endDate ->
                viewModel.updateGoalDetails(
                    goalId = targetGoal.id,
                    title = title,
                    description = description,
                    category = category,
                    targetValue = target,
                    unit = unit,
                    quickIncrement = increment,
                    startDate = startDate,
                    endDate = endDate
                )
                editingGoal = null
            }
        )
    }

    customLogGoal?.let { targetGoal ->
        CustomLogDialog(
            goalTitle = targetGoal.title,
            unit = targetGoal.unit,
            defaultAmount = targetGoal.quickIncrement,
            onDismiss = { customLogGoal = null },
            onConfirm = { amount, note ->
                viewModel.incrementProgress(targetGoal.id, amount, note)
                customLogGoal = null
            }
        )
    }

    pendingDeleteGoal?.let { targetGoal ->
        AlertDialog(
            onDismissRequest = { pendingDeleteGoal = null },
            title = { Text(stringResource(R.string.delete_goal_confirm_title)) },
            text = { Text(stringResource(R.string.delete_goal_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(targetGoal.id)
                        pendingDeleteGoal = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("room_confirm_delete_goal_button")
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteGoal = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun RoomDashboardScreen(
    uiState: RoomDashboardUiState,
    onSelectCategory: (GoalCategory?) -> Unit,
    onQuickIncrement: (FitnessGoal) -> Unit,
    onOpenCustomLog: (FitnessGoal) -> Unit,
    onEditGoal: (FitnessGoal) -> Unit,
    onDeleteGoal: (FitnessGoal) -> Unit,
    onOpenCreateGoal: () -> Unit,
    onLoadStarterGoals: () -> Unit,
    onRequestCloudSignIn: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("room_goals_list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .testTag("room_dashboard_screen"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Storage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.room_dashboard_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.room_dashboard_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.room_dashboard_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (onRequestCloudSignIn != null) {
                        FilledTonalButton(
                            onClick = onRequestCloudSignIn,
                            modifier = Modifier.testTag("room_cloud_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudSync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.room_cloud_sync_action))
                        }
                    }
                }

                AnimatedVisibility(visible = uiState.goals.isNotEmpty()) {
                    RoomOverallProgressCard(uiState = uiState)
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
                            modifier = Modifier.testTag("room_filter_chip_all")
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
                            modifier = Modifier.testTag("room_filter_chip_${category.name.lowercase()}")
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.testTag("room_dashboard_loading"))
                }
            }
        } else if (uiState.goals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .testTag("room_empty_goals_card"),
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
                                .height(180.dp)
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
                                .testTag("room_load_starter_button")
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
                                .testTag("room_create_goal_empty_button")
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
            ) { item ->
                RoomGoalProgressCard(
                    item = item,
                    onQuickIncrement = { onQuickIncrement(item.goal) },
                    onOpenCustomLog = { onOpenCustomLog(item.goal) },
                    onEdit = { onEditGoal(item.goal) },
                    onDelete = { onDeleteGoal(item.goal) },
                    modifier = Modifier.widthIn(max = 720.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun RoomOverallProgressCard(
    uiState: RoomDashboardUiState,
    modifier: Modifier = Modifier
) {
    val animatedOverall by animateFloatAsState(
        targetValue = uiState.overallProgressFraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "room_overall_progress"
    )
    val percent = (animatedOverall * 100f).toInt().coerceIn(0, 100)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("room_overall_summary_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.09f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.07f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.room_overall_progress),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            R.string.goals_completed_format,
                            uiState.completedCount,
                            uiState.totalCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { animatedOverall },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .testTag("room_overall_progress_bar"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun RoomGoalProgressCard(
    item: RoomGoalDashboardItem,
    onQuickIncrement: () -> Unit,
    onOpenCustomLog: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goal = item.goal
    val numberFmt = NumberFormat.getIntegerInstance()
    val animatedFraction by animateFloatAsState(
        targetValue = item.progressFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "room_goal_progress_${goal.id}"
    )
    val accent = item.category.accentColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("room_goal_card_${goal.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isCompleted) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.category.icon,
                        contentDescription = item.category.displayName,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Goal completed",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "${numberFmt.format(goal.currentProgress)} / ${numberFmt.format(goal.targetValue)} ${goal.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (item.isCompleted) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = "${item.percentComplete}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isCompleted) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (goal.description.isNotBlank()) {
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .testTag("room_goal_progress_bar_${goal.id}"),
                color = if (item.isCompleted) MaterialTheme.colorScheme.secondary else accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            val endLabel = goal.endDate?.let { DateKeyUtils.formatShortDate(it) } ?: "Ongoing"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "${DateKeyUtils.formatShortDate(goal.startDate)} – $endLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("room_edit_goal_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit_goal),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("room_delete_goal_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_goal),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenCustomLog,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("room_custom_log_${goal.id}")
                    ) {
                        Text(text = stringResource(R.string.quick_log_button))
                    }

                    Button(
                        onClick = onQuickIncrement,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("room_quick_add_${goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+${numberFmt.format(goal.quickIncrement)}")
                    }
                }
            }
        }
    }
}
