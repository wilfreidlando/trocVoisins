package com.example.data.repository

import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.toChatMessage
import com.example.data.model.toChatThread
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/** Une conversation = une annonce + un demandeur (identifiant : annonce_demandeur). */
data class ThreadSeed(
    val itemId: String,
    val itemTitle: String,
    val ownerId: String,
    val ownerName: String,
    val requesterId: String,
    val requesterName: String
) {
    val id: String get() = "${itemId}_$requesterId"
}

class ChatRepository(private val db: FirebaseFirestore) {

    fun threadsFlow(uid: String): Flow<List<ChatThread>> =
        db.collection("chats").whereArrayContains("participants", uid).listFlow { it.toChatThread() }

    fun threadFlow(threadId: String): Flow<ChatThread?> =
        db.collection("chats").document(threadId).docFlow { it.toChatThread() }

    fun messagesFlow(threadId: String): Flow<List<ChatMessage>> =
        db.collection("chats").document(threadId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .listFlow { it.toChatMessage() }

    /**
     * Ajoute à un lot d'écritures un message et la mise à jour (ou la création) de la conversation.
     * `set(merge)` : crée la conversation si elle n'existe pas, sinon met à jour les derniers champs.
     */
    fun stageMessage(
        batch: WriteBatch,
        seed: ThreadSeed,
        senderId: String,
        senderName: String,
        text: String,
        isSystem: Boolean = false
    ) {
        val chatRef = db.collection("chats").document(seed.id)
        batch.set(
            chatRef,
            mapOf(
                "itemId" to seed.itemId,
                "itemTitle" to seed.itemTitle,
                "ownerId" to seed.ownerId,
                "ownerName" to seed.ownerName,
                "requesterId" to seed.requesterId,
                "requesterName" to seed.requesterName,
                "participants" to listOf(seed.ownerId, seed.requesterId),
                "lastMessage" to text.take(200),
                "lastSenderId" to senderId,
                "lastMessageAt" to serverTime()
            ),
            SetOptions.merge()
        )
        batch.set(
            chatRef.collection("messages").document(),
            mapOf(
                "senderId" to senderId,
                "senderName" to senderName,
                "text" to text.take(1000),
                "isSystem" to isSystem,
                "createdAt" to serverTime()
            )
        )
    }

    suspend fun send(seed: ThreadSeed, senderId: String, senderName: String, text: String): Result<Unit> =
        runCatching {
            val clean = text.trim()
            if (clean.isEmpty()) return@runCatching
            val batch = db.batch()
            stageMessage(batch, seed, senderId, senderName, clean)
            batch.commit().await()
        }
}
