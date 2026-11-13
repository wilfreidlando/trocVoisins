package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssociationStatus
import com.example.data.model.AssociationType
import com.example.data.model.Cities
import com.example.data.model.GrainLevel
import com.example.data.model.GrainRules
import com.example.data.model.NotificationType
import com.example.ui.AppViewModel
import com.example.ui.Route
import com.example.ui.ThemeMode
import com.example.ui.components.AppCard
import com.example.ui.components.AppTextField
import com.example.ui.components.Avatar
import com.example.ui.components.BannerKind
import com.example.ui.components.CertifiedBadge
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.DropdownField
import com.example.ui.components.EmptyState
import com.example.ui.components.GrainsPill
import com.example.ui.components.InfoBanner
import com.example.ui.components.LevelBadge
import com.example.ui.components.PrimaryAction
import com.example.ui.components.RatingStars
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.SecondaryAction
import com.example.ui.components.SectionTitle
import com.example.ui.components.TabScaffold
import com.example.ui.components.UseMyLocationButton
import com.example.ui.theme.ForestGreenPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun day(ms: Long): String = if (ms <= 0) "" else SimpleDateFormat("dd MMM yyyy", Locale.FRANCE).format(Date(ms))

// =============================================================================================
//  Profil (onglet)
// =============================================================================================

