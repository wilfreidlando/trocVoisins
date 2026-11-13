package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Vocabulaire unique de l'application.
 * Les mêmes valeurs (name) sont stockées dans Firestore et lues par les règles de sécurité.
 */

enum class UserRole(val label: String) {
    CITOYEN("Citoyen solidaire"),
    ASSOCIATION("Association"),
    MEDIATEUR("Médiateur")
}

enum class AssociationStatus(val label: String) {
    NONE("Non concerné"),
    PENDING("En attente de certification"),
    CERTIFIED("Certifiée"),
    REJECTED("Certification refusée")
}

enum class AssociationType(val label: String) {
    ORPHELINAT("Orphelinat"),
    PAROISSE("Paroisse / centre religieux"),
    CHEFFERIE("Chefferie traditionnelle"),
    CENTRE_SOCIAL("Centre social de quartier"),
    RESSOURCERIE("Ressourcerie solidaire"),
    ECOLE("École / centre de formation"),
    AUTRE("Autre association")
}

enum class ExchangeType(val label: String, val description: String) {
    DON("Don", "Gratuit : vous offrez l'objet à un voisin ou à une association"),
    TROC("Troc", "Échange : vous cherchez un autre objet en retour")
}

enum class ItemCondition(val label: String) {
    NEUF("Neuf"),
    TRES_BON_ETAT("Très bon état"),
    BON_ETAT("Bon état"),
    A_REPARER("À réparer")
}

enum class ItemStatus(val label: String) {
    PENDING_MODERATION("En validation"),
    AVAILABLE("Disponible"),
    RESERVED("Réservé"),
    COMPLETED("Échange terminé"),
    REJECTED("Refusé"),
    SUSPENDED("Suspendu")
}

enum class RequestStatus(val label: String) {
    PENDING("En attente"),
    ACCEPTED("Acceptée"),
    DECLINED("Refusée"),
    CANCELLED("Annulée")
}

enum class Category(val label: String, val icon: ImageVector) {
    TOUT("Tous", Icons.Default.MoreHoriz),
    SCOLAIRE_EDUCATION("Scolaire", Icons.Default.School),
    VETEMENTS_ENFANTS("Vêtements & bébés", Icons.Default.ChildCare),
    MAISON_DECO("Maison", Icons.Default.Chair),
    BRICOLAGE_OUTILS("Outils", Icons.Default.Build),
    ELECTROMENAGER("Électroménager", Icons.Default.Kitchen),
    LIVRES_LOISIRS("Livres & culture", Icons.Default.MenuBook),
    PLANTES_JARDIN("Plantes & jardin", Icons.Default.LocalFlorist),
    AUTRE("Autres", Icons.Default.MoreHoriz)
}

enum class FundraiserStatus(val label: String) {
    PENDING_REVIEW("En validation"),
    ACTIVE("En cours"),
    FUNDED("Objectif atteint"),
    CLOSED("Clôturée"),
    REJECTED("Refusée")
}

enum class ContributionStatus(val label: String) {
    PLEDGED("Promesse à confirmer"),
    CONFIRMED("Reçue"),
    REJECTED("Non reçue")
}

enum class DisputeReason(val label: String) {
    ITEM_MISMATCH("Objet non conforme à l'annonce"),
    NO_SHOW("Absence au rendez-vous"),
    COMMERCIAL_RESELL("Revente commerciale"),
    SUSPICIOUS_BEHAVIOR("Comportement suspect ou impoli"),
    OTHER("Autre motif")
}

enum class DisputeStatus(val label: String) {
    OPEN("Ouvert"),
    UNDER_REVIEW("En cours d'arbitrage"),
    RESOLVED("Résolu"),
    DISMISSED("Rejeté")
}

/** Décision du médiateur lors de la clôture d'un litige : elle a un effet réel. */
enum class DisputeAction(val label: String) {
    NONE("Aucune sanction"),
    SUSPEND_ITEM("Suspendre l'annonce"),
    WARN_USER("Avertir la personne signalée"),
    SUSPEND_USER("Suspendre le compte signalé")
}

enum class CollectionPointType(val label: String) {
    ORPHELINAT("Orphelinat"),
    PAROISSE("Paroisse / centre religieux"),
    CHEFFERIE("Chefferie traditionnelle"),
    CENTRE_SOCIAL("Centre social de quartier"),
    RESSOURCERIE("Ressourcerie solidaire")
}

enum class NotificationType { EXCHANGE, MESSAGE, MODERATION, DISPUTE, FUNDRAISER, ASSOCIATION, GRAINS }

enum class LedgerReason(val label: String) {
    DON_DONOR("Don remis"),
    DON_RECEIVED("Don reçu"),
    TROC_DONE("Troc réussi"),
    TOPUP_PAID("Complément de troc versé"),
    TOPUP_RECEIVED("Complément de troc reçu"),
    REVIEW("Avis laissé"),
    FUNDRAISER("Soutien à une cagnotte")
}

/** Niveaux de confiance basés sur le total de Graines GAGNÉES (jamais sur le solde). */
enum class GrainLevel(val label: String, val minEarned: Int, val emoji: String) {
    GRAINE("Graine", 0, "🌱"),
    POUSSE("Pousse", 60, "🌿"),
    ARBRE("Arbre", 200, "🌳"),
    FORET("Forêt", 500, "🌲");

    companion object {
        fun forTotal(totalEarned: Int): GrainLevel =
            entries.lastOrNull { totalEarned >= it.minEarned } ?: GRAINE

        fun next(level: GrainLevel): GrainLevel? = entries.getOrNull(level.ordinal + 1)
    }
}

/** Règles de l'économie de Graines (une seule source de vérité, reprise dans firestore.rules). */
object GrainRules {
    /** Capital de départ : permet de recevoir avant d'avoir donné. */
    const val WELCOME = 20
    const val DON_DONOR = 20
    /** Coût pour recevoir un don d'un citoyen (0 = réception 100 % gratuite). Les associations donnent gratuitement. */
    const val DON_RECEIVE_COST = 5
    const val TROC_EACH = 10
    const val REVIEW = 2
    const val MAX_TOPUP = 30
    /** À partir de ce total gagné (niveau Pousse), les annonces sont publiées sans attendre la modération. */
    const val TRUSTED_THRESHOLD = 60
}

object Cities {
    data class City(val name: String, val lat: Double, val lng: Double)

    val all = listOf(
        City("Douala", 4.0511, 9.7679),
        City("Yaoundé", 3.8480, 11.5021),
        City("Bafoussam", 5.4737, 10.4179),
        City("Garoua", 9.3014, 13.3977)
    )

    val names: List<String> get() = all.map { it.name }

    fun find(name: String): City = all.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: all.first()
}
