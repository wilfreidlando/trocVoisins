package com.example.data.model

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Modèles de l'application. Chaque modèle est construit à partir d'un document Firestore
 * (fonctions `toXxx()` ci-dessous). Firestore est la source de vérité.
 */

data class UserProfile(
    val id: String,
    val name: String,
    val role: String = UserRole.CITOYEN.name,
    val city: String = "Douala",
    val neighborhood: String = "",
    val landmark: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val radiusKm: Float = 5f,
    val grains: Int = 0,
    val totalGrainsEarned: Int = 0,
    val lastLedgerId: String = "",
    val associationName: String = "",
    val associationType: String = "",
    val associationRegNumber: String = "",
    val associationPhone: String = "",
    val associationStatus: String = AssociationStatus.NONE.name,
    val associationNote: String = "",
    val ratingSum: Int = 0,
    val reviewCount: Int = 0,
    val lastReviewId: String = "",
    val giftsGiven: Int = 0,
    val trocsCompleted: Int = 0,
    val photoUrl: String = "",
    val isSuspended: Boolean = false,
    val warnings: Int = 0,
    val createdAtMs: Long = 0L
) {
    val roleEnum: UserRole
        get() = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.CITOYEN)

    val isMediator: Boolean get() = roleEnum == UserRole.MEDIATEUR
    val isAssociation: Boolean get() = roleEnum == UserRole.ASSOCIATION
    val isCertifiedAssociation: Boolean
        get() = isAssociation && associationStatus == AssociationStatus.CERTIFIED.name
    val level: GrainLevel get() = GrainLevel.forTotal(totalGrainsEarned)
    val isTrusted: Boolean get() = totalGrainsEarned >= GrainRules.TRUSTED_THRESHOLD || isMediator

    /** Note moyenne dérivée (somme des notes / nombre d'avis) : exacte et vérifiable par les règles. */
    val rating: Double get() = if (reviewCount > 0) ratingSum.toDouble() / reviewCount else 0.0

    /** Nom à afficher : pour une association, le nom de l'organisation. */
    val displayName: String
        get() = if (isAssociation && associationName.isNotBlank()) associationName else name

    val initials: String
        get() = displayName.split(" ", "-")
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
            .take(2)
            .ifEmpty { "?" }
}

data class Item(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val condition: String,
    val exchangeType: String,
    val wantedInReturn: String = "",
    val donorId: String,
    val donorName: String,
    val donorRole: String = UserRole.CITOYEN.name,
    val donorCertified: Boolean = false,
    val city: String,
    val neighborhood: String = "",
    val landmark: String = "",
    val approximateLat: Double = 0.0,
    val approximateLng: Double = 0.0,
    /** Calculée côté client par rapport à la position de l'utilisateur (non stockée). */
    val distanceMeters: Int = 0,
    val status: String = ItemStatus.PENDING_MODERATION.name,
    val reservedByUserId: String = "",
    val reservedByName: String = "",
    val donorConfirmed: Boolean = false,
    val receiverConfirmed: Boolean = false,
    val imageB64: String = "",
    val isEmergencyResponse: Boolean = false,
    val targetOrphanageName: String = "",
    val moderationNote: String = "",
    val createdAtMs: Long = 0L
) {
    val statusEnum: ItemStatus
        get() = runCatching { ItemStatus.valueOf(status) }.getOrDefault(ItemStatus.PENDING_MODERATION)

    val isTroc: Boolean get() = exchangeType == ExchangeType.TROC.name
    val categoryEnum: Category
        get() = runCatching { Category.valueOf(category) }.getOrDefault(Category.AUTRE)
    val conditionLabel: String
        get() = runCatching { ItemCondition.valueOf(condition).label }.getOrDefault(condition)
}

data class ItemRequest(
    val id: String,
    val itemId: String,
    val itemTitle: String,
    val itemOwnerId: String,
    val requesterId: String,
    val requesterName: String,
    val message: String = "",
    val graineTopUp: Int = 0,
    val status: String = RequestStatus.PENDING.name,
    val createdAtMs: Long = 0L
) {
    val statusEnum: RequestStatus
        get() = runCatching { RequestStatus.valueOf(status) }.getOrDefault(RequestStatus.PENDING)
}

