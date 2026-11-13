package com.example.data.repository

import com.example.data.model.AppNotification
import com.example.data.model.GrainRules
import com.example.data.model.Item
import com.example.data.model.ItemRequest
import com.example.data.model.ItemStatus
import com.example.data.model.LedgerEntry
import com.example.data.model.LedgerReason
import com.example.data.model.NotificationType
import com.example.data.model.Review
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.ExchangeType
import com.example.data.model.toLedgerEntry
import com.example.data.model.toNotification
import com.example.data.model.toReview
import com.example.data.model.toUserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/** Notifications internes (cloche de l'application) : écrites en « meilleur effort », jamais bloquantes. */
class NotificationRepository(private val db: FirebaseFirestore) {

    fun notificationsFlow(uid: String): Flow<List<AppNotification>> =
        db.collection("notifications").whereEqualTo("userId", uid).listFlow { it.toNotification() }

    suspend fun send(
        toUserId: String,
        fromUserId: String,
        type: NotificationType,
        title: String,
        body: String,
        refId: String = ""
    ) {
        if (toUserId.isBlank() || toUserId == fromUserId) return
        runCatching {
            db.collection("notifications").document().set(
                mapOf(
                    "userId" to toUserId,
                    "fromUserId" to fromUserId,
                    "type" to type.name,
                    "title" to title.take(100),
                    "body" to body.take(300),
                    "refId" to refId,
                    "read" to false,
                    "createdAt" to serverTime()
                )
            ).await()
        }
    }

    suspend fun markAllRead(list: List<AppNotification>) {
        val unread = list.filter { !it.read }.take(400)
        if (unread.isEmpty()) return
        runCatching {
            val batch = db.batch()
            unread.forEach { n -> batch.update(db.collection("notifications").document(n.id), "read", true) }
            batch.commit().await()
        }
    }
}

/** Profils publics, avis et grand livre de Graines. */
class SocialRepository(private val db: FirebaseFirestore) {

    fun publicProfileFlow(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid).docFlow { it.toUserProfile() }

    fun reviewsFlow(targetUserId: String): Flow<List<Review>> =
        db.collection("reviews").whereEqualTo("targetUserId", targetUserId).listFlow { it.toReview() }

    fun myReviewsWrittenFlow(authorId: String): Flow<List<Review>> =
        db.collection("reviews").whereEqualTo("authorId", authorId).listFlow { it.toReview() }

    fun ledgerFlow(uid: String): Flow<List<LedgerEntry>> =
        db.collection("users").document(uid).collection("ledger").listFlow { it.toLedgerEntry() }

    /**
     * Dépose un avis sur l'autre partie d'un échange terminé.
     * Le document avis (1 par personne et par échange) et le cumul de notes du destinataire
     * sont écrits ensemble : les règles Firestore vérifient leur cohérence.
     */
    suspend fun addReview(
        item: Item,
        author: UserProfile,
        targetUserId: String,
        rating: Int,
        badge: String,
        comment: String
    ): Result<Unit> = runCatching {
        val reviewId = "${item.id}_${author.id}"
        val reviewRef = db.collection("reviews").document(reviewId)
        val targetRef = db.collection("users").document(targetUserId)
        db.runTransaction { tx ->
            if (tx.get(reviewRef).exists()) throw IllegalStateException("Vous avez déjà laissé un avis pour cet échange.")
            val target = tx.get(targetRef)
            val sum = target.getLong("ratingSum")?.toInt() ?: 0
            val count = target.getLong("reviewCount")?.toInt() ?: 0
            tx.set(
                reviewRef,
                mapOf(
                    "itemId" to item.id,
                    "itemTitle" to item.title,
                    "authorId" to author.id,
                    "authorName" to author.displayName,
                    "targetUserId" to targetUserId,
                    "rating" to rating,
                    "badge" to badge,
                    "comment" to comment.trim(),
                    "createdAt" to serverTime()
                )
            )
            tx.update(
                targetRef,
                mapOf(
                    "ratingSum" to sum + rating,
                    "reviewCount" to count + 1,
                    "lastReviewId" to reviewId,
                    "updatedAt" to serverTime()
                )
            )
            true
        }.await()
        Unit
    }
}

