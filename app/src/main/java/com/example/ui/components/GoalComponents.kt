package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DayCompletionRate
import com.example.data.model.DayTotal
import com.example.data.model.GoalWithProgress
import com.example.data.model.WeeklySummary
import java.text.NumberFormat

@Composable
fun DailyOverviewRingCard(
    summary: WeeklySummary,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = summary.dailyOverallFraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "daily_ring_progress"
    )
    val percent = (animatedProgress * 100f).toInt()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_overview_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.10f),
                            secondaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier.size(108.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(108.dp)) {
                        val strokeWidth = 12.dp.toPx()
                        val inset = strokeWidth / 2f
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        if (animatedProgress > 0f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(primaryColor, secondaryColor, primaryColor)
                                ),
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.daily_progress_label),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            R.string.goals_completed_format,
                            summary.completedTodayCount,
                            summary.totalActiveGoals
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.best_streak_format, summary.bestActiveStreak),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalProgressCard(
    item: GoalWithProgress,
    onQuickIncrement: () -> Unit,
    onCustomLog: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFmt = NumberFormat.getIntegerInstance()
    val animatedFraction by animateFloatAsState(
        targetValue = item.progressFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "goal_progress_${item.goal.id}"
    )
    val accent = item.category.accentColor

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onCardClick)
            .testTag("goal_card_${item.goal.id}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isCompletedToday) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            text = item.goal.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.isCompletedToday) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Goal completed today",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "${numberFmt.format(item.todayLogged)} / ${numberFmt.format(item.goal.targetValue)} ${item.goal.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (item.currentStreakDays > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Current streak",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${item.currentStreakDays}d",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Custom animated pill progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (item.isCompletedToday) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                accent
                            }
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.isCompletedToday) {
                        "Target reached today"
                    } else {
                        "${numberFmt.format(item.remainingToday)} ${item.goal.unit} to go"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.isCompletedToday) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onCustomLog,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("custom_log_button_${item.goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.quick_log_button))
                    }

                    Button(
                        onClick = onQuickIncrement,
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("quick_add_button_${item.goal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+${numberFmt.format(item.goal.quickIncrement)}")
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyCompletionBarChart(
    rates: List<DayCompletionRate>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (rates.isEmpty()) return@Canvas
            val count = rates.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.44f).coerceAtMost(28.dp.toPx())
            val maxBarHeight = size.height - 8.dp.toPx()

            rates.forEachIndexed { index, day ->
                val centerX = slotWidth * index + slotWidth / 2f
                val left = centerX - barWidth / 2f

                // Track background pill
                drawRoundRect(
                    color = track,
                    topLeft = Offset(left, 4.dp.toPx()),
                    size = Size(barWidth, maxBarHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )

                val filledHeight = (maxBarHeight * day.fraction).coerceAtLeast(
                    if (day.completedCount > 0) 10.dp.toPx() else 0f
                )
                if (filledHeight > 0f) {
                    val topY = 4.dp.toPx() + (maxBarHeight - filledHeight)
                    drawRoundRect(
                        color = if (day.fraction >= 1f) secondary else primary,
                        topLeft = Offset(left, topY),
                        size = Size(barWidth, filledHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            rates.forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = day.shortDayLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${day.completedCount}/${day.totalGoals}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun Goal7DayMiniChart(
    days: List<DayTotal>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            if (days.isEmpty()) return@Canvas
            val count = days.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.42f).coerceAtMost(24.dp.toPx())
            val maxBarHeight = size.height - 6.dp.toPx()

            days.forEachIndexed { index, day ->
                val centerX = slotWidth * index + slotWidth / 2f
                val left = centerX - barWidth / 2f
                drawRoundRect(
                    color = track,
                    topLeft = Offset(left, 3.dp.toPx()),
                    size = Size(barWidth, maxBarHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )

                val filledHeight = (maxBarHeight * day.progressFraction).coerceAtLeast(
                    if (day.amount > 0) 8.dp.toPx() else 0f
                )
                if (filledHeight > 0f) {
                    val topY = 3.dp.toPx() + (maxBarHeight - filledHeight)
                    drawRoundRect(
                        color = if (day.progressFraction >= 1f) secondary else accentColor,
                        topLeft = Offset(left, topY),
                        size = Size(barWidth, filledHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            days.forEach { day ->
                Text(
                    text = day.shortDayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
