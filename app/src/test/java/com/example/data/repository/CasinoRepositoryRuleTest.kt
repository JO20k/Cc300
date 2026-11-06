package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.annotation.Config

@Config(sdk = [36], manifest = Config.NONE)
class CasinoRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndGetProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CasinoRepository(firestore)

        val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInitialProfile(username = "AliceWhale", tonBalance = 300.0)
        }
        assertTrue("Expected profile creation to succeed: ${createRes.exceptionOrNull()}", createRes.isSuccess)

        val fetchRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProfile(uid)
        }
        assertTrue(fetchRes.isSuccess)
        val profile = fetchRes.getOrNull()
        assertNotNull(profile)
        assertEquals("AliceWhale", profile?.username)
        assertEquals(300.0, profile?.tonBalance ?: 0.0, 0.01)
    }

    @Test
    fun recordAndObserveMessages_authenticatedOwner_emitsRealtimeMessages() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = CasinoRepository(firestore)

        val msgRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordMessage(
                sender = "BOT",
                commandOrText = "🎲 High Roller Dice rolled 6! You won 30 TON!",
                gameType = "EMOJI_DICE",
                emojiSymbol = "🎲",
                emojiValue = 6L,
                betAmount = 10.0,
                payoutAmount = 30.0,
                currency = "TON",
                outcome = "WIN",
                metadataSummary = "Roll 6/6 (3.0x)"
            )
        }
        assertTrue("Expected message creation to succeed: ${msgRes.exceptionOrNull()}", msgRes.isSuccess)
        val msgId = msgRes.getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeMessages(uid).first { list -> list.any { it.id == msgId } }
        }
        assertTrue(emitted.any { it.id == msgId })
    }

    @Test
    fun getProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CasinoRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createInitialProfile(username = "AliceWhale").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CasinoRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProfile(aliceUid)
        }
        assertTrue("Cross-user access must fail", crossResult.isFailure)
        val firestoreEx = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun observeMessages_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = CasinoRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeMessages(aliceUid).first()
            }
            fail("Expected observeMessages to fail when unauthenticated")
        } catch (e: Exception) {
            val firestoreEx = checkNotNull(e.unwrapFirestoreException()) {
                "Expected nested FirebaseFirestoreException, got $e"
            }
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_casino@test.com"
        const val BOB_EMAIL = "bob_casino@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
