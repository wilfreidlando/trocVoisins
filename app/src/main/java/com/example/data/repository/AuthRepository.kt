package com.example.data.repository

import com.example.data.model.AssociationStatus
import com.example.data.model.GrainRules
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.toUserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Données saisies à l'inscription (étape « profil »). */
data class OnboardingForm(
    val name: String,
    val role: UserRole,
    val city: String,
    val neighborhood: String,
    val landmark: String,
    val lat: Double,
    val lng: Double,
    val associationName: String = "",
    val associationType: String = "",
    val associationRegNumber: String = "",
    val associationPhone: String = ""
)

class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun profileFlow(uid: String): Flow<UserProfile?> =
        db.collection("users").document(uid).docFlow { it.toUserProfile() }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    fun signOut() {
        auth.signOut()
    }

    /** Vrai tant que personne n'a effectué la première installation (création de l'administrateur). */
    suspend fun adminExists(): Boolean = try {
        db.collection("meta").document("admin").get().await().exists()
    } catch (e: Exception) {
        true // dans le doute, on ne propose pas l'option
    }

    /** Crée le profil du citoyen ou de l'association. Une association démarre « en attente de certification ». */
    suspend fun createProfile(user: FirebaseUser, form: OnboardingForm): Result<Unit> = runCatching {
        val isAssociation = form.role == UserRole.ASSOCIATION
        db.collection("users").document(user.uid).set(profileMap(user, form, form.role)).await()
        Unit
    }

    /**
     * Première installation : crée l'administrateur. Le code de configuration doit correspondre
     * à celui inscrit dans firestore.rules ; sinon l'écriture groupée est refusée.
     */
    suspend fun createAdminProfile(user: FirebaseUser, form: OnboardingForm, setupCode: String): Result<Unit> =
        runCatching {
            val batch = db.batch()
            batch.set(
                db.collection("adminSetup").document(setupCode.trim()),
                mapOf("uid" to user.uid, "createdAt" to serverTime())
            )
            batch.set(
                db.collection("meta").document("admin"),
                mapOf("uid" to user.uid, "createdAt" to serverTime())
            )
            batch.set(
                db.collection("users").document(user.uid),
                profileMap(user, form, UserRole.MEDIATEUR)
            )
            batch.commit().await()
            Unit
        }

    private fun profileMap(user: FirebaseUser, form: OnboardingForm, role: UserRole): Map<String, Any> {
        val isAssociation = role == UserRole.ASSOCIATION
        val map = mutableMapOf<String, Any>(
            "name" to form.name.trim(),
            "role" to role.name,
            "city" to form.city,
            "neighborhood" to form.neighborhood.trim(),
            "landmark" to form.landmark.trim(),
            "lat" to form.lat,
            "lng" to form.lng,
            "radiusKm" to 5.0,
            "grains" to GrainRules.WELCOME,
            "totalGrainsEarned" to 0,
            "lastLedgerId" to "",
            "ratingSum" to 0,
            "reviewCount" to 0,
            "lastReviewId" to "",
            "giftsGiven" to 0,
            "trocsCompleted" to 0,
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "isSuspended" to false,
            "warnings" to 0,
            "associationStatus" to (if (isAssociation) AssociationStatus.PENDING.name else AssociationStatus.NONE.name),
            "associationNote" to "",
            "createdAt" to serverTime(),
            "updatedAt" to serverTime()
        )
        if (isAssociation) {
            map["associationName"] = form.associationName.trim()
            map["associationType"] = form.associationType
            map["associationRegNumber"] = form.associationRegNumber.trim()
            map["associationPhone"] = form.associationPhone.trim()
        } else {
            map["associationName"] = ""
            map["associationType"] = ""
            map["associationRegNumber"] = ""
            map["associationPhone"] = ""
        }
        return map
    }

    /** Mise à jour de la localisation et du rayon de recherche. */
    suspend fun updateLocation(
        uid: String,
        city: String,
        neighborhood: String,
        landmark: String,
        lat: Double,
        lng: Double,
        radiusKm: Float
    ): Result<Unit> = runCatching {
        db.collection("users").document(uid).update(
            mapOf(
                "city" to city,
                "neighborhood" to neighborhood.trim(),
                "landmark" to landmark.trim(),
                "lat" to lat,
                "lng" to lng,
                "radiusKm" to radiusKm.toDouble(),
                "updatedAt" to serverTime()
            )
        ).await()
        Unit
    }

    /** Association refusée : elle peut corriger ses informations et redemander la certification. */
    suspend fun resubmitAssociation(
        uid: String,
        name: String,
        type: String,
        regNumber: String,
        phone: String
    ): Result<Unit> = runCatching {
        db.collection("users").document(uid).update(
            mapOf(
                "associationName" to name.trim(),
                "associationType" to type,
                "associationRegNumber" to regNumber.trim(),
                "associationPhone" to phone.trim(),
                "associationStatus" to AssociationStatus.PENDING.name,
                "associationNote" to "",
                "updatedAt" to serverTime()
            )
        ).await()
        Unit
    }
}
