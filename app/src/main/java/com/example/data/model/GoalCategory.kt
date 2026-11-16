package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.CardioColor
import com.example.ui.theme.HydrationColor
import com.example.ui.theme.MindfulnessColor
import com.example.ui.theme.MobilityColor
import com.example.ui.theme.StepsColor
import com.example.ui.theme.StrengthColor

enum class GoalCategory(
    val displayName: String,
    val defaultUnit: String,
    val defaultTarget: Int,
    val defaultIncrement: Int,
    val accentColor: Color,
    val icon: ImageVector
) {
    STEPS(
        displayName = "Steps",
        defaultUnit = "steps",
        defaultTarget = 8000,
        defaultIncrement = 1000,
        accentColor = StepsColor,
        icon = Icons.AutoMirrored.Filled.DirectionsWalk
    ),
    CARDIO(
        displayName = "Cardio",
        defaultUnit = "min",
        defaultTarget = 30,
        defaultIncrement = 10,
        accentColor = CardioColor,
        icon = Icons.AutoMirrored.Filled.DirectionsRun
    ),
    STRENGTH(
        displayName = "Strength",
        defaultUnit = "min",
        defaultTarget = 40,
        defaultIncrement = 15,
        accentColor = StrengthColor,
        icon = Icons.Filled.FitnessCenter
    ),
    HYDRATION(
        displayName = "Hydration",
        defaultUnit = "glasses",
        defaultTarget = 8,
        defaultIncrement = 1,
        accentColor = HydrationColor,
        icon = Icons.Filled.WaterDrop
    ),
    MOBILITY(
        displayName = "Mobility",
        defaultUnit = "min",
        defaultTarget = 15,
        defaultIncrement = 5,
        accentColor = MobilityColor,
        icon = Icons.Filled.SelfImprovement
    ),
    MINDFULNESS(
        displayName = "Recovery",
        defaultUnit = "min",
        defaultTarget = 15,
        defaultIncrement = 5,
        accentColor = MindfulnessColor,
        icon = Icons.Filled.Spa
    );

    companion object {
        fun fromString(value: String): GoalCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CARDIO
        }
    }
}
