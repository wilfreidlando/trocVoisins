package com.example.data.repository

import com.example.data.model.AssociationStatus
import com.example.data.model.AssociationType
import com.example.data.model.CollectionPoint
import com.example.data.model.CollectionPointType
import com.example.data.model.Cities
import com.example.data.model.Contribution
import com.example.data.model.ContributionStatus
import com.example.data.model.Fundraiser
import com.example.data.model.FundraiserStatus
import com.example.data.model.LedgerReason
import com.example.data.model.UserProfile
import com.example.data.model.toCollectionPoint
import com.example.data.model.toContribution
import com.example.data.model.toFundraiser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

data class FundraiserForm(
    val title: String,
    val description: String,
    val neighborhood: String,
    val targetFcfa: Long,
    val targetGrains: Int,
    val deadlineMs: Long,
    val category: AssociationType,
    val isEmergency: Boolean,
    val payInstructions: String,
    val contactPhone: String,
    val proofNote: String
)

data class CollectionPointForm(
    val name: String,
    val type: CollectionPointType,
    val neighborhood: String,
    val landmark: String,
    val contactPhone: String,
    val openingHours: String,
    val acceptedCategories: String
)

/**
 * Cagnottes solidaires, contributions et points relais.
 *
 * Contributions :
 *  - en Graines : immédiates (grand livre + compteur de la cagnotte dans une seule transaction) ;
 *  - en FCFA : « promesse » faite par le donateur (Mobile Money / remise en main propre selon les
 *    instructions de l'association), puis CONFIRMÉE par l'association quand elle a bien reçu l'argent.
 *    Aucun montant n'est compté tant que l'association n'a pas confirmé.
 */
class FundraiserRepository(private val db: FirebaseFirestore) {

    private val fundraisers = db.collection("fundraisers")
    private val contributions = db.collection("contributions")
    private val points = db.collection("collectionPoints")

    fun fundraisersFlow(): Flow<List<Fundraiser>> =
        fundraisers.orderBy("createdAt", Query.Direction.DESCENDING).limit(100).listFlow { it.toFundraiser() }

    fun contributionsReceivedFlow(associationId: String): Flow<List<Contribution>> =
        contributions.whereEqualTo("associationId", associationId).listFlow { it.toContribution() }

    fun myContributionsFlow(uid: String): Flow<List<Contribution>> =
        contributions.whereEqualTo("contributorId", uid).listFlow { it.toContribution() }

    fun pointsFlow(): Flow<List<CollectionPoint>> =
        points.orderBy("createdAt", Query.Direction.DESCENDING).limit(200).listFlow { it.toCollectionPoint() }

    // ------------------------------------------------------------------ Cagnottes

    suspend fun create(profile: UserProfile, form: FundraiserForm): Result<String> = runCatching {
        check(profile.isCertifiedAssociation) { "Seule une association certifiée peut créer une cagnotte." }
        check(form.targetFcfa > 0 || form.targetGrains > 0) { "Indiquez un objectif en FCFA ou en Graines." }
        val ref = fundraisers.document()
        ref.set(
            mapOf<String, Any>(
                "associationId" to profile.id,
                "associationName" to profile.displayName,
                "title" to form.title.trim(),
                "description" to form.description.trim(),
                "city" to profile.city,
                "neighborhood" to form.neighborhood.trim(),
                "targetFcfa" to form.targetFcfa,
                "currentFcfa" to 0L,
                "targetGrains" to form.targetGrains,
                "currentGrains" to 0,
                "contributorsCount" to 0,
                "deadlineMs" to form.deadlineMs,
                "category" to form.category.name,
                "status" to FundraiserStatus.PENDING_REVIEW.name,
                "isEmergency" to form.isEmergency,
                "payInstructions" to form.payInstructions.trim(),
                "contactPhone" to form.contactPhone.trim(),
                "proofNote" to form.proofNote.trim(),
                "lastContributionId" to "",
                "createdAt" to serverTime(),
                "updatedAt" to serverTime()
            )
        ).await()
        ref.id
    }

    /** Validation par le médiateur. */
    suspend fun review(fundraiser: Fundraiser, approve: Boolean): Result<Unit> = runCatching {
        fundraisers.document(fundraiser.id).update(
            mapOf(
                "status" to (if (approve) FundraiserStatus.ACTIVE else FundraiserStatus.REJECTED).name,
                "updatedAt" to serverTime()
            )
        ).await()
        Unit
    }

    suspend fun close(fundraiser: Fundraiser): Result<Unit> = runCatching {
        fundraisers.document(fundraiser.id).update(
            mapOf("status" to FundraiserStatus.CLOSED.name, "updatedAt" to serverTime())
        ).await()
        Unit
    }

    // ------------------------------------------------------------------ Contributions

