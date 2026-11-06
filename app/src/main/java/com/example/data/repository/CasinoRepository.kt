package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BotChatMessage
import com.example.data.model.CryptoTransaction
import com.example.data.model.PlayerProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class CasinoRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before writing to Firestore.")
    }

    fun observeProfile(userId: String): Flow<PlayerProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        PlayerProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception) ?: RuntimeException(error)
                handleFirestoreError(exceptionToLog, OperationType.GET, path)
                throw error
            }
    }

    fun observeMessages(userId: String): Flow<List<BotChatMessage>> {
        val path = "users/$userId/messages"
        return db.collection("users").document(userId).collection("messages")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    BotChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception) ?: RuntimeException(error)
                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                throw error
            }
    }

    fun observeTransactions(userId: String): Flow<List<CryptoTransaction>> {
        val path = "users/$userId/transactions"
        return db.collection("users").document(userId).collection("transactions")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CryptoTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception) ?: RuntimeException(error)
                handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getProfile(userId: String): Result<PlayerProfile?> = runCatching {
        val docRef = db.collection("users").document(userId)
        try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.toObject(
                    PlayerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun createInitialProfile(
        username: String = "TelePlayer",
        tonBalance: Double = 250.0,
        usdtBalance: Double = 1000.0,
        btcBalance: Double = 0.05
    ): Result<PlayerProfile> = runCatching {
        val uid = requireUserId()
        val sanitizedName = username.trim().take(36).ifBlank { "TelePlayer" }
            .let { if (it.length < 2) "${it}X" else it }
        val docRef = db.collection("users").document(uid)
        val seedHash = sha256Hex("televegas_server_seed_$uid")
        val payload = mapOf(
            "userId" to uid,
            "username" to sanitizedName,
            "tonBalance" to tonBalance,
            "usdtBalance" to usdtBalance,
            "btcBalance" to btcBalance,
            "activeCurrency" to "TON",
            "totalWagered" to 0.0,
            "totalWon" to 0.0,
            "gamesPlayed" to 0L,
            "vipLevel" to 1L,
            "botChatId" to "",
            "serverSeedHash" to seedHash,
            "clientSeed" to "televegas_client_1",
            "nonce" to 1L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mapOf(
                    "username" to sanitizedName,
                    "tonBalance" to tonBalance,
                    "usdtBalance" to usdtBalance,
                    "btcBalance" to btcBalance,
                    "activeCurrency" to "TON",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                docRef.set(payload).await()
            }
            PlayerProfile(
                id = uid,
                userId = uid,
                username = sanitizedName,
                tonBalance = tonBalance,
                usdtBalance = usdtBalance,
                btcBalance = btcBalance,
                activeCurrency = "TON",
                serverSeedHash = seedHash
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProfile(profile: PlayerProfile): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val sanitizedName = profile.username.trim().take(36).ifBlank { "TelePlayer" }
            .let { if (it.length < 2) "${it}X" else it }
        val updates = mapOf(
            "username" to sanitizedName,
            "tonBalance" to profile.tonBalance.coerceAtLeast(0.0),
            "usdtBalance" to profile.usdtBalance.coerceAtLeast(0.0),
            "btcBalance" to profile.btcBalance.coerceAtLeast(0.0),
            "activeCurrency" to profile.activeCurrency,
            "totalWagered" to profile.totalWagered.coerceAtLeast(0.0),
            "totalWon" to profile.totalWon.coerceAtLeast(0.0),
            "gamesPlayed" to profile.gamesPlayed.coerceAtLeast(0L),
            "vipLevel" to profile.vipLevel.coerceIn(1L, 10L),
            "botChatId" to profile.botChatId.take(64),
            "serverSeedHash" to profile.serverSeedHash.take(128).ifBlank { sha256Hex(uid) },
            "clientSeed" to profile.clientSeed.take(64).ifBlank { "seed_1" },
            "nonce" to profile.nonce.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun recordMessage(
        sender: String,
        commandOrText: String,
        gameType: String = "NONE",
        emojiSymbol: String = "💬",
        emojiValue: Long = 0L,
        betAmount: Double = 0.0,
        payoutAmount: Double = 0.0,
        currency: String = "TON",
        outcome: String = "INFO",
        metadataSummary: String = ""
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "")}"
        val docRef = db.collection("users").document(uid).collection("messages").document(msgId)
        val payload = mapOf(
            "userId" to uid,
            "sender" to sender,
            "commandOrText" to commandOrText.take(1000).ifBlank { "..." },
            "gameType" to gameType,
            "emojiSymbol" to emojiSymbol.take(32),
            "emojiValue" to emojiValue.coerceIn(0L, 64L),
            "betAmount" to betAmount.coerceAtLeast(0.0),
            "payoutAmount" to payoutAmount.coerceAtLeast(0.0),
            "currency" to currency,
            "outcome" to outcome,
            "metadataSummary" to metadataSummary.take(500),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            msgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun recordTransaction(
        txType: String,
        currency: String,
        amount: Double,
        txHash: String,
        addressOrNote: String,
        status: String = "CONFIRMED"
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val txId = "tx_${UUID.randomUUID().toString().replace("-", "")}"
        val docRef = db.collection("users").document(uid).collection("transactions").document(txId)
        val payload = mapOf(
            "userId" to uid,
            "txType" to txType,
            "currency" to currency,
            "amount" to amount.coerceAtLeast(0.0),
            "txHash" to txHash.take(128).padEnd(4, '0'),
            "addressOrNote" to addressOrNote.take(200).ifBlank { "Casino Ledger" },
            "status" to status,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            txId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun clearChatHistory(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("messages")
        try {
            val docs = colRef.whereEqualTo("userId", uid).get().await()
            for (doc in docs.documents) {
                doc.reference.delete().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, colRef.path)
            throw e
        }
    }

    companion object {
        fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
