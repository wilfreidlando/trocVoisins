package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector

enum class UserRole(val label: String, val badgeColor: Long) {
    CITOYEN("Citoyen Solidaire", 0xFF1E6F5C),
    ASSOCIATION("Association / Orphelinat", 0xFF0277BD),
    ADMIN_MEDIATEUR("Administrateur & Médiateur", 0xFFD32F2F)
}

enum class ExchangeType(val label: String, val description: String) {
    DON("Don caritatif gratuit", "100% gratuit, destiné aux familles ou orphelinats"),
    TROC("Troc solidaire en Graines", "Échange sans argent basé sur les Graines de Solidarité")
}

enum class ItemCondition(val label: String) {
    NEUF("Neuf"),
    TRES_BON_ETAT("Très bon état"),
    BON_ETAT("Bon état"),
    A_REPARER("À réparer / Bricoleur")
}

enum class ItemStatus(val label: String) {
    AVAILABLE("Disponible"),
    RESERVED("Réservé pour rencontre"),
    COMPLETED("Donné / Échangé"),
    PENDING_MODERATION("En cours de modération"),
    REPORTED("Signalé à la médiation")
}

enum class Category(val label: String, val icon: ImageVector) {
    TOUT("Tous", Icons.Default.MoreHoriz),
    SCOLAIRE_EDUCATION("Scolaire & Éducation", Icons.Default.School),
    VETEMENTS_ENFANTS("Vêtements & Bébés", Icons.Default.ChildCare),
    MAISON_DECO("Maison & Vaisselle", Icons.Default.Chair),
    BRICOLAGE_OUTILS("Bricolage & Outils", Icons.Default.Build),
    ELECTROMENAGER("Électroménager", Icons.Default.Kitchen),
    LIVRES_LOISIRS("Livres & Culture", Icons.Default.MenuBook),
    PLANTES_JARDIN("Plantes & Agriculture", Icons.Default.LocalFlorist),
    AUTRE("Autres objets utiles", Icons.Default.MoreHoriz)
}

enum class FundraiserStatus(val label: String) {
    ACTIVE("En cours"),
    FUNDED("Objectif atteint"),
    CLOSED("Clôturé")
}

enum class DisputeReason(val label: String) {
    ITEM_MISMATCH("Objet non conforme à la description"),
    NO_SHOW("Absence au lieu de rendez-vous"),
    COMMERCIAL_RESELL("Tentative de vente commerciale"),
    SUSPICIOUS_BEHAVIOR("Comportement suspect ou impoli"),
    OTHER("Autre motif")
}

enum class DisputeStatus(val label: String) {
    OPEN("Ouvert"),
    UNDER_REVIEW("En cours d'arbitrage"),
    RESOLVED("Résolu"),
    DISMISSED("Rejeté")
}

enum class CollectionPointType(val label: String) {
    ORPHELINAT("Orphelinat"),
    PAROISSE("Paroisse / Centre religieux"),
    CHEFFERIE("Chefferie traditionnelle"),
    CENTRE_SOCIAL("Centre social de quartier"),
    RESSOURCERIE("Ressourcerie solidaire")
}
