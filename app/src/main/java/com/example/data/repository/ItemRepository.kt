package com.example.data.repository

import com.example.data.model.AssociationStatus
import com.example.data.model.Category
import com.example.data.model.Cities
import com.example.data.model.ExchangeType
import com.example.data.model.Item
import com.example.data.model.ItemCondition
import com.example.data.model.ItemRequest
import com.example.data.model.ItemStatus
import com.example.data.model.RequestStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.toItem
import com.example.data.model.toItemRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

data class PublishForm(
    val title: String,
    val description: String,
    val category: Category,
    val condition: ItemCondition,
    val exchangeType: ExchangeType,
    val wantedInReturn: String,
    val imageB64: String,
    val isEmergencyResponse: Boolean,
    val targetOrphanageName: String
)

/**
 * Annonces et processus d'échange.
 *
 * Machine à états d'une annonce :
 *   PENDING_MODERATION --(médiateur approuve)--> AVAILABLE --(propriétaire accepte une demande)--> RESERVED
 *   RESERVED --(les 2 parties confirment la remise)--> COMPLETED
 *   RESERVED --(annulation par l'une des parties)--> AVAILABLE
 *   PENDING_MODERATION --(médiateur refuse)--> REJECTED ;  AVAILABLE --(litige)--> SUSPENDED
 * Un citoyen « de confiance » (niveau Pousse et plus) publie directement en AVAILABLE.
 */
