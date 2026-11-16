package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.DateKeyUtils
import com.example.data.model.GoalCategory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FirestoreFitnessRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeGoals(userId: String): Flow<List<FirestoreFitnessGoal>> = flow {
        val path = "users/$userId/fitness_goals"
        emitAll(
            db.collection("users").document(userId).collection("fitness_goals")
                .whereEqualTo("isArchived", false)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreFitnessGoal::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeLogs(userId: String): Flow<List<FirestoreGoalLog>> = flow {
        val path = "users/$userId/goal_logs"
        emitAll(
            db.collection("users").document(userId).collection("goal_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreGoalLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getGoalById(goalId: String): FirestoreFitnessGoal? {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("fitness_goals").document(goalId)
        return try {
            docRef.get().await().toObject(
                FirestoreFitnessGoal::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createGoal(
        title: String,
        description: String = "",
        category: GoalCategory,
        targetValue: Int,
        unit: String,
        quickIncrement: Int,
        startDate: Long = System.currentTimeMillis(),
        endDate: Long? = null
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("fitness_goals").document()
        val payload = buildMap<String, Any> {
            put("userId", uid)
            put("title", title.trim().take(80))
            val trimmedDesc = description.trim().take(300)
            if (trimmedDesc.isNotEmpty()) {
                put("description", trimmedDesc)
            }
            put("category", category.name)
            put("targetValue", targetValue.coerceAtLeast(1).toLong())
            put("currentProgress", 0L)
            put("unit", unit.trim().ifBlank { category.defaultUnit }.take(24))
            put("quickIncrement", quickIncrement.coerceAtLeast(1).toLong())
            put("startDate", startDate.coerceAtLeast(1L))
            if (endDate != null && endDate >= startDate) {
                put("endDate", endDate)
            }
            put("isArchived", false)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        try {
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateGoal(goal: FirestoreFitnessGoal) {
        val uid = requireUserId()
        require(goal.id.isNotBlank()) { "Goal ID must not be blank for update" }
        requireNotNull(goal.createdAt) { "Goal createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("fitness_goals").document(goal.id)
        try {
            docRef.set(goal.copy(userId = uid, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteGoal(goalId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("fitness_goals").document(goalId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun logProgress(goalId: String, amount: Int, note: String = "") {
        if (amount <= 0) return
        val uid = requireUserId()
        val todayKey = DateKeyUtils.todayKey()
        val logRef = db.collection("users").document(uid).collection("goal_logs").document()
        val logPayload = buildMap<String, Any> {
            put("userId", uid)
            put("goalId", goalId)
            put("amount", amount.toLong())
            put("dateKey", todayKey)
            val trimmedNote = note.trim().take(200)
            if (trimmedNote.isNotEmpty()) {
                put("note", trimmedNote)
            }
            put("createdAt", FieldValue.serverTimestamp())
        }

        try {
            logRef.set(logPayload).await()
            val goalRef = db.collection("users").document(uid).collection("fitness_goals").document(goalId)
            goalRef.update(
                mapOf(
                    "currentProgress" to FieldValue.increment(amount.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logRef.path)
            throw e
        }
    }

    suspend fun deleteLog(log: FirestoreGoalLog) {
        val uid = requireUserId()
        val logRef = db.collection("users").document(uid).collection("goal_logs").document(log.id)
        try {
            logRef.delete().await()
            if (log.dateKey == DateKeyUtils.todayKey()) {
                val todayLogs = db.collection("users").document(uid).collection("goal_logs")
                    .whereEqualTo("goalId", log.goalId)
                    .whereEqualTo("dateKey", log.dateKey)
                    .get()
                    .await()
                val newTotal = todayLogs.documents.sumOf { (it.getLong("amount") ?: 0L) }
                val goalRef = db.collection("users").document(uid).collection("fitness_goals").document(log.goalId)
                goalRef.update(
                    mapOf(
                        "currentProgress" to newTotal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, logRef.path)
            throw e
        }
    }

    suspend fun addStarterGoals() {
        val uid = requireUserId()
        val path = "users/$uid/fitness_goals"
        try {
            val existing = db.collection("users").document(uid).collection("fitness_goals")
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) return

            val now = System.currentTimeMillis()
            val thirtyDays = now + TimeUnit.DAYS.toMillis(30)
            createGoal("Daily Steps", "Hit 8,000 steps daily for steady cardiovascular endurance.", GoalCategory.STEPS, 8000, "steps", 1000, now, thirtyDays)
            createGoal("Cardio Session", "30 minutes of brisk running, cycling, or HIIT.", GoalCategory.CARDIO, 30, "min", 10, now, thirtyDays)
            createGoal("Hydration", "Drink 8 full glasses of water throughout the day.", GoalCategory.HYDRATION, 8, "glasses", 1, now, null)
            createGoal("Mobility & Stretch", "15 minutes of joint mobility and post-workout stretching.", GoalCategory.MOBILITY, 15, "min", 5, now, thirtyDays)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun getTodayPendingSummary(): Pair<Int, Int> {
        val uid = auth.currentUser?.uid ?: return 0 to 0
        val todayKey = DateKeyUtils.todayKey()
        return try {
            val goals = db.collection("users").document(uid).collection("fitness_goals")
                .whereEqualTo("isArchived", false)
                .get()
                .await()
                .toObjects(FirestoreFitnessGoal::class.java)
            if (goals.isEmpty()) return 0 to 0

            val todayLogs = db.collection("users").document(uid).collection("goal_logs")
                .whereEqualTo("dateKey", todayKey)
                .get()
                .await()
                .toObjects(FirestoreGoalLog::class.java)

            val totalsByGoal = todayLogs.groupBy { it.goalId }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
            val remaining = goals.count { goal ->
                (totalsByGoal[goal.id] ?: 0L) < goal.targetValue
            }
            remaining to goals.size
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$uid/fitness_goals")
            0 to 0
        }
    }
}
