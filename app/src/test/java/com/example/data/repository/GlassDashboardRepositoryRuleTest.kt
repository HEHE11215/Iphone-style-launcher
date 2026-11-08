package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CustomIconPack
import com.example.data.model.GlassThemeMode
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GlassDashboardRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveTask_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = GlassDashboardRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                title = "Optimize 90Hz Glass Shader",
                notes = "Keep overdraw minimal",
                category = TaskCategory.FOCUS,
                priority = TaskPriority.HIGH,
                sortOrder = 0,
                reminderLabel = "10:00 AM",
                reminderEnabled = true
            )
        }
        assertTrue(createResult.isSuccess)
        val taskId = createResult.getOrThrow()
        assertTrue(taskId.isNotEmpty())

        val tasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks(aliceUid).first { list -> list.any { it.id == taskId } }
        }
        assertTrue(tasks.any { it.id == taskId && it.title == "Optimize 90Hz Glass Shader" })
    }

    @Test
    fun savePreferences_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = GlassDashboardRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.savePreferences(
                blurIntensity = 75,
                themeMode = GlassThemeMode.AURORA_DYNAMIC,
                iconPack = CustomIconPack.NEON_GLYPH,
                parallaxEnabled = true,
                city = "Tokyo"
            )
        }
        assertTrue(saveResult.isSuccess)

        val prefs = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePreferences(aliceUid).first { it.city == "Tokyo" }
        }
        assertEquals(75L, prefs.blurIntensity)
        assertEquals(GlassThemeMode.AURORA_DYNAMIC.name, prefs.themeMode)
    }

    @Test
    fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GlassDashboardRepository(firestore)
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTask(title = "Alice Secret Task").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = GlassDashboardRepository(firestore)
        val bobResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getTaskById(aliceUid, taskId)
        }
        assertTrue(bobResult.isFailure)
        val firestoreEx = bobResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = GlassDashboardRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks(aliceUid).first()
            }
            fail("Expected exception for unauthenticated observeTasks")
        } catch (e: Exception) {
            val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@aeroglass.test"
        const val BOB_EMAIL = "bob@aeroglass.test"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