class ItemRepository(
    private val db: FirebaseFirestore,
    private val chat: ChatRepository
) {
    private val items = db.collection("items")
    private val requests = db.collection("requests")

    fun itemsFlow(): Flow<List<Item>> =
        items.orderBy("createdAt", Query.Direction.DESCENDING).limit(400).listFlow { it.toItem() }

    /** Demandes que j'ai faites. */
    fun myRequestsFlow(uid: String): Flow<List<ItemRequest>> =
        requests.whereEqualTo("requesterId", uid).listFlow { it.toItemRequest() }

    /** Demandes reçues sur mes annonces. */
    fun incomingRequestsFlow(uid: String): Flow<List<ItemRequest>> =
        requests.whereEqualTo("itemOwnerId", uid).listFlow { it.toItemRequest() }

    // ------------------------------------------------------------------ Publication

    suspend fun publish(profile: UserProfile, form: PublishForm): Result<Item> = runCatching {
        check(!profile.isSuspended) { "Votre compte est suspendu : vous ne pouvez pas publier." }
        if (profile.isAssociation) {
            check(profile.associationStatus == AssociationStatus.CERTIFIED.name) {
                "Votre association doit être certifiée pour publier."
            }
        }
        val ref = items.document()
        val (lat, lng) = approxCoords(profile, ref.id)
        val status = if (profile.isTrusted) ItemStatus.AVAILABLE else ItemStatus.PENDING_MODERATION
        val data = mapOf<String, Any>(
            "title" to form.title.trim(),
            "description" to form.description.trim(),
            "category" to form.category.name,
            "condition" to form.condition.name,
            "exchangeType" to form.exchangeType.name,
            "wantedInReturn" to (if (form.exchangeType == ExchangeType.TROC) form.wantedInReturn.trim() else ""),
            "donorId" to profile.id,
            "donorName" to profile.displayName,
            "donorRole" to profile.role,
            "donorCertified" to profile.isCertifiedAssociation,
            "city" to profile.city,
            "neighborhood" to profile.neighborhood,
            "landmark" to profile.landmark,
            "approximateLat" to lat,
            "approximateLng" to lng,
            "status" to status.name,
            "reservedByUserId" to "",
            "reservedByName" to "",
            "donorConfirmed" to false,
            "receiverConfirmed" to false,
            "imageB64" to form.imageB64,
            "isEmergencyResponse" to form.isEmergencyResponse,
            "targetOrphanageName" to form.targetOrphanageName.trim(),
            "moderationNote" to "",
            "createdAt" to serverTime(),
            "updatedAt" to serverTime()
        )
        ref.set(data).await()
        Item(
            id = ref.id,
            title = form.title,
            description = form.description,
            category = form.category.name,
            condition = form.condition.name,
            exchangeType = form.exchangeType.name,
            donorId = profile.id,
            donorName = profile.displayName,
            city = profile.city,
            status = status.name
        )
    }

    /** Position floutée (~300 m) : arrondie et décalée de façon stable (pas de hasard à chaque affichage). */
    private fun approxCoords(profile: UserProfile, seed: String): Pair<Double, Double> {
        val city = Cities.find(profile.city)
        val baseLat = if (profile.lat != 0.0) profile.lat else city.lat
        val baseLng = if (profile.lng != 0.0) profile.lng else city.lng
        val h = seed.hashCode()
        val jLat = (((h shr 8) and 0xFF) - 128) / 128.0 * 0.0015
        val jLng = ((h and 0xFF) - 128) / 128.0 * 0.0015
        return Pair(Math.round(baseLat * 1000) / 1000.0 + jLat, Math.round(baseLng * 1000) / 1000.0 + jLng)
    }

    suspend fun delete(item: Item): Result<Unit> = runCatching {
        check(item.statusEnum != ItemStatus.RESERVED && item.statusEnum != ItemStatus.COMPLETED) {
            "Une annonce réservée ou terminée ne peut pas être supprimée."
        }
        items.document(item.id).delete().await()
        Unit
    }

    // ------------------------------------------------------------------ Modération (médiateur)

    suspend fun moderate(item: Item, approve: Boolean, note: String): Result<Unit> = runCatching {
        items.document(item.id).update(
            mapOf(
                "status" to (if (approve) ItemStatus.AVAILABLE else ItemStatus.REJECTED).name,
                "moderationNote" to note.trim(),
                "updatedAt" to serverTime()
            )
        ).await()
        Unit
    }

    suspend fun setSuspended(item: Item, suspended: Boolean, note: String): Result<Unit> = runCatching {
        items.document(item.id).update(
            mapOf(
                "status" to (if (suspended) ItemStatus.SUSPENDED else ItemStatus.AVAILABLE).name,
                "moderationNote" to note.trim(),
                "updatedAt" to serverTime()
            )
        ).await()
        Unit
    }

    // ------------------------------------------------------------------ Demandes

    /**
     * Demande d'un objet (don ou troc). Crée la demande + le premier message dans la conversation.
     * Pour un troc, `message` décrit l'objet proposé en échange ; `topUp` = Graines ajoutées pour équilibrer.
     */
    suspend fun requestItem(
        item: Item,
        me: UserProfile,
        message: String,
        topUp: Int,
        previous: ItemRequest?
    ): Result<Unit> = runCatching {
        check(!me.isSuspended) { "Votre compte est suspendu." }
        check(item.donorId != me.id) { "C'est votre propre annonce." }
        check(item.statusEnum == ItemStatus.AVAILABLE) { "Cet objet n'est plus disponible." }
        if (item.isTroc) {
            check(message.isNotBlank()) { "Décrivez l'objet que vous proposez en échange." }
            check(topUp in 0..me.grains) { "Vous n'avez pas assez de Graines pour ce complément." }
        } else if (com.example.data.model.GrainRules.DON_RECEIVE_COST > 0 &&
            item.donorRole != UserRole.ASSOCIATION.name
        ) {
            check(me.grains >= com.example.data.model.GrainRules.DON_RECEIVE_COST) {
                "Il vous faut ${com.example.data.model.GrainRules.DON_RECEIVE_COST} Graines pour recevoir ce don. Donnez ou aidez pour en gagner."
            }
        }

        val reqId = "${item.id}_${me.id}"
        val ref = requests.document(reqId)
        val text = if (item.isTroc) "🔄 Proposition de troc : ${message.trim()}" +
                (if (topUp > 0) "\n+ $topUp Graines en complément" else "")
        else "🙋 Bonjour, cet objet m'intéresse." + (if (message.isNotBlank()) "\n${message.trim()}" else "")

        val batch = db.batch()
        if (previous == null) {
            batch.set(
                ref,
                mapOf(
                    "itemId" to item.id,
                    "itemTitle" to item.title,
                    "itemOwnerId" to item.donorId,
                    "requesterId" to me.id,
                    "requesterName" to me.displayName,
                    "message" to message.trim(),
                    "graineTopUp" to topUp,
                    "status" to RequestStatus.PENDING.name,
                    "createdAt" to serverTime(),
                    "updatedAt" to serverTime()
                )
            )
        } else {
            batch.update(
                ref,
                mapOf(
                    "message" to message.trim(),
                    "graineTopUp" to topUp,
                    "status" to RequestStatus.PENDING.name,
                    "updatedAt" to serverTime()
                )
            )
        }
        val seed = ThreadSeed(item.id, item.title, item.donorId, item.donorName, me.id, me.displayName)
        chat.stageMessage(batch, seed, me.id, me.displayName, text)
        batch.commit().await()
        Unit
    }

    suspend fun cancelRequest(request: ItemRequest): Result<Unit> = runCatching {
        requests.document(request.id).update(
            mapOf("status" to RequestStatus.CANCELLED.name, "updatedAt" to serverTime())
        ).await()
        Unit
    }

    suspend fun declineRequest(request: ItemRequest): Result<Unit> = runCatching {
        requests.document(request.id).update(
            mapOf("status" to RequestStatus.DECLINED.name, "updatedAt" to serverTime())
        ).await()
        Unit
    }

    /** Le propriétaire choisit un demandeur : l'objet passe en RESERVED, les autres demandes sont refusées. */
    suspend fun acceptRequest(item: Item, request: ItemRequest, allIncoming: List<ItemRequest>): Result<Unit> =
        runCatching {
            check(item.statusEnum == ItemStatus.AVAILABLE) { "Cet objet n'est plus disponible." }
            val batch = db.batch()
            batch.update(
                items.document(item.id),
                mapOf(
                    "status" to ItemStatus.RESERVED.name,
                    "reservedByUserId" to request.requesterId,
                    "reservedByName" to request.requesterName,
                    "donorConfirmed" to false,
                    "receiverConfirmed" to false,
                    "updatedAt" to serverTime()
                )
            )
            batch.update(
                requests.document(request.id),
                mapOf("status" to RequestStatus.ACCEPTED.name, "updatedAt" to serverTime())
            )
            allIncoming
                .filter { it.itemId == item.id && it.id != request.id && it.statusEnum == RequestStatus.PENDING }
                .forEach { other ->
                    batch.update(
                        requests.document(other.id),
                        mapOf("status" to RequestStatus.DECLINED.name, "updatedAt" to serverTime())
                    )
                }
            val seed = ThreadSeed(item.id, item.title, item.donorId, item.donorName, request.requesterId, request.requesterName)
            chat.stageMessage(
                batch, seed, item.donorId, "TrocVoisins",
                "✅ Demande acceptée. Fixez un rendez-vous dans un lieu public (carrefour, pharmacie, point relais). " +
                        "Quand la remise est faite, chacun confirme dans l'application.",
                isSystem = true
            )
            batch.commit().await()
            Unit
        }

    /** Confirmation de remise par l'une des parties ; quand les deux ont confirmé, l'échange est terminé. */
    suspend fun confirmHandover(itemId: String, meId: String): Result<Unit> = runCatching {
        val ref = items.document(itemId)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val donorId = snap.getString("donorId") ?: ""
            val reservedBy = snap.getString("reservedByUserId") ?: ""
            val status = snap.getString("status") ?: ""
            if (status != ItemStatus.RESERVED.name) throw IllegalStateException("Cet échange n'est plus en cours.")
            val donorDone = snap.getBoolean("donorConfirmed") ?: false
            val receiverDone = snap.getBoolean("receiverConfirmed") ?: false
            val updates = mutableMapOf<String, Any>("updatedAt" to serverTime())
            when (meId) {
                donorId -> {
                    updates["donorConfirmed"] = true
                    if (receiverDone) updates["status"] = ItemStatus.COMPLETED.name
                }
                reservedBy -> {
                    updates["receiverConfirmed"] = true
                    if (donorDone) updates["status"] = ItemStatus.COMPLETED.name
                }
                else -> throw IllegalStateException("Vous ne participez pas à cet échange.")
            }
            tx.update(ref, updates)
            true
        }.await()
        Unit
    }

    /** Annulation de la réservation par l'une des parties : l'objet redevient disponible. */
    suspend fun cancelReservation(item: Item, meId: String, meName: String): Result<Unit> = runCatching {
        check(item.statusEnum == ItemStatus.RESERVED) { "Aucune réservation à annuler." }
        val batch = db.batch()
        batch.update(
            items.document(item.id),
            mapOf(
                "status" to ItemStatus.AVAILABLE.name,
                "reservedByUserId" to "",
                "reservedByName" to "",
                "donorConfirmed" to false,
                "receiverConfirmed" to false,
                "updatedAt" to serverTime()
            )
        )
        batch.update(
            requests.document("${item.id}_${item.reservedByUserId}"),
            mapOf("status" to RequestStatus.CANCELLED.name, "updatedAt" to serverTime())
        )
        val seed = ThreadSeed(item.id, item.title, item.donorId, item.donorName, item.reservedByUserId, item.reservedByName)
        chat.stageMessage(
            batch, seed, meId, "TrocVoisins",
            "⚠️ $meName a annulé la réservation. L'objet est de nouveau disponible.",
            isSystem = true
        )
        batch.commit().await()
        Unit
    }
}
