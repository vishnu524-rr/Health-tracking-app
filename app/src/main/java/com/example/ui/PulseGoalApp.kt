package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.model.GoalWithProgress
import com.example.notifications.NotificationHelper
import com.example.ui.components.CreateOrEditGoalDialog
import com.example.ui.components.CustomLogDialog
import com.example.ui.screens.GoalDetailScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.viewmodel.FitnessViewModel

const val ROUTE_TODAY = "today"
const val ROUTE_INSIGHTS = "insights"
const val ROUTE_REMINDERS = "reminders"
const val ROUTE_GOAL_DETAIL = "goal_detail/{goalId}"

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val topLevelDestinations = listOf(
    NavDestinationItem(
        route = ROUTE_TODAY,
        labelRes = R.string.nav_today,
        selectedIcon = Icons.Filled.Flag,
        unselectedIcon = Icons.Outlined.Flag,
        testTag = "nav_tab_today"
    ),
    NavDestinationItem(
        route = ROUTE_INSIGHTS,
        labelRes = R.string.nav_insights,
        selectedIcon = Icons.Filled.Insights,
        unselectedIcon = Icons.Outlined.Insights,
        testTag = "nav_tab_insights"
    ),
    NavDestinationItem(
        route = ROUTE_REMINDERS,
        labelRes = R.string.nav_reminders,
        selectedIcon = Icons.Filled.NotificationsActive,
        unselectedIcon = Icons.Outlined.Notifications,
        testTag = "nav_tab_reminders"
    )
)

@Composable
fun PulseGoalApp(
    viewModel: FitnessViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_TODAY
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var customLogGoal by remember { mutableStateOf<GoalWithProgress?>(null) }

    // Prompt once on Android 13+ if notification permission has not yet been requested
    val startupPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasNotificationPermission(context)
        ) {
            startupPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    val isTopLevel = currentRoute in topLevelDestinations.map { it.route }

    fun navigateToTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail && isTopLevel) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        FloatingActionButton(
                            onClick = { showCreateGoalDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .testTag("rail_new_goal_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.new_goal_fab)
                            )
                        }
                    }
                ) {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(dest.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = stringResource(dest.labelRes)
                                )
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            modifier = Modifier.testTag(dest.testTag)
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useRail && isTopLevel) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            topLevelDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(dest.labelRes)) },
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (!useRail && currentRoute == ROUTE_TODAY) {
                        ExtendedFloatingActionButton(
                            onClick = { showCreateGoalDialog = true },
                            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            text = { Text(stringResource(R.string.new_goal_fab)) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("new_goal_fab")
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_TODAY,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(ROUTE_TODAY) {
                        TodayScreen(
                            uiState = uiState,
                            onSelectCategory = viewModel::selectCategory,
                            onQuickIncrement = { goalProgress ->
                                viewModel.logProgress(
                                    goalId = goalProgress.goal.id,
                                    amount = goalProgress.goal.quickIncrement
                                )
                            },
                            onOpenCustomLog = { customLogGoal = it },
                            onGoalClick = { goalId ->
                                navController.navigate("goal_detail/$goalId")
                            },
                            onOpenCreateGoal = { showCreateGoalDialog = true },
                            onLoadStarterGoals = viewModel::loadStarterGoals,
                            onOpenReminders = { navigateToTopLevel(ROUTE_REMINDERS) }
                        )
                    }

                    composable(ROUTE_INSIGHTS) {
                        InsightsScreen(uiState = uiState)
                    }

                    composable(ROUTE_REMINDERS) {
                        RemindersScreen(
                            uiState = uiState,
                            onToggleEnabled = viewModel::updateReminderEnabled,
                            onTimeSelected = viewModel::updateReminderTime,
                            onToggleDay = viewModel::toggleReminderDay,
                            onCustomMessageChange = viewModel::updateCustomReminderMessage,
                            onIncludeGoalSummaryChange = viewModel::updateIncludeGoalSummary,
                            onSendTestNotification = viewModel::sendTestNotification
                        )
                    }

                    composable(
                        route = ROUTE_GOAL_DETAIL,
                        arguments = listOf(navArgument("goalId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val goalId = backStackEntry.arguments?.getLong("goalId") ?: -1L
                        val goalProgress = uiState.goals.firstOrNull { it.goal.id == goalId }
                        GoalDetailScreen(
                            goalProgress = goalProgress,
                            onBack = { navController.popBackStack() },
                            onQuickIncrement = { id, amount -> viewModel.logProgress(id, amount) },
                            onCustomLog = { id, amount, note -> viewModel.logProgress(id, amount, note) },
                            onUpdateGoal = viewModel::updateGoal,
                            onDeleteGoal = viewModel::deleteGoal,
                            onDeleteLog = viewModel::deleteLog
                        )
                    }
                }
            }
        }
    }

    if (showCreateGoalDialog) {
        CreateOrEditGoalDialog(
            onDismiss = { showCreateGoalDialog = false },
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
            }
        )
    }

    customLogGoal?.let { targetGoal ->
        CustomLogDialog(
            goalTitle = targetGoal.goal.title,
            unit = targetGoal.goal.unit,
            defaultAmount = targetGoal.goal.quickIncrement,
            onDismiss = { customLogGoal = null },
            onConfirm = { amount, note ->
                viewModel.logProgress(targetGoal.goal.id, amount, note)
                customLogGoal = null
            }
        )
    }
}