/**
 * Grand livre de Graines.
 *
 * Règle d'or : un solde ne change JAMAIS sans une ligne de grand livre correspondante
 * (identifiant unique par échange et par motif => impossible de réclamer deux fois).
 * Les règles Firestore vérifient que la ligne de grand livre est justifiée par un échange réellement
 * terminé, et que la variation du solde est égale au montant de la ligne.
 */
class GrainsRepository(private val db: FirebaseFirestore) {

    data class Claim(
        val reason: LedgerReason,
        val amount: Int,
        val refId: String,
        val giftsDelta: Int = 0,
        val trocsDelta: Int = 0
    )

    /** Retourne true si les Graines viennent d'être créditées/débitées, false si déjà réclamées. */
    suspend fun claim(uid: String, claim: Claim): Result<Boolean> = runCatching {
        val ledgerId = "${claim.refId}_${claim.reason.name}"
        val userRef = db.collection("users").document(uid)
        val ledgerRef = userRef.collection("ledger").document(ledgerId)
        db.runTransaction { tx ->
            if (tx.get(ledgerRef).exists()) {
                return@runTransaction false
            }
            val user = tx.get(userRef)
            val grains = user.getLong("grains")?.toInt() ?: 0
            val total = user.getLong("totalGrainsEarned")?.toInt() ?: 0
            val gifts = user.getLong("giftsGiven")?.toInt() ?: 0
            val trocs = user.getLong("trocsCompleted")?.toInt() ?: 0
            val newGrains = grains + claim.amount
            if (newGrains < 0) throw IllegalStateException("Solde de Graines insuffisant.")
            tx.set(
                ledgerRef,
                mapOf(
                    "amount" to claim.amount,
                    "reason" to claim.reason.name,
                    "refId" to claim.refId,
                    "createdAt" to serverTime()
                )
            )
            val updates = mutableMapOf<String, Any>(
                "grains" to newGrains,
                "totalGrainsEarned" to (total + if (claim.amount > 0) claim.amount else 0),
                "lastLedgerId" to ledgerId,
                "updatedAt" to serverTime()
            )
            if (claim.giftsDelta != 0) updates["giftsGiven"] = gifts + claim.giftsDelta
            if (claim.trocsDelta != 0) updates["trocsCompleted"] = trocs + claim.trocsDelta
            tx.update(userRef, updates)
            true
        }.await()
    }

    /** Liste des réclamations dues à `me` pour un échange terminé. */
    fun exchangeClaims(item: Item, request: ItemRequest?, me: String): List<Claim> {
        if (item.statusEnum != ItemStatus.COMPLETED) return emptyList()
        val isDonor = me == item.donorId
        val isReceiver = me == item.reservedByUserId
        if (!isDonor && !isReceiver) return emptyList()

        val claims = mutableListOf<Claim>()
        if (item.exchangeType == ExchangeType.DON.name) {
            if (isDonor) {
                claims += Claim(LedgerReason.DON_DONOR, GrainRules.DON_DONOR, item.id, giftsDelta = 1)
            } else if (GrainRules.DON_RECEIVE_COST > 0 && item.donorRole != UserRole.ASSOCIATION.name) {
                claims += Claim(LedgerReason.DON_RECEIVED, -GrainRules.DON_RECEIVE_COST, item.id)
            }
        } else {
            claims += Claim(LedgerReason.TROC_DONE, GrainRules.TROC_EACH, item.id, trocsDelta = 1)
            val topUp = request?.graineTopUp ?: 0
            if (topUp > 0) {
                if (isReceiver) claims += Claim(LedgerReason.TOPUP_PAID, -topUp, item.id)
                else claims += Claim(LedgerReason.TOPUP_RECEIVED, topUp, item.id)
            }
        }
        return claims
    }
}