    /** Don de Graines : immédiat. */
    suspend fun giveGrains(
        fundraiser: Fundraiser,
        me: UserProfile,
        grains: Int,
        anonymous: Boolean
    ): Result<Unit> = runCatching {
        check(grains > 0) { "Indiquez un nombre de Graines." }
        check(fundraiser.statusEnum == FundraiserStatus.ACTIVE) { "Cette cagnotte n'est pas active." }
        val fundRef = fundraisers.document(fundraiser.id)
        val contribRef = contributions.document()
        val userRef = db.collection("users").document(me.id)
        val ledgerId = "${contribRef.id}_${LedgerReason.FUNDRAISER.name}"
        val ledgerRef = userRef.collection("ledger").document(ledgerId)

        db.runTransaction { tx ->
            val fund = tx.get(fundRef)
            val user = tx.get(userRef)
            val myGrains = user.getLong("grains")?.toInt() ?: 0
            if (grains > myGrains) throw IllegalStateException("Solde de Graines insuffisant.")
            val currentGrains = fund.getLong("currentGrains")?.toInt() ?: 0
            val count = fund.getLong("contributorsCount")?.toInt() ?: 0

            tx.set(
                contribRef,
                mapOf(
                    "fundraiserId" to fundraiser.id,
                    "fundraiserTitle" to fundraiser.title,
                    "associationId" to fundraiser.associationId,
                    "contributorId" to me.id,
                    "contributorName" to (if (anonymous) "Anonyme" else me.displayName),
                    "amountFcfa" to 0L,
                    "grains" to grains,
                    "isAnonymous" to anonymous,
                    "status" to ContributionStatus.CONFIRMED.name,
                    "createdAt" to serverTime()
                )
            )
            tx.update(
                fundRef,
                mapOf(
                    "currentGrains" to currentGrains + grains,
                    "contributorsCount" to count + 1,
                    "lastContributionId" to contribRef.id,
                    "updatedAt" to serverTime()
                )
            )
            tx.set(
                ledgerRef,
                mapOf(
                    "amount" to -grains,
                    "reason" to LedgerReason.FUNDRAISER.name,
                    "refId" to contribRef.id,
                    "createdAt" to serverTime()
                )
            )
            tx.update(
                userRef,
                mapOf(
                    "grains" to myGrains - grains,
                    "lastLedgerId" to ledgerId,
                    "updatedAt" to serverTime()
                )
            )
            true
        }.await()
        Unit
    }

    /** Promesse de don en FCFA : à confirmer par l'association une fois l'argent reçu. */
    suspend fun pledgeFcfa(
        fundraiser: Fundraiser,
        me: UserProfile,
        amountFcfa: Long,
        anonymous: Boolean
    ): Result<Unit> = runCatching {
        check(amountFcfa >= 100) { "Le montant minimum est de 100 FCFA." }
        check(fundraiser.statusEnum == FundraiserStatus.ACTIVE) { "Cette cagnotte n'est pas active." }
        contributions.document().set(
            mapOf(
                "fundraiserId" to fundraiser.id,
                "fundraiserTitle" to fundraiser.title,
                "associationId" to fundraiser.associationId,
                "contributorId" to me.id,
                "contributorName" to (if (anonymous) "Anonyme" else me.displayName),
                "amountFcfa" to amountFcfa,
                "grains" to 0,
                "isAnonymous" to anonymous,
                "status" to ContributionStatus.PLEDGED.name,
                "createdAt" to serverTime()
            )
        ).await()
        Unit
    }

    /** L'association confirme (ou refuse) la réception d'une promesse en FCFA. */
    suspend fun confirmPledge(
        contribution: Contribution,
        fundraiser: Fundraiser,
        received: Boolean
    ): Result<Unit> = runCatching {
        val batch = db.batch()
        batch.update(
            contributions.document(contribution.id),
            mapOf(
                "status" to (if (received) ContributionStatus.CONFIRMED else ContributionStatus.REJECTED).name
            )
        )
        if (received) {
            val reached = fundraiser.targetFcfa > 0 &&
                    fundraiser.currentFcfa + contribution.amountFcfa >= fundraiser.targetFcfa
            val updates = mutableMapOf<String, Any>(
                "currentFcfa" to FieldValue.increment(contribution.amountFcfa),
                "contributorsCount" to FieldValue.increment(1L),
                "lastContributionId" to contribution.id,
                "updatedAt" to serverTime()
            )
            if (reached) updates["status"] = FundraiserStatus.FUNDED.name
            batch.update(fundraisers.document(fundraiser.id), updates)
        }
        batch.commit().await()
        Unit
    }

    // ------------------------------------------------------------------ Points relais

    suspend fun createPoint(profile: UserProfile, form: CollectionPointForm): Result<Unit> = runCatching {
        check(profile.isCertifiedAssociation || profile.isMediator) {
            "Seule une association certifiée ou le médiateur peut créer un point relais."
        }
        val city = Cities.find(profile.city)
        val lat = if (profile.lat != 0.0) profile.lat else city.lat
        val lng = if (profile.lng != 0.0) profile.lng else city.lng
        points.document().set(
            mapOf<String, Any>(
                "name" to form.name.trim(),
                "organizationName" to profile.displayName,
                "ownerId" to profile.id,
                "type" to form.type.name,
                "city" to profile.city,
                "neighborhood" to form.neighborhood.trim(),
                "landmark" to form.landmark.trim(),
                "contactPhone" to form.contactPhone.trim(),
                "openingHours" to form.openingHours.trim(),
                "acceptedCategories" to form.acceptedCategories.trim(),
                "lat" to lat,
                "lng" to lng,
                "createdAt" to serverTime()
            )
        ).await()
        Unit
    }

    suspend fun deletePoint(point: CollectionPoint): Result<Unit> = runCatching {
        points.document(point.id).delete().await()
        Unit
    }
}