@Composable
fun ProfileScreen(vm: AppViewModel) {
    val profile by vm.profile.collectAsState()
    val theme by vm.themeMode.collectAsState()
    var confirmSignOut by remember { mutableStateOf(false) }
    val me = profile ?: return

    TabScaffold(title = "Mon profil") { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppCard {
                Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(me.initials, me.photoUrl, size = 72.dp)
                    Spacer(Modifier.height(10.dp))
                    Text(me.displayName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Text(me.roleEnum.label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!me.isMediator) RatingStars(me.rating, me.reviewCount)
                        if (!me.isMediator) LevelBadge(me.level)
                        if (me.isCertifiedAssociation) CertifiedBadge()
                    }
                    Text("${me.neighborhood.ifBlank { me.city }}, ${me.city}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (me.isAssociation) AssociationStatusCard(vm, me)

            if (me.isSuspended) {
                InfoBanner("Votre compte est suspendu. Vous pouvez consulter l'application mais pas publier ni faire de demande.", title = "Compte suspendu", kind = BannerKind.DANGER)
            } else if (me.warnings > 0) {
                InfoBanner("Vous avez reçu ${me.warnings} avertissement(s) de la médiation. Respectez les règles de la communauté.", kind = BannerKind.WARNING)
            }

            if (!me.isMediator) {
                AppCard(onClick = { vm.push(Route.Wallet) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ForestGreenPrimary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Mes Graines", fontWeight = FontWeight.Bold)
                            Text("Solde, niveau et historique", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        GrainsPill(me.grains)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox("Dons remis", me.giftsGiven.toString(), Modifier.weight(1f))
                    StatBox("Trocs réussis", me.trocsCompleted.toString(), Modifier.weight(1f))
                    StatBox("Avis reçus", me.reviewCount.toString(), Modifier.weight(1f))
                }
            }

            SectionTitle("Réglages")
            MenuRow("Notifications", Icons.Default.Notifications) { vm.push(Route.Notifications) }
            if (!me.isMediator) MenuRow("Ma localisation et mon rayon", Icons.Default.Place) { vm.push(Route.EditLocation) }
            MenuRow("Aide et règles de la communauté", Icons.AutoMirrored.Filled.HelpOutline) { vm.push(Route.Help) }

            AppCard {
                Column(Modifier.padding(14.dp)) {
                    Text("Apparence", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { m ->
                            FilterChip(
                                selected = theme == m, onClick = { vm.setThemeMode(m) },
                                label = { Text(when (m) { ThemeMode.LIGHT -> "Clair"; ThemeMode.DARK -> "Sombre"; ThemeMode.SYSTEM -> "Système" }) }
                            )
                        }
                    }
                }
            }

            SecondaryAction("Se déconnecter", { confirmSignOut = true })
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmSignOut) {
        ConfirmDialog("Se déconnecter ?", "Vous devrez saisir à nouveau votre e-mail et votre mot de passe.", "Se déconnecter", { vm.signOut() }, { confirmSignOut = false })
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier) {
    AppCard(modifier) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = ForestGreenPrimary)
            Text(label, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MenuRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = ForestGreenPrimary)
            Spacer(Modifier.width(12.dp))
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AssociationStatusCard(vm: AppViewModel, me: com.example.data.model.UserProfile) {
    var editing by remember { mutableStateOf(false) }
    when (me.associationStatus) {
        AssociationStatus.CERTIFIED.name ->
            InfoBanner("Votre association est certifiée : vous pouvez publier des dons, créer des cagnottes et des points relais.", title = "Association certifiée", kind = BannerKind.SUCCESS)
        AssociationStatus.REJECTED.name -> {
            InfoBanner(
                "Motif : ${me.associationNote.ifBlank { "dossier incomplet" }}. Corrigez vos informations puis renvoyez la demande.",
                title = "Certification refusée", kind = BannerKind.DANGER
            )
            if (!editing) SecondaryAction("Corriger mon dossier", { editing = true })
        }
        else -> InfoBanner("Un médiateur examine votre dossier. Vous serez notifié dès qu'il sera traité.", title = "Certification en attente", kind = BannerKind.WARNING)
    }
    if (editing) {
        var name by remember { mutableStateOf(me.associationName) }
        var type by remember { mutableStateOf(AssociationType.entries.firstOrNull { it.name == me.associationType } ?: AssociationType.AUTRE) }
        var reg by remember { mutableStateOf(me.associationRegNumber) }
        var phone by remember { mutableStateOf(me.associationPhone) }
        AppCard {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTextField(name, { name = it }, "Nom officiel", maxLength = 120)
                DropdownField("Type", AssociationType.entries, type, { it.label }, { type = it })
                AppTextField(reg, { reg = it }, "Numéro d'agrément", maxLength = 60)
                AppTextField(phone, { phone = it }, "Téléphone", maxLength = 30)
                PrimaryAction("Renvoyer la demande", {
                    if (name.trim().length >= 2 && reg.trim().length >= 3) { vm.resubmitAssociation(name, type.name, reg, phone); editing = false }
                })
            }
        }
    }
}

// =============================================================================================
//  Portefeuille de Graines
// =============================================================================================

@Composable
fun WalletScreen(vm: AppViewModel) {
    val profile by vm.profile.collectAsState()
    val ledger by vm.ledger.collectAsState()
    val me = profile ?: return
    val level = me.level
    val next = GrainLevel.next(level)

    ScreenScaffold(title = "Mes Graines", onBack = { vm.back() }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                AppCard {
                    Column(Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Solde", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${me.grains} 🌾", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = ForestGreenPrimary)
                        Spacer(Modifier.height(8.dp))
                        LevelBadge(level)
                        if (next != null) {
                            Spacer(Modifier.height(10.dp))
                            val span = (next.minEarned - level.minEarned).coerceAtLeast(1)
                            val progress = ((me.totalGrainsEarned - level.minEarned).toFloat() / span).coerceIn(0f, 1f)
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = ForestGreenPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text("${next.minEarned - me.totalGrainsEarned} Graines gagnées avant le niveau ${next.label} ${next.emoji}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Niveau maximum atteint, merci pour votre engagement !", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                InfoBanner(
                    title = "À quoi servent les Graines ?",
                    text = "• Recevoir un don d'un voisin coûte ${GrainRules.DON_RECEIVE_COST} Graines (gratuit pour les dons d'associations).\n" +
                            "• Compléter un troc inégal.\n" +
                            "• Soutenir la cagnotte d'une association.\n" +
                            "• Les gagner : +${GrainRules.DON_DONOR} par don remis, +${GrainRules.TROC_EACH} par troc réussi, +${GrainRules.REVIEW} par avis.\n" +
                            "• Votre niveau (Graines gagnées au total) donne la confiance : dès ${GrainRules.TRUSTED_THRESHOLD}, vos annonces sont publiées sans attendre la modération.\n" +
                            "Les Graines ne s'achètent pas et ne se convertissent pas en argent.",
                    kind = BannerKind.INFO
                )
            }
            item { SectionTitle("Historique") }
            if (ledger.isEmpty()) {
                item { Text("Aucun mouvement pour l'instant. Vos Graines apparaîtront après un échange confirmé.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(ledger, key = { it.id }) { e ->
                AppCard {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.reasonLabel, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(day(e.createdAtMs), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            (if (e.amount > 0) "+" else "") + "${e.amount} 🌾", fontWeight = FontWeight.ExtraBold,
                            color = if (e.amount > 0) ForestGreenPrimary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================================
//  Profil public d'un voisin
// =============================================================================================

@Composable
fun PublicProfileScreen(vm: AppViewModel, uid: String) {
    val user by vm.publicProfileFlow(uid).collectAsState(initial = null)
    val reviews by vm.reviewsFlow(uid).collectAsState(initial = emptyList())

    ScreenScaffold(title = "Profil", onBack = { vm.back() }) { padding ->
        val u = user
        if (u == null) {
            EmptyState(Icons.Default.Place, "Profil introuvable", "Ce membre n'est plus accessible.", Modifier.padding(padding))
            return@ScreenScaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                AppCard {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Avatar(u.initials, u.photoUrl, size = 72.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(u.displayName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RatingStars(u.rating, u.reviewCount)
                            LevelBadge(u.level)
                            if (u.isCertifiedAssociation) CertifiedBadge()
                        }
                        Text("${u.neighborhood.ifBlank { u.city }}, ${u.city}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("${u.giftsGiven} dons remis • ${u.trocsCompleted} trocs réussis", fontSize = 13.sp)
                    }
                }
            }
            item { SectionTitle("Avis (${reviews.size})") }
            if (reviews.isEmpty()) item { Text("Pas encore d'avis.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(reviews, key = { it.id }) { r ->
                AppCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(r.authorName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text(day(r.createdAtMs), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${"★".repeat(r.rating)}${"☆".repeat((5 - r.rating).coerceAtLeast(0))}  ${r.badge}", fontSize = 12.5.sp, color = androidx.compose.ui.graphics.Color(0xFFB77900))
                        if (r.comment.isNotBlank()) Text(r.comment, fontSize = 13.sp)
                        Text("Pour « ${r.itemTitle} »", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// =============================================================================================
//  Localisation et rayon
// =============================================================================================

@Composable
fun EditLocationScreen(vm: AppViewModel) {
    val profile by vm.profile.collectAsState()
    val busy by vm.busy.collectAsState()
    val me = profile ?: return
    var city by remember { mutableStateOf(me.city) }
    var neighborhood by remember { mutableStateOf(me.neighborhood) }
    var landmark by remember { mutableStateOf(me.landmark) }
    var lat by remember { mutableStateOf(me.lat) }
    var lng by remember { mutableStateOf(me.lng) }
    var radius by remember { mutableFloatStateOf(me.radiusKm.coerceIn(1f, 25f)) }
    var note by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Ma localisation", onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoBanner("Votre adresse exacte n'est jamais affichée : les autres voient seulement votre quartier et le repère que vous indiquez.", kind = BannerKind.INFO)
            DropdownField("Ville", Cities.names, city, { it }, { city = it; lat = 0.0; lng = 0.0 })
            AppTextField(neighborhood, { neighborhood = it }, "Quartier", maxLength = 100)
            AppTextField(landmark, { landmark = it }, "Repère de rencontre", maxLength = 150)
            UseMyLocationButton(
                onLocation = { a, b -> lat = a; lng = b; note = "Position mise à jour (approximative)." },
                onUnavailable = { note = "Position indisponible : le centre de la ville sera utilisé." }
            )
            note?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("Rayon de recherche : ${radius.toInt()} km", fontWeight = FontWeight.SemiBold)
            Slider(value = radius, onValueChange = { radius = it }, valueRange = 1f..25f, steps = 23)
            PrimaryAction("Enregistrer", enabled = !busy && neighborhood.isNotBlank(), onClick = {
                val c = Cities.find(city)
                vm.updateLocation(city, neighborhood, landmark, if (lat != 0.0 || lng != 0.0) lat else c.lat, if (lat != 0.0 || lng != 0.0) lng else c.lng, radius)
                vm.back()
            })
        }
    }
}

// =============================================================================================
//  Notifications
// =============================================================================================

@Composable
fun NotificationsScreen(vm: AppViewModel) {
    val list by vm.notifications.collectAsState()
    DisposableEffect(list.count { !it.read }) {
        if (list.any { !it.read }) vm.markNotificationsRead()
        onDispose { }
    }
    ScreenScaffold(title = "Notifications", onBack = { vm.back() }) { padding ->
        if (list.isEmpty()) {
            EmptyState(Icons.Default.Notifications, "Rien de neuf", "Vous serez prévenu ici des demandes, validations, décisions et dons reçus.", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { n ->
                    val target = when (n.type) {
                        NotificationType.FUNDRAISER.name -> if (n.refId.isNotBlank()) Route.FundraiserDetail(n.refId) else null
                        NotificationType.ASSOCIATION.name -> Route.Profile
                        else -> if (n.refId.isNotBlank()) Route.ItemDetail(n.refId) else null
                    }
                    AppCard(onClick = target?.let { t -> { vm.push(t) } }) {
                        Column(Modifier.padding(14.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(n.title, fontWeight = if (n.read) FontWeight.SemiBold else FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text(day(n.createdAtMs), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(n.body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================================
//  Aide
// =============================================================================================

@Composable
fun HelpScreen(vm: AppViewModel) {
    ScreenScaffold(title = "Aide", onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HelpBlock("Comment ça marche", "1. Publiez un objet à donner ou à troquer.\n2. Un médiateur le valide (ou il est publié directement si vous êtes de confiance).\n3. Un voisin le demande ; vous acceptez celui de votre choix.\n4. Vous vous retrouvez dans un lieu public.\n5. Chacun confirme la remise : les Graines sont créditées et vous pouvez vous noter.")
            HelpBlock("Dons et trocs", "Un don est gratuit. Un troc est un échange d'objets : vous pouvez ajouter quelques Graines pour équilibrer. Aucune somme d'argent n'est demandée entre voisins.")
            HelpBlock("Les Graines 🌾", "Points de solidarité : on les gagne en donnant et en échangeant, on les dépense pour recevoir un don, compléter un troc ou soutenir une cagnotte. Elles donnent aussi un niveau de confiance.")
            HelpBlock("Associations et cagnottes", "Les associations (orphelinats, paroisses, écoles…) sont certifiées par la médiation. Seules les associations certifiées créent des cagnottes. Un don en FCFA est compté quand l'association confirme l'avoir reçu.")
            HelpBlock("Sécurité", "Rencontrez-vous toujours dans un lieu public (carrefour, pharmacie, point relais). N'envoyez jamais d'argent à l'avance. Signalez toute annonce suspecte avec le bouton drapeau : la médiation examine chaque signalement.")
            HelpBlock("Interdits", "Revente commerciale, objets dangereux ou illégaux, annonces trompeuses, harcèlement. Les comptes qui ne respectent pas ces règles reçoivent un avertissement, puis peuvent être suspendus.")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HelpBlock(title: String, text: String) {
    AppCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(text, fontSize = 13.5.sp, lineHeight = 20.sp)
        }
    }
}
