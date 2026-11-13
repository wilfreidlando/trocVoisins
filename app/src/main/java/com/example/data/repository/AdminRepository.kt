package com.example.data.repository

import com.example.data.model.AssociationStatus
import com.example.data.model.Dispute
import com.example.data.model.DisputeAction
import com.example.data.model.DisputeReason
import com.example.data.model.DisputeStatus
import com.example.data.model.Item
import com.example.data.model.ItemStatus
import com.example.data.model.UserProfile
import com.example.data.model.toDispute
import com.example.data.model.toUserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/** Signalements, arbitrage des litiges et certification des associations. */
class AdminRepository(private val db: FirebaseFirestore) {

    private val disputes = db.collection("disputes")
    private val users = db.collection("users")

    /** Le médiateur voit tous les litiges ; un utilisateur voit ceux qu'il a ouverts. */
    fun disputesFlow(uid: String, isMediator: Boolean): Flow<List<Dispute>> =
        if (isMediator) disputes.limit(300).listFlow { it.toDispute() }
        else disputes.whereEqualTo("reporterId", uid).listFlow { it.toDispute() }

    fun pendingAssociationsFlow(): Flow<List<UserProfile>> =
        users.whereEqualTo("associationStatus", AssociationStatus.PENDING.name).listFlow { it.toUserProfile() }

    suspend fun reportItem(
        me: UserProfile,
        item: Item,
        reason: DisputeReason,
        details: String
    ): Result<Unit> = runCatching {
        check(item.donorId != me.id) { "Vous ne pouvez pas signaler votre propre annonce." }
        disputes.document().set(
            mapOf(
                "itemId" to item.id,
                "itemTitle" to item.title,
                "reporterId" to me.id,
                "reporterName" to me.displayName,
                "reportedUserId" to item.donorId,
                "reportedUserName" to item.donorName,
                "reason" to reason.name,
                "description" to details.trim(),
                "status" to DisputeStatus.OPEN.name,
                "resolutionNote" to "",
                "action" to DisputeAction.NONE.name,
                "mediatorId" to "",
                "createdAt" to serverTime(),
                "updatedAt" to serverTime()
            )
        ).await()
        Unit
    }

    suspend fun takeDispute(dispute: Dispute, mediatorId: String): Result<Unit> = runCatching {
        disputes.document(dispute.id).update(
            mapOf(
                "status" to DisputeStatus.UNDER_REVIEW.name,
                "mediatorId" to mediatorId,
                "updatedAt" to serverTime()
            )
        ).await()
        Unit
    }

    /**
     * Clôture un litige avec un effet réel : suspension d'annonce, avertissement ou suspension du compte.
     * Tout est écrit dans un seul lot : soit tout est appliqué, soit rien.
     */
    suspend fun resolveDispute(
        dispute: Dispute,
        mediatorId: String,
        dismissed: Boolean,
        note: String,
        action: DisputeAction
    ): Result<Unit> = runCatching {
        val effectiveAction = if (dismissed) DisputeAction.NONE else action
        val batch = db.batch()
        batch.update(
            disputes.document(dispute.id),
            mapOf(
                "status" to (if (dismissed) DisputeStatus.DISMISSED else DisputeStatus.RESOLVED).name,
                "resolutionNote" to note.trim(),
                "action" to effectiveAction.name,
                "mediatorId" to mediatorId,
                "updatedAt" to serverTime()
            )
        )
        when (effectiveAction) {
            DisputeAction.SUSPEND_ITEM -> batch.update(
                db.collection("items").document(dispute.itemId),
                mapOf(
                    "status" to ItemStatus.SUSPENDED.name,
                    "moderationNote" to "Suspendue après signalement : ${note.trim()}",
                    "updatedAt" to serverTime()
                )
            )
            DisputeAction.WARN_USER -> batch.update(
                users.document(dispute.reportedUserId),
                mapOf("warnings" to FieldValue.increment(1L), "updatedAt" to serverTime())
            )
            DisputeAction.SUSPEND_USER -> batch.update(
                users.document(dispute.reportedUserId),
                mapOf(
                    "isSuspended" to true,
                    "warnings" to FieldValue.increment(1L),
                    "updatedAt" to serverTime()
                )
            )
            DisputeAction.NONE -> Unit
        }
        batch.commit().await()
        Unit
    }

    suspend fun certifyAssociation(user: UserProfile, approve: Boolean, note: String): Result<Unit> = runCatching {
        users.document(user.id).update(
            mapOf(
                "associationStatus" to (if (approve) AssociationStatus.CERTIFIED else AssociationStatus.REJECTED).name,
                "associationNote" to note.trim(),
                "updatedAt" to serverTime()
            )
        ).await()
        Unit
    }
}