data class Fundraiser(
    val id: String,
    val associationId: String,
    val associationName: String,
    val title: String,
    val description: String,
    val city: String,
    val neighborhood: String = "",
    val targetFcfa: Long = 0,
    val currentFcfa: Long = 0,
    val targetGrains: Int = 0,
    val currentGrains: Int = 0,
    val contributorsCount: Int = 0,
    val deadlineMs: Long = 0L,
    val category: String = AssociationType.ORPHELINAT.name,
    val status: String = FundraiserStatus.PENDING_REVIEW.name,
    val isEmergency: Boolean = false,
    val payInstructions: String = "",
    val contactPhone: String = "",
    val proofNote: String = "",
    val lastContributionId: String = "",
    val createdAtMs: Long = 0L
) {
    val statusEnum: FundraiserStatus
        get() = runCatching { FundraiserStatus.valueOf(status) }.getOrDefault(FundraiserStatus.PENDING_REVIEW)

    val fcfaProgress: Float
        get() = if (targetFcfa <= 0) 0f else (currentFcfa.toFloat() / targetFcfa.toFloat()).coerceIn(0f, 1f)

    val grainsProgress: Float
        get() = if (targetGrains <= 0) 0f else (currentGrains.toFloat() / targetGrains.toFloat()).coerceIn(0f, 1f)
}

data class Contribution(
    val id: String,
    val fundraiserId: String,
    val fundraiserTitle: String,
    val associationId: String,
    val contributorId: String,
    val contributorName: String,
    val amountFcfa: Long = 0,
    val grains: Int = 0,
    val isAnonymous: Boolean = false,
    val status: String = ContributionStatus.PLEDGED.name,
    val createdAtMs: Long = 0L
) {
    val statusEnum: ContributionStatus
        get() = runCatching { ContributionStatus.valueOf(status) }.getOrDefault(ContributionStatus.PLEDGED)
}

data class CollectionPoint(
    val id: String,
    val name: String,
    val organizationName: String,
    val ownerId: String,
    val type: String,
    val city: String,
    val neighborhood: String,
    val landmark: String,
    val contactPhone: String,
    val openingHours: String,
    val acceptedCategories: String,
    val lat: Double,
    val lng: Double,
    val createdAtMs: Long = 0L
) {
    val typeLabel: String
        get() = runCatching { CollectionPointType.valueOf(type).label }.getOrDefault(type)
}

data class Dispute(
    val id: String,
    val itemId: String,
    val itemTitle: String,
    val reporterId: String,
    val reporterName: String,
    val reportedUserId: String,
    val reportedUserName: String,
    val reason: String,
    val description: String,
    val status: String = DisputeStatus.OPEN.name,
    val resolutionNote: String = "",
    val action: String = DisputeAction.NONE.name,
    val mediatorId: String = "",
    val createdAtMs: Long = 0L
) {
    val statusEnum: DisputeStatus
        get() = runCatching { DisputeStatus.valueOf(status) }.getOrDefault(DisputeStatus.OPEN)
    val reasonLabel: String
        get() = runCatching { DisputeReason.valueOf(reason).label }.getOrDefault(reason)
}

data class ChatThread(
    val id: String,
    val itemId: String,
    val itemTitle: String,
    val ownerId: String,
    val ownerName: String,
    val requesterId: String,
    val requesterName: String,
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastMessageAtMs: Long = 0L
) {
    fun otherName(myId: String): String = if (myId == ownerId) requesterName else ownerName
}

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val isSystem: Boolean = false,
    val createdAtMs: Long = 0L
)

data class Review(
    val id: String,
    val itemId: String,
    val itemTitle: String,
    val authorId: String,
    val authorName: String,
    val targetUserId: String,
    val rating: Int,
    val badge: String = "",
    val comment: String = "",
    val createdAtMs: Long = 0L
)

data class AppNotification(
    val id: String,
    val userId: String,
    val fromUserId: String,
    val type: String,
    val title: String,
    val body: String,
    val refId: String = "",
    val read: Boolean = false,
    val createdAtMs: Long = 0L
)

data class LedgerEntry(
    val id: String,
    val amount: Int,
    val reason: String,
    val refId: String = "",
    val createdAtMs: Long = 0L
) {
    val reasonLabel: String
        get() = runCatching { LedgerReason.valueOf(reason).label }.getOrDefault(reason)
}

