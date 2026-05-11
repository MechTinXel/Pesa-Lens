package com.example.pesalens.data

import com.example.pesalens.NetworkProvider
import com.example.pesalens.PesaTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Firebase Cloud Sync Service
 * Handles authentication and data synchronization with Firebase
 */
class FirebaseSyncService {

    private val auth by lazy { 
        try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (e: Exception) { null }
    }
    private val firestore by lazy { 
        try { com.google.firebase.firestore.FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }

    // Authentication state
    val isAuthenticated: Boolean
        get() = auth?.currentUser != null

    val currentUserId: String?
        get() = auth?.currentUser?.uid

    // Anonymous authentication for basic sync
    suspend fun signInAnonymously(): Result<String> {
        val auth = auth ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            val result = auth.signInAnonymously().await()
            Result.success(result.user?.uid ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sync transaction data to cloud
    suspend fun syncTransactions(transactions: List<PesaTransaction>): Result<Unit> {
        val firestore = firestore ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("Not authenticated"))

            // Convert transactions to map for Firestore
            val transactionMaps = transactions.map { transaction ->
                mapOf(
                    "id" to (transaction.reference ?: ""),
                    "name" to transaction.name,
                    "amount" to transaction.amount,
                    "fee" to transaction.fee,
                    "type" to transaction.type,
                    "provider" to transaction.provider.name,
                    "date" to transaction.date,
                    "rawMessage" to transaction.rawMessage,
                    "timestamp" to System.currentTimeMillis()
                )
            }

            // Batch write to Firestore
            val batch = firestore.batch()
            val collectionRef = firestore.collection("users").document(userId).collection("transactions")

            // Clear existing data and add new
            val existingDocs = collectionRef.get().await()
            existingDocs.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            transactionMaps.forEach { transactionMap ->
                val docRef = collectionRef.document()
                batch.set(docRef, transactionMap)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get synced transactions from cloud
    fun getSyncedTransactions(): Flow<List<PesaTransaction>> = flow {
        val firestore = firestore ?: return@flow
        try {
            val userId = currentUserId ?: return@flow

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val transactions = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    PesaTransaction(
                        name = data["name"] as? String ?: "Unknown",
                        amount = (data["amount"] as? Double) ?: 0.0,
                        fee = (data["fee"] as? Double) ?: 0.0,
                        type = data["type"] as? String ?: "Unknown",
                        provider = NetworkProvider.valueOf(data["provider"] as? String ?: "MPESA"),
                        date = (data["date"] as? Long) ?: 0L,
                        reference = data["id"] as? String ?: "",
                        rawMessage = data["rawMessage"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }

            emit(transactions)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // Sync user settings
    suspend fun syncSettings(settings: Map<String, Any>): Result<Unit> {
        val firestore = firestore ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("Not authenticated"))

            firestore.collection("users")
                .document(userId)
                .collection("settings")
                .document("preferences")
                .set(settings, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get synced settings
    suspend fun getSyncedSettings(): Result<Map<String, Any>> {
        val firestore = firestore ?: return Result.failure(Exception("Firebase not initialized"))
        return try {
            val userId = currentUserId ?: return Result.failure(Exception("Not authenticated"))

            val doc = firestore.collection("users")
                .document(userId)
                .collection("settings")
                .document("preferences")
                .get()
                .await()

            val data = doc.data ?: emptyMap()
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sign out
    fun signOut() {
        auth?.signOut()
    }
}
