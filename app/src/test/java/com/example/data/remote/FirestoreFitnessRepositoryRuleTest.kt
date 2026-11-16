package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GoalCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreFitnessRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveGoal_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreFitnessRepository(firestore, auth)

        val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createGoal(
                title = "Morning Run",
                description = "Run 5km along the river",
                category = GoalCategory.CARDIO,
                targetValue = 30,
                unit = "min",
                quickIncrement = 10
            )
        }
        assertTrue(goalId.isNotEmpty())

        val goals = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGoals(uid).first { list -> list.any { it.id == goalId } }
        }
        val created = goals.first { it.id == goalId }
        assertEquals("Morning Run", created.title)
        assertEquals("Run 5km along the river", created.description)
        assertNotNull(created.createdAt)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.logProgress(goalId, 15, "Warmup pace")
        }
        val updatedGoals = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeGoals(uid).first { list ->
                list.any { it.id == goalId && it.currentProgress == 15L }
            }
        }
        assertEquals(15L, updatedGoals.first { it.id == goalId }.currentProgress)
    }

    @Test
    fun getGoalById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreFitnessRepository(firestore, auth)
        val goalId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createGoal(
                title = "Alice Hydration",
                category = GoalCategory.HYDRATION,
                targetValue = 8,
                unit = "glasses",
                quickIncrement = 1
            )
        }

        signInTestUser(BOB_EMAIL)
        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users")
                    .document(aliceUid)
                    .collection("fitness_goals")
                    .document(goalId)
                    .get()
                    .await()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertNotNull(captured)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    @Test
    fun observeGoals_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = FirestoreFitnessRepository(firestore, auth)

        var captured: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeGoals("some_user_id").first()
            }
        } catch (e: Exception) {
            captured = e.unwrapFirestoreException()
        }
        assertNotNull(captured)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, captured?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_fitness@test.com"
        const val BOB_EMAIL = "bob_fitness@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