// ---------------------------------------------------------------------------------------------
// Lecture des documents Firestore
// ---------------------------------------------------------------------------------------------

private fun DocumentSnapshot.s(field: String): String = getString(field) ?: ""
private fun DocumentSnapshot.i(field: String): Int = getLong(field)?.toInt() ?: 0
private fun DocumentSnapshot.l(field: String): Long = getLong(field) ?: 0L
private fun DocumentSnapshot.d(field: String): Double = getDouble(field) ?: 0.0
private fun DocumentSnapshot.b(field: String): Boolean = getBoolean(field) ?: false
private fun DocumentSnapshot.ts(field: String = "createdAt"): Long =
    getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
        ?: System.currentTimeMillis()

fun DocumentSnapshot.toUserProfile(): UserProfile? {
    if (!exists()) return null
    return UserProfile(
        id = id,
        name = s("name"),
        role = s("role").ifBlank { UserRole.CITOYEN.name },
        city = s("city").ifBlank { "Douala" },
        neighborhood = s("neighborhood"),
        landmark = s("landmark"),
        lat = d("lat"),
        lng = d("lng"),
        radiusKm = (getDouble("radiusKm") ?: 5.0).toFloat(),
        grains = i("grains"),
        totalGrainsEarned = i("totalGrainsEarned"),
        lastLedgerId = s("lastLedgerId"),
        associationName = s("associationName"),
        associationType = s("associationType"),
        associationRegNumber = s("associationRegNumber"),
        associationPhone = s("associationPhone"),
        associationStatus = s("associationStatus").ifBlank { AssociationStatus.NONE.name },
        associationNote = s("associationNote"),
        ratingSum = i("ratingSum"),
        reviewCount = i("reviewCount"),
        lastReviewId = s("lastReviewId"),
        giftsGiven = i("giftsGiven"),
        trocsCompleted = i("trocsCompleted"),
        photoUrl = s("photoUrl"),
        isSuspended = b("isSuspended"),
        warnings = i("warnings"),
        createdAtMs = ts()
    )
}

fun DocumentSnapshot.toItem(): Item? {
    if (!exists()) return null
    return Item(
        id = id,
        title = s("title"),
        description = s("description"),
        category = s("category"),
        condition = s("condition"),
        exchangeType = s("exchangeType"),
        wantedInReturn = s("wantedInReturn"),
        donorId = s("donorId"),
        donorName = s("donorName"),
        donorRole = s("donorRole").ifBlank { UserRole.CITOYEN.name },
        donorCertified = b("donorCertified"),
        city = s("city"),
        neighborhood = s("neighborhood"),
        landmark = s("landmark"),
        approximateLat = d("approximateLat"),
        approximateLng = d("approximateLng"),
        status = s("status").ifBlank { ItemStatus.PENDING_MODERATION.name },
        reservedByUserId = s("reservedByUserId"),
        reservedByName = s("reservedByName"),
        donorConfirmed = b("donorConfirmed"),
        receiverConfirmed = b("receiverConfirmed"),
        imageB64 = s("imageB64"),
        isEmergencyResponse = b("isEmergencyResponse"),
        targetOrphanageName = s("targetOrphanageName"),
        moderationNote = s("moderationNote"),
        createdAtMs = ts()
    )
}

fun DocumentSnapshot.toItemRequest(): ItemRequest? {
    if (!exists()) return null
    return ItemRequest(
        id = id,
        itemId = s("itemId"),
        itemTitle = s("itemTitle"),
        itemOwnerId = s("itemOwnerId"),
        requesterId = s("requesterId"),
        requesterName = s("requesterName"),
        message = s("message"),
        graineTopUp = i("graineTopUp"),
        status = s("status").ifBlank { RequestStatus.PENDING.name },
        createdAtMs = ts()
    )
}

fun DocumentSnapshot.toFundraiser(): Fundraiser? {
    if (!exists()) return null
    return Fundraiser(
        id = id,
        associationId = s("associationId"),
        associationName = s("associationName"),
        title = s("title"),
        description = s("description"),
        city = s("city"),
        neighborhood = s("neighborhood"),
        targetFcfa = l("targetFcfa"),
        currentFcfa = l("currentFcfa"),
        targetGrains = i("targetGrains"),
        currentGrains = i("currentGrains"),
        contributorsCount = i("contributorsCount"),
        deadlineMs = l("deadlineMs"),
        category = s("category"),
        status = s("status").ifBlank { FundraiserStatus.PENDING_REVIEW.name },
        isEmergency = b("isEmergency"),
        payInstructions = s("payInstructions"),
        contactPhone = s("contactPhone"),
        proofNote = s("proofNote"),
        lastContributionId = s("lastContributionId"),
        createdAtMs = ts()
    )
}

