package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.local.SeedData
import com.example.data.local.TrocDao
import com.example.util.TrocNotificationManager
import com.example.data.model.Category
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CollectionPointEntity
import com.example.data.model.DisputeEntity
import com.example.data.model.DisputeStatus
import com.example.data.model.ExchangeType
import com.example.data.model.FundraiserEntity
import com.example.data.model.ItemCondition
import com.example.data.model.ItemEntity
import com.example.data.model.ItemStatus
import com.example.data.model.ReviewEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.firebase.OperationType
import com.example.firebase.getFirebaseAuthSafe
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.InputStream
import java.util.UUID

private const val TAG_AI = "TrocAiVision"

data class SmartAnalysisResult(
    val detectedTitle: String,
    val suggestedCategory: Category,
    val suggestedCondition: ItemCondition,
    val estimatedCo2SavedKg: Float,
    val recommendedGrains: Int,
    val matchingEmergencyNeed: String? = null
)

class TrocRepository(
    private val dao: TrocDao,
    private val firestore: FirebaseFirestore? = null,
    private val context: Context? = null
) {

    // Lazy initialization of Firebase AI Logic model with gemini-3.8-flash
    private val aiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedIfNeeded()
        }
    }

    private suspend fun seedIfNeeded() {
        // Clear all mock/sample data and ensure only adminUser is present in the database
        dao.clearAllItems()
        dao.clearAllFundraisers()
        dao.clearAllCollectionPoints()
        dao.clearAllDisputes()
        dao.clearAllMessages()
        dao.clearAllReviews()
        dao.clearAllUsers()
        dao.insertUser(SeedData.adminUser)
    }

    suspend fun resetDatabaseToAdminOnly() {
        dao.clearAllItems()
        dao.clearAllFundraisers()
        dao.clearAllCollectionPoints()
        dao.clearAllDisputes()
        dao.clearAllMessages()
        dao.clearAllReviews()
        dao.clearAllUsers()
        dao.insertUser(SeedData.adminUser)
    }

    // Items
    fun getAllItems(): Flow<List<ItemEntity>> = dao.getAllItems()

    fun getAllItemsForAdmin(): Flow<List<ItemEntity>> = dao.getAllItemsForAdmin()

    fun getReportedItems(): Flow<List<ItemEntity>> = dao.getReportedItems()

    fun getItem(id: String): Flow<ItemEntity?> = dao.getItemById(id)

    fun getFilteredItems(
        selectedCategory: Category,
        exchangeFilter: ExchangeType?,
        searchQuery: String,
        selectedCity: String,
        maxRadiusKm: Float
    ): Flow<List<ItemEntity>> {
        return combine(dao.getAllItems()) { itemsArray ->
            val all = itemsArray[0]
            all.filter { item ->
                val matchesCategory = if (selectedCategory == Category.TOUT) true
                else item.category == selectedCategory.name

                val matchesExchange = if (exchangeFilter == null) true
                else item.exchangeType == exchangeFilter.name

                val matchesCity = if (selectedCity.isBlank() || selectedCity == "Toutes les villes") true
                else item.city.equals(selectedCity, ignoreCase = true)

                val matchesSearch = if (searchQuery.isBlank()) true
                else item.title.contains(searchQuery, ignoreCase = true) ||
                        item.description.contains(searchQuery, ignoreCase = true) ||
                        item.neighborhood.contains(searchQuery, ignoreCase = true) ||
                        item.landmark.contains(searchQuery, ignoreCase = true)

                val matchesRadius = (item.distanceMeters / 1000f) <= maxRadiusKm

                matchesCategory && matchesExchange && matchesCity && matchesSearch && matchesRadius
            }
        }
    }

    suspend fun publishItem(
        title: String,
        description: String,
        category: Category,
        condition: String,
        exchangeType: ExchangeType,
        wantedInReturn: String,
        imageUri: String,
        isEmergencyResponse: Boolean,
        targetOrphanageName: String
    ): String {
        val user = dao.getCurrentUser().firstOrNull() ?: SeedData.currentUser
        val id = "item_cm_" + UUID.randomUUID().toString().take(8)
        val newItem = ItemEntity(
            id = id,
            title = title,
            description = description,
            category = category.name,
            condition = condition,
            exchangeType = exchangeType.name,
            wantedInReturn = wantedInReturn,
            grainsReward = if (exchangeType == ExchangeType.DON) 20 else 15,
            grainsCost = if (exchangeType == ExchangeType.TROC) 15 else 0,
            imageUri = imageUri,
            donorId = user.id,
            donorName = user.name,
            donorRole = user.role,
            donorRating = user.rating,
            donorReviewsCount = user.reviewCount,
            donorVerified = user.isVerified,
            city = user.city,
            neighborhood = user.neighborhood,
            landmark = user.landmark,
            approximateLat = 4.0515 + (Math.random() - 0.5) * 0.015,
            approximateLng = 9.7675 + (Math.random() - 0.5) * 0.015,
            distanceMeters = 300,
            status = ItemStatus.AVAILABLE.name,
            isEmergencyResponse = isEmergencyResponse,
            targetOrphanageName = targetOrphanageName,
            createdAt = System.currentTimeMillis()
        )
        dao.insertItem(newItem)
        if (exchangeType == ExchangeType.DON) {
            dao.rewardGivingUser(user.id)
        }

        // Cloud sync to Firestore if authenticated
        val authUser = getFirebaseAuthSafe()?.currentUser
        if (firestore != null && authUser != null) {
            try {
                val cloudPayload = mapOf(
                    "id" to id,
                    "ownerId" to authUser.uid,
                    "ownerName" to user.name,
                    "title" to title,
                    "description" to description,
                    "category" to category.label,
                    "condition" to condition,
                    "exchangeType" to exchangeType.name,
                    "desiredExchange" to wantedInReturn,
                    "grainesValue" to (if (exchangeType == ExchangeType.TROC) 15 else 0),
                    "city" to user.city,
                    "neighborhood" to user.neighborhood,
                    "approxLandmark" to user.landmark,
                    "status" to "DISPONIBLE",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("items").document(id).set(cloudPayload).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "items/$id")
            }
        }
        return id
    }

    suspend fun reserveItem(itemId: String) {
        dao.updateItemStatus(itemId, ItemStatus.RESERVED.name)
        val notice = ChatMessageEntity(
            id = "sys_" + System.currentTimeMillis(),
            itemId = itemId,
            senderId = "system",
            senderName = "Médiation TrocVoisins",
            text = "🤝 Objet réservé ! L'annonce est temporairement bloquée le temps de votre rencontre fraternelle.",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            isSystemNotice = true
        )
        dao.insertMessage(notice)
        context?.let { ctx ->
            TrocNotificationManager.showPushNotification(
                ctx,
                "Objet Réservé 🤝",
                "Votre réservation a bien été enregistrée. Préparez la rencontre au point de repère."
            )
        }

        val authUser = getFirebaseAuthSafe()?.currentUser
        if (firestore != null && authUser != null) {
            try {
                firestore.collection("items").document(itemId).update(
                    mapOf(
                        "status" to "RESERVE",
                        "reservedByUserId" to authUser.uid
                    )
                ).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, "items/$itemId")
            }
        }
    }

    suspend fun completeItemExchange(itemId: String) {
        dao.updateItemStatus(itemId, ItemStatus.COMPLETED.name)
        val notice = ChatMessageEntity(
            id = "sys_done_" + System.currentTimeMillis(),
            itemId = itemId,
            senderId = "system",
            senderName = "Médiation TrocVoisins",
            text = "🎉 Don/Troc finalisé avec succès ! Merci pour ce geste solidaire qui aide notre communauté.",
            timestamp = System.currentTimeMillis(),
            isFromMe = false,
            isSystemNotice = true
        )
        dao.insertMessage(notice)
    }

    suspend fun reportItemToMediation(itemId: String, itemTitle: String, donorId: String, donorName: String, reason: String, details: String) {
        val user = dao.getCurrentUser().firstOrNull() ?: SeedData.currentUser
        dao.reportItem(itemId, reason)
        val dispId = "disp_" + UUID.randomUUID().toString().take(8)
        val dispute = DisputeEntity(
            id = dispId,
            itemId = itemId,
            itemTitle = itemTitle,
            reporterId = user.id,
            reporterName = user.name,
            reportedUserId = donorId,
            reportedUserName = donorName,
            reason = reason,
            description = details,
            status = DisputeStatus.OPEN.name,
            timestamp = System.currentTimeMillis()
        )
        dao.insertDispute(dispute)

        val authUser = getFirebaseAuthSafe()?.currentUser
        if (firestore != null && authUser != null) {
            try {
                val cloudDispute = mapOf(
                    "id" to dispId,
                    "itemId" to itemId,
                    "itemTitle" to itemTitle,
                    "reportedByUserId" to authUser.uid,
                    "reporterName" to user.name,
                    "targetUserId" to donorId,
                    "reason" to reason,
                    "status" to "OUVERT",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("disputes").document(dispId).set(cloudDispute).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, "disputes/$dispId")
            }
        }
    }

    // Fundraisers
    fun getFundraisers(): Flow<List<FundraiserEntity>> = dao.getAllFundraisers()

    suspend fun contributeToFundraiser(fundraiserId: String, amountFcfa: Long, grains: Int) {
        val user = dao.getCurrentUser().firstOrNull() ?: SeedData.currentUser
        dao.contributeToFundraiser(fundraiserId, amountFcfa, grains)
        if (grains > 0) {
            dao.deductGrains(user.id, grains)
        }

        if (firestore != null) {
            try {
                firestore.collection("fundraisers").document(fundraiserId).update(
                    mapOf(
                        "currentFcfa" to FieldValue.increment(amountFcfa.toDouble()),
                        "currentGraines" to FieldValue.increment(grains.toDouble()),
                        "contributorsCount" to FieldValue.increment(1)
                    )
                ).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, "fundraisers/$fundraiserId")
            }
        }
    }

    // Collection Points
    fun getCollectionPoints(): Flow<List<CollectionPointEntity>> = dao.getAllCollectionPoints()

    // Disputes & Admin
    fun getDisputes(): Flow<List<DisputeEntity>> = dao.getAllDisputes()

    suspend fun resolveDispute(disputeId: String, status: String, notes: String) {
        dao.resolveDispute(disputeId, status, notes)
        val authUser = getFirebaseAuthSafe()?.currentUser
        if (firestore != null && authUser != null) {
            try {
                firestore.collection("disputes").document(disputeId).update(
                    mapOf(
                        "status" to status,
                        "resolutionNote" to notes,
                        "mediatorId" to authUser.uid,
                        "resolvedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, "disputes/$disputeId")
            }
        }
    }

    suspend fun approveItem(itemId: String) {
        dao.updateItemApproval(itemId, true)
    }

    suspend fun rejectItem(itemId: String) {
        dao.deleteItem(itemId)
    }

    // Chat
    fun getChatMessages(itemId: String): Flow<List<ChatMessageEntity>> = dao.getMessagesForItem(itemId)

    suspend fun sendChatMessage(itemId: String, text: String, neighborName: String) {
        val user = dao.getCurrentUser().firstOrNull() ?: SeedData.currentUser
        val myMessage = ChatMessageEntity(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            itemId = itemId,
            senderId = user.id,
            senderName = user.name,
            text = text,
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        )
        dao.insertMessage(myMessage)

        CoroutineScope(Dispatchers.IO).launch {
            delay(1200)
            val replyText = when {
                text.contains("disponible", ignoreCase = true) ->
                    "Oui Frère/Sœur, c'est bien disponible ! Je suis disponible au carrefour cet après-midi vers 17h."
                text.contains("carrefour", ignoreCase = true) || text.contains("ndokoti", ignoreCase = true) || text.contains("bastos", ignoreCase = true) ->
                    "Parfait pour ce carrefour ! Devant la pharmacie ou la station, c'est très sécurisé."
                text.contains("merci", ignoreCase = true) ->
                    "De rien, nous sommes ensemble ! La solidarité avant tout."
                text.contains("orphelinat", ignoreCase = true) ->
                    "Merci infiniment pour les enfants, ce don est une vraie bénédiction."
                else ->
                    "D'accord, c'est bien noté. On se tient informés par message pour la remise."
            }
            val reply = ChatMessageEntity(
                id = "reply_" + UUID.randomUUID().toString().take(8),
                itemId = itemId,
                senderId = "neighbor",
                senderName = neighborName,
                text = replyText,
                timestamp = System.currentTimeMillis(),
                isFromMe = false
            )
            dao.insertMessage(reply)
        }
    }

    /**
     * Smart Photo & Description Analysis using Firebase AI Logic (gemini-3.8-flash)
     * with graceful local intelligence fallback.
     */
    suspend fun analyzePhoto(hint: String, imageUri: String? = null): SmartAnalysisResult = withContext(Dispatchers.IO) {
        // Attempt AI analysis with Firebase AI Logic
        try {
            val bitmap = imageUri?.let { loadBitmapFromUri(it) }
            val prompt = """
                Tu es l'assistant IA solidaire de l'application citoyenne camerounaise 'TrocVoisins CM'.
                Analyse cet objet ou cette description: '$hint'.
                Fournis une réponse au format JSON strict avec les clés:
                - title: nom concis de l'objet
                - category: une catégorie parmi ['SCOLAIRE_EDUCATION', 'VETEMENTS_ENFANTS', 'ELECTROMENAGER', 'BRICOLAGE_OUTILS', 'BEBE_PUERICULTURE', 'MAISON_DECO', 'LIVRES_ROMANS', 'AUTRE']
                - condition: état parmi ['NEUF', 'TRES_BON_ETAT', 'BON_ETAT', 'A_REPARER']
                - co2SavedKg: estimation de kg de CO2 économisés (nombre flottant)
                - grainsReward: valeur conseillée en Graines de Solidarité (entier entre 10 et 30)
                - emergencyNeed: mentionne si cela correspond à un besoin urgent d'orphelinat ou d'école au Cameroun (ou null)
            """.trimIndent()

            val response = withTimeout(30_000L) {
                if (bitmap != null) {
                    aiModel.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )
                } else {
                    aiModel.generateContent(prompt)
                }
            }

            val responseText = response.text
            if (!responseText.isNullOrBlank()) {
                val json = JSONObject(responseText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim())
                val catStr = json.optString("category", "MAISON_DECO")
                val condStr = json.optString("condition", "BON_ETAT")
                val category = Category.entries.find { it.name.equals(catStr, ignoreCase = true) } ?: Category.MAISON_DECO
                val condition = ItemCondition.entries.find { it.name.equals(condStr, ignoreCase = true) } ?: ItemCondition.BON_ETAT

                return@withContext SmartAnalysisResult(
                    detectedTitle = json.optString("title", hint),
                    suggestedCategory = category,
                    suggestedCondition = condition,
                    estimatedCo2SavedKg = json.optDouble("co2SavedKg", 6.5).toFloat(),
                    recommendedGrains = json.optInt("grainsReward", 20),
                    matchingEmergencyNeed = json.optString("emergencyNeed").takeIf { it.isNotBlank() && it != "null" }
                )
            }
        } catch (e: Exception) {
            Log.w(TAG_AI, "Firebase AI Logic analysis failed or offline; using local Cameroon heuristic: ${e.message}")
        }

        // Local heuristic fallback
        val lower = hint.lowercase()
        when {
            lower.contains("sac") || lower.contains("cahier") || lower.contains("livre") || lower.contains("scolaire") -> {
                SmartAnalysisResult(
                    detectedTitle = "Fournitures scolaires & Manuels d'enseignement",
                    suggestedCategory = Category.SCOLAIRE_EDUCATION,
                    suggestedCondition = ItemCondition.TRES_BON_ETAT,
                    estimatedCo2SavedKg = 5.2f,
                    recommendedGrains = 20,
                    matchingEmergencyNeed = "L'Orphelinat Sainte-Thérèse de Deido recherche urgemment ce type de fournitures !"
                )
            }
            lower.contains("habit") || lower.contains("bébé") || lower.contains("vetement") || lower.contains("poussette") -> {
                SmartAnalysisResult(
                    detectedTitle = "Vêtements d'enfants & Accessoires puériculture",
                    suggestedCategory = Category.VETEMENTS_ENFANTS,
                    suggestedCondition = ItemCondition.BON_ETAT,
                    estimatedCo2SavedKg = 8.5f,
                    recommendedGrains = 15,
                    matchingEmergencyNeed = "La pouponnière locale a un besoin prioritaire pour des nourrissons."
                )
            }
            lower.contains("ventilateur") || lower.contains("fer") || lower.contains("machine") || lower.contains("mixeur") -> {
                SmartAnalysisResult(
                    detectedTitle = "Appareil électroménager fonctionnel",
                    suggestedCategory = Category.ELECTROMENAGER,
                    suggestedCondition = ItemCondition.BON_ETAT,
                    estimatedCo2SavedKg = 14.0f,
                    recommendedGrains = 25,
                    matchingEmergencyNeed = null
                )
            }
            lower.contains("outil") || lower.contains("bricolage") || lower.contains("couture") -> {
                SmartAnalysisResult(
                    detectedTitle = "Outillage artisanal & Bricolage utile",
                    suggestedCategory = Category.BRICOLAGE_OUTILS,
                    suggestedCondition = ItemCondition.BON_ETAT,
                    estimatedCo2SavedKg = 18.2f,
                    recommendedGrains = 25,
                    matchingEmergencyNeed = "L'atelier couture solidaire d'Akwa recherche des machines de ce type !"
                )
            }
            else -> {
                SmartAnalysisResult(
                    detectedTitle = if (hint.isNotBlank()) hint.replaceFirstChar { it.uppercase() } else "Objet solidaire réemployable",
                    suggestedCategory = Category.MAISON_DECO,
                    suggestedCondition = ItemCondition.BON_ETAT,
                    estimatedCo2SavedKg = 6.0f,
                    recommendedGrains = 15,
                    matchingEmergencyNeed = null
                )
            }
        }
    }

    private fun loadBitmapFromUri(uriString: String): Bitmap? {
        return try {
            val ctx = context ?: return null
            val uri = Uri.parse(uriString)
            val stream: InputStream? = ctx.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            null
        }
    }

    // User Profile
    fun getCurrentUser(): Flow<UserEntity?> = dao.getCurrentUser()

    suspend fun switchUserRole(role: UserRole) {
        val user = dao.getCurrentUser().firstOrNull() ?: SeedData.currentUser
        val isAdmin = role == UserRole.ADMIN_MEDIATEUR
        dao.switchUserRole(user.id, role.name, isAdmin)
    }

    suspend fun updateLocation(city: String, neighborhood: String, landmark: String, radiusKm: Float) {
        val user = dao.getCurrentUser().firstOrNull() ?: SeedData.currentUser
        dao.updateCameroonLocation(user.id, city, neighborhood, landmark, radiusKm)
    }

    fun getReviews(userId: String): Flow<List<ReviewEntity>> = dao.getReviewsForUser(userId)

    suspend fun addReview(targetUserId: String, rating: Int, badge: String, comment: String) {
        val user = dao.getCurrentUser().firstOrNull() ?: SeedData.currentUser
        val review = ReviewEntity(
            id = "rev_" + UUID.randomUUID().toString().take(8),
            targetUserId = targetUserId,
            authorName = "${user.name} (${user.neighborhood})",
            rating = rating,
            badge = badge,
            comment = comment,
            date = "Aujourd'hui"
        )
        dao.insertReview(review)
    }

    suspend fun syncFirebaseUser(firebaseUser: com.google.firebase.auth.FirebaseUser, role: UserRole? = null) {
        val existing = dao.getUserById(firebaseUser.uid)
        val displayName = firebaseUser.displayName.takeIf { !it.isNullOrBlank() }
            ?: existing?.name
            ?: firebaseUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
            ?: "Citoyen Solidaire"
        val photoUrl = firebaseUser.photoUrl?.toString() ?: existing?.photoUrl
        val initials = displayName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(2).uppercase().ifEmpty { "CS" }

        val userEntity = existing?.copy(
            name = displayName,
            email = firebaseUser.email ?: existing.email,
            photoUrl = photoUrl,
            avatarInitials = initials,
            role = role?.name ?: existing.role
        ) ?: UserEntity(
            id = firebaseUser.uid,
            name = displayName,
            email = firebaseUser.email ?: "",
            photoUrl = photoUrl,
            avatarInitials = initials,
            role = (role ?: UserRole.CITOYEN).name,
            city = "Douala",
            neighborhood = "Akwa",
            landmark = "Carrefour Salle des Fêtes",
            solidarityGrains = 50,
            isVerified = true
        )

        dao.insertUser(userEntity)

        if (firestore != null) {
            try {
                val cloudUser = mapOf(
                    "userId" to firebaseUser.uid,
                    "name" to userEntity.name,
                    "email" to userEntity.email,
                    "role" to userEntity.role,
                    "city" to userEntity.city,
                    "neighborhood" to userEntity.neighborhood,
                    "landmark" to userEntity.landmark,
                    "grainesBalance" to userEntity.solidarityGrains,
                    "rating" to userEntity.rating.toDouble(),
                    "reviewsCount" to userEntity.reviewCount,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                firestore.collection("users").document(firebaseUser.uid).set(cloudUser, com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, "users/${firebaseUser.uid}")
            }
        }
    }

    suspend fun proposeBarter(itemId: String, proofDescription: String, proofPhotoUri: String?) {
        val user = dao.getCurrentUser().firstOrNull() ?: SeedData.currentUser
        val proposalText = "🔄 Proposition de Troc par ${user.name} :\n- Objet proposé : $proofDescription" +
                if (!proofPhotoUri.isNullOrBlank()) "\n- Photo-preuve jointe" else ""

        val message = ChatMessageEntity(
            id = "barter_" + UUID.randomUUID().toString().take(8),
            itemId = itemId,
            senderId = user.id,
            senderName = user.name,
            text = proposalText,
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        )
        dao.insertMessage(message)

        val notice = ChatMessageEntity(
            id = "sys_barter_" + UUID.randomUUID().toString().take(8),
            itemId = itemId,
            senderId = "system",
            senderName = "Médiation TrocVoisins",
            text = "⏳ Preuve de troc transmise au propriétaire. En attente de son acceptation pour valider et réserver l'objet.",
            timestamp = System.currentTimeMillis() + 100,
            isFromMe = false,
            isSystemNotice = true
        )
        dao.insertMessage(notice)
        context?.let { ctx ->
            TrocNotificationManager.showPushNotification(
                ctx,
                "Proposition de Troc 🔄",
                "Votre preuve de troc a été transmise au propriétaire. En attente de validation."
            )
        }
    }
}
