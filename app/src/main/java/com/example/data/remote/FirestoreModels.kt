package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreFitnessGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "CARDIO",
    val targetValue: Long = 30L,
    val currentProgress: Long = 0L,
    val unit: String = "min",
    val quickIncrement: Long = 10L,
    val startDate: Long = 0L,
    val endDate: Long? = null,
    val isArchived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreGoalLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val goalId: String = "",
    val amount: Long = 0L,
    val dateKey: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