fun DocumentSnapshot.toContribution(): Contribution? {
    if (!exists()) return null
    return Contribution(
        id = id,
        fundraiserId = s("fundraiserId"),
        fundraiserTitle = s("fundraiserTitle"),
        associationId = s("associationId"),
        contributorId = s("contributorId"),
        contributorName = s("contributorName"),
        amountFcfa = l("amountFcfa"),
        grains = i("grains"),
        isAnonymous = b("isAnonymous"),
        status = s("status").ifBlank { ContributionStatus.PLEDGED.name },
        createdAtMs = ts()
    )
}

fun DocumentSnapshot.toCollectionPoint(): CollectionPoint? {
    if (!exists()) return null
    return CollectionPoint(
        id = id,
        name = s("name"),
        organizationName = s("organizationName"),
        ownerId = s("ownerId"),
        type = s("type"),
        city = s("city"),
        neighborhood = s("neighborhood"),
        landmark = s("landmark"),
        contactPhone = s("contactPhone"),
        openingHours = s("openingHours"),
        acceptedCategories = s("acceptedCategories"),
        lat = d("lat"),
        lng = d("lng"),
        createdAtMs = ts()
    )
}

fun DocumentSnapshot.toDispute(): Dispute? {
    if (!exists()) return null
    return Dispute(
        id = id,
        itemId = s("itemId"),
        itemTitle = s("itemTitle"),
        reporterId = s("reporterId"),
        reporterName = s("reporterName"),
        reportedUserId = s("reportedUserId"),
        reportedUserName = s("reportedUserName"),
        reason = s("reason"),
        description = s("description"),
        status = s("status").ifBlank { DisputeStatus.OPEN.name },
        resolutionNote = s("resolutionNote"),
        action = s("action").ifBlank { DisputeAction.NONE.name },
        mediatorId = s("mediatorId"),
        createdAtMs = ts()
    )
}

fun DocumentSnapshot.toChatThread(): ChatThread? {
    if (!exists()) return null
    return ChatThread(
        id = id,
        itemId = s("itemId"),
        itemTitle = s("itemTitle"),
        ownerId = s("ownerId"),
        ownerName = s("ownerName"),
        requesterId = s("requesterId"),
        requesterName = s("requesterName"),
        lastMessage = s("lastMessage"),
        lastSenderId = s("lastSenderId"),
        lastMessageAtMs = getTimestamp("lastMessageAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?.toDate()?.time ?: 0L
    )
}

fun DocumentSnapshot.toChatMessage(): ChatMessage? {
    if (!exists()) return null
    return ChatMessage(
        id = id,
        senderId = s("senderId"),
        senderName = s("senderName"),
        text = s("text"),
        isSystem = b("isSystem"),
        createdAtMs = ts()
    )
}

fun DocumentSnapshot.toReview(): Review? {
    if (!exists()) return null
    return Review(
        id = id,
        itemId = s("itemId"),
        itemTitle = s("itemTitle"),
        authorId = s("authorId"),
        authorName = s("authorName"),
        targetUserId = s("targetUserId"),
        rating = i("rating"),
        badge = s("badge"),
        comment = s("comment"),
        createdAtMs = ts()
    )
}

fun DocumentSnapshot.toNotification(): AppNotification? {
    if (!exists()) return null
    return AppNotification(
        id = id,
        userId = s("userId"),
        fromUserId = s("fromUserId"),
        type = s("type"),
        title = s("title"),
        body = s("body"),
        refId = s("refId"),
        read = b("read"),
        createdAtMs = ts()
    )
}

fun DocumentSnapshot.toLedgerEntry(): LedgerEntry? {
    if (!exists()) return null
    return LedgerEntry(
        id = id,
        amount = i("amount"),
        reason = s("reason"),
        refId = s("refId"),
        createdAtMs = ts()
    )
}
