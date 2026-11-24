package com.example.data

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.model.AssistantReminder
import com.example.model.ChatMessage
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class HayatFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        return uid
    }

    fun observeProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeMessages(userId: String): Flow<List<ChatMessage>> =
        db.collection("users").document(userId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(40)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeReminders(userId: String): Flow<List<AssistantReminder>> =
        db.collection("users").document(userId).collection("reminders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AssistantReminder::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun ensureUserProfile(userId: String, defaultDisplayName: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = defaultDisplayName.trim().ifBlank { "Friend" }.take(60)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "preferredLanguage" to "hinglish",
                    "mood" to "Focused",
                    "voiceEnabled" to true,
                    "ttsSpeechRate" to 1.0,
                    "backgroundAssistantEnabled" to false,
                    "cityName" to "New Delhi",
                    "memorySummary" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileFields(
        userId: String,
        displayName: String? = null,
        preferredLanguage: String? = null,
        mood: String? = null,
        voiceEnabled: Boolean? = null,
        ttsSpeechRate: Double? = null,
        backgroundAssistantEnabled: Boolean? = null,
        cityName: String? = null,
        memorySummary: String? = null
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            displayName?.let {
                val clean = it.trim().ifBlank { "Friend" }.take(60)
                updates["displayName"] = clean
            }
            preferredLanguage?.let {
                if (it in listOf("en", "hi", "hinglish")) updates["preferredLanguage"] = it
            }
            mood?.let {
                if (it in listOf("Focused", "Chill", "Study", "Energized")) updates["mood"] = it
            }
            voiceEnabled?.let { updates["voiceEnabled"] = it }
            ttsSpeechRate?.let { updates["ttsSpeechRate"] = it.coerceIn(0.5, 2.0) }
            backgroundAssistantEnabled?.let { updates["backgroundAssistantEnabled"] = it }
            cityName?.let { updates["cityName"] = it.trim().take(80) }
            memorySummary?.let { updates["memorySummary"] = it.trim().take(1000) }

            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addMessage(
        userId: String,
        role: String,
        text: String,
        language: String,
        actionTag: String? = null,
        hasImage: Boolean = false
    ): Result<Unit> {
        val path = "users/$userId/messages"
        return try {
            val uid = requireUserId(userId)
            val cleanText = text.trim().take(8000)
            require(cleanText.isNotEmpty()) { "Message text cannot be empty" }
            val safeRole = if (role == "model") "model" else "user"
            val safeLang = if (language == "hi") "hi" else "en"
            val cleanTag = actionTag?.trim()?.take(64)?.ifBlank { null }

            val payload = mapOf(
                "userId" to uid,
                "role" to safeRole,
                "text" to cleanText,
                "language" to safeLang,
                "actionTag" to cleanTag,
                "hasImage" to hasImage,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.collection("users").document(uid).collection("messages")
                .document()
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun clearChatHistory(userId: String): Result<Unit> {
        val path = "users/$userId/messages"
        return try {
            val uid = requireUserId(userId)
            val docs = db.collection("users").document(uid).collection("messages")
                .limit(100)
                .get()
                .await()
            if (!docs.isEmpty) {
                val batch = db.batch()
                docs.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun addReminder(
        userId: String,
        title: String,
        timeLabel: String,
        category: String
    ): Result<Unit> {
        val path = "users/$userId/reminders"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim().take(140)
            require(cleanTitle.isNotEmpty()) { "Reminder title cannot be empty" }
            val safeCategory = if (category in listOf("Alarm", "Reminder", "Study", "Music")) {
                category
            } else {
                "Reminder"
            }
            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "timeLabel" to timeLabel.trim().take(40),
                "category" to safeCategory,
                "completed" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("reminders")
                .document()
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleReminder(userId: String, reminderId: String, completed: Boolean): Result<Unit> {
        val path = "users/$userId/reminders/$reminderId"
        return try {
            val uid = requireUserId(userId)
            require(reminderId.isNotBlank()) { "Reminder ID cannot be blank" }
            db.collection("users").document(uid).collection("reminders").document(reminderId)
                .update(
                    mapOf(
                        "completed" to completed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteReminder(userId: String, reminderId: String): Result<Unit> {
        val path = "users/$userId/reminders/$reminderId"
        return try {
            val uid = requireUserId(userId)
            require(reminderId.isNotBlank()) { "Reminder ID cannot be blank" }
            db.collection("users").document(uid).collection("reminders").document(reminderId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
