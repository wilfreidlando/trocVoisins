package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GrainRules
import com.example.data.model.Item
import com.example.data.model.ItemRequest
import com.example.data.model.ItemStatus
import com.example.data.model.RequestStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.ui.AppViewModel
import com.example.ui.Route
import com.example.ui.components.AppCard
import com.example.ui.components.AppTextField
import com.example.ui.components.Avatar
import com.example.ui.components.BannerKind
import com.example.ui.components.CertifiedBadge
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.EmergencyBadge
import com.example.ui.components.EmptyState
import com.example.ui.components.ExchangeBadge
import com.example.ui.components.InfoBanner
import com.example.ui.components.ItemImage
import com.example.ui.components.LevelBadge
import com.example.ui.components.PrimaryAction
import com.example.ui.components.RatingStars
import com.example.ui.components.ReportDialog
import com.example.ui.components.ReviewDialog
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.SecondaryAction
import com.example.ui.components.SectionTitle
import com.example.ui.components.SmallTextButton
import com.example.ui.components.SmallTonalButton
import com.example.ui.components.StatusChip
import com.example.ui.theme.ForestGreenPrimary
import com.example.util.LocationUtils

@Composable
fun ItemDetailScreen(vm: AppViewModel, itemId: String) {
    val profile by vm.profile.collectAsState()
    val allItems by vm.items.collectAsState()
    val myRequests by vm.myRequests.collectAsState()
    val incoming by vm.incomingRequests.collectAsState()
    val busy by vm.busy.collectAsState()
    val me = profile ?: return
    val item = allItems.firstOrNull { it.id == itemId }

    var showReport by remember { mutableStateOf(false) }
    var showRequest by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showReview by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Détail",
        onBack = { vm.back() },
        actions = {
            if (item != null && item.donorId != me.id && !me.isMediator) {
                IconButton(onClick = { showReport = true }, modifier = Modifier.testTag("report_item_button")) {
                    Icon(Icons.Default.Flag, contentDescription = "Signaler cette annonce", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    ) { padding ->
        if (item == null) {
            EmptyState(
                Icons.Default.SearchOff, "Annonce introuvable",
                "Elle a peut-être été supprimée ou n'est plus accessible.",
                Modifier.padding(padding), "Retour", { vm.back() }
            )
            return@ScreenScaffold
        }

        val isOwner = item.donorId == me.id
        val isReserver = item.reservedByUserId == me.id
        val myRequest = myRequests.firstOrNull { it.itemId == item.id }
        val itemRequests = incoming.filter { it.itemId == item.id }
        val donor by vm.publicProfileFlow(item.donorId).collectAsState(initial = null)
        val reviewsWritten by vm.myReviewsWrittenFlow().collectAsState(initial = emptyList())
        val alreadyReviewed = reviewsWritten.any { it.itemId == item.id }

        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ItemImage(item, Modifier.fillMaxWidth().height(230.dp), iconSize = 64.dp)

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExchangeBadge(item.exchangeType)
                    StatusChip(item.statusEnum)
                    if (item.isEmergencyResponse) EmergencyBadge()
                }
                Text(item.title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    val dist = vm.distanceTo(item)
                    Text(
                        "${item.neighborhood.ifBlank { item.city }}, ${item.city} • ${LocationUtils.formatDistance(dist)}",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("État : ${item.conditionLabel}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (item.description.isNotBlank()) Text(item.description, fontSize = 14.sp, lineHeight = 21.sp)

                if (item.isTroc && item.wantedInReturn.isNotBlank()) {
                    InfoBanner("Le propriétaire cherche : ${item.wantedInReturn}", title = "En échange", kind = BannerKind.WARNING)
                }
                if (item.targetOrphanageName.isNotBlank()) {
                    InfoBanner("Destiné en priorité à : ${item.targetOrphanageName}", kind = BannerKind.INFO)
                }

                // Statut de validation
                if (item.statusEnum == ItemStatus.PENDING_MODERATION && isOwner) {
                    InfoBanner("Un médiateur vérifie votre annonce avant de la rendre visible. Vous serez notifié.", title = "En validation", kind = BannerKind.INFO)
                }
                if ((item.statusEnum == ItemStatus.REJECTED || item.statusEnum == ItemStatus.SUSPENDED) && isOwner) {
                    InfoBanner(
                        item.moderationNote.ifBlank { "Cette annonce n'est pas visible." },
                        title = if (item.statusEnum == ItemStatus.REJECTED) "Annonce refusée" else "Annonce suspendue",
                        kind = BannerKind.DANGER
                    )
                }

                // Proposé par
                SectionTitle("Proposé par")
                AppCard(onClick = { vm.push(Route.PublicProfile(item.donorId)) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(donor?.initials ?: item.donorName.take(1), donor?.photoUrl.orEmpty(), size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.donorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RatingStars(donor?.rating ?: 0.0, donor?.reviewCount ?: 0)
                                donor?.let { LevelBadge(it.level) }
                            }
                            if (item.donorCertified) { Spacer(Modifier.height(4.dp)); CertifiedBadge() }
                        }
                    }
                }

                // Actions selon la situation
                when {
                    isOwner -> OwnerPanel(vm, item, itemRequests, busy, onDelete = { showDelete = true })
                    isReserver || (myRequest?.statusEnum == RequestStatus.ACCEPTED && item.reservedByUserId == me.id) ->
                        HandoverPanel(vm, me, item, alreadyReviewed, onCancel = { showCancel = true }, onReview = { showReview = true })
                    item.statusEnum == ItemStatus.AVAILABLE && !me.isMediator ->
                        RequesterPanel(vm, me, item, myRequest, onRequest = { showRequest = true })
                    item.statusEnum == ItemStatus.RESERVED ->
                        InfoBanner("Cet objet est réservé pour quelqu'un d'autre. Il pourra redevenir disponible.", kind = BannerKind.WARNING)
                    item.statusEnum == ItemStatus.COMPLETED ->
                        InfoBanner("Cet objet a trouvé preneur.", kind = BannerKind.SUCCESS)
                }

                SectionTitle("Sécurité")
                InfoBanner(
                    "• Rencontrez-vous dans un lieu public et éclairé : carrefour, pharmacie, point relais.\n" +
                            "• N'allez jamais seul chez un inconnu.\n" +
                            "• Aucun paiement n'est demandé pour un don. Signalez toute demande d'argent.",
                    title = "Remise en main propre", kind = BannerKind.INFO
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        if (showRequest) {
            RequestDialog(
                item = item, me = me,
                onDismiss = { showRequest = false },
                onSubmit = { message, topUp ->
                    vm.requestItem(item, message, topUp) { threadId -> vm.push(Route.Chat(threadId)) }
                }
            )
        }
        if (showReport) {
            ReportDialog(item.title, onDismiss = { showReport = false }) { reason, details -> vm.reportItem(item, reason, details) }
        }
        if (showDelete) {
            ConfirmDialog(
                "Supprimer l'annonce ?", "Cette action est définitive.", "Supprimer",
                onConfirm = { vm.deleteItem(item) { vm.back() } }, onDismiss = { showDelete = false }, destructive = true
            )
        }
        if (showCancel) {
            ConfirmDialog(
                "Annuler l'échange ?", "L'objet redeviendra disponible et l'autre personne sera prévenue.", "Annuler l'échange",
                onConfirm = { vm.cancelReservation(item) }, onDismiss = { showCancel = false }, destructive = true
            )
        }
        if (showReview) {
            val targetId = if (me.id == item.donorId) item.reservedByUserId else item.donorId
            val targetName = if (me.id == item.donorId) item.reservedByName else item.donorName
            ReviewDialog(targetName, onDismiss = { showReview = false }) { rating, badge, comment ->
                vm.submitReview(item, targetId, rating, badge, comment)
            }
        }
    }
}

@Composable
private fun RequesterPanel(vm: AppViewModel, me: UserProfile, item: Item, myRequest: ItemRequest?, onRequest: () -> Unit) {
    val status = myRequest?.statusEnum
    val needsCost = !item.isTroc && GrainRules.DON_RECEIVE_COST > 0 && item.donorRole != UserRole.ASSOCIATION.name
    when (status) {
        RequestStatus.PENDING -> {
            InfoBanner("Le propriétaire a reçu votre demande. Vous serez notifié de sa réponse.", title = "Demande envoyée", kind = BannerKind.INFO)
            SecondaryAction("Ouvrir la discussion", { vm.push(Route.Chat("${item.id}_${me.id}")) })
            SmallTextButton("Annuler ma demande", { vm.cancelRequest(myRequest) })
        }
        else -> {
            if (status == RequestStatus.DECLINED) {
                InfoBanner("Votre précédente demande n'a pas été retenue. Vous pouvez en refaire une.", kind = BannerKind.WARNING)
            }
            if (needsCost) {
                InfoBanner(
                    "Recevoir ce don coûte ${GrainRules.DON_RECEIVE_COST} Graines, déduites quand la remise est confirmée (vous avez ${me.grains} 🌾). " +
                            "Les dons des associations sont gratuits.",
                    kind = BannerKind.INFO
                )
            }
            PrimaryAction(
                if (item.isTroc) "Proposer un troc" else "Demander cet objet",
                onRequest,
                enabled = !me.isSuspended,
                tag = "request_item_button"
            )
        }
    }
}

@Composable
private fun OwnerPanel(vm: AppViewModel, item: Item, requests: List<ItemRequest>, busy: Boolean, onDelete: () -> Unit) {
    if (item.statusEnum == ItemStatus.AVAILABLE || item.statusEnum == ItemStatus.RESERVED) {
        val pending = requests.filter { it.statusEnum == RequestStatus.PENDING }
        if (item.statusEnum == ItemStatus.AVAILABLE) {
            SectionTitle("Demandes reçues (${pending.size})")
            if (pending.isEmpty()) {
                Text("Personne n'a encore demandé cet objet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            pending.forEach { r ->
                AppCard {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(r.requesterName, fontWeight = FontWeight.Bold)
                        if (r.message.isNotBlank()) Text(r.message, fontSize = 13.sp)
                        if (r.graineTopUp > 0) Text("+ ${r.graineTopUp} 🌾 Graines proposées en complément", fontSize = 12.5.sp, color = ForestGreenPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            SmallTonalButton("Accepter", { vm.acceptRequest(item, r) }, enabled = !busy)
                            SmallTextButton("Discuter", { vm.push(Route.Chat("${item.id}_${r.requesterId}")) })
                            SmallTextButton("Refuser", { vm.declineRequest(r) })
                        }
                    }
                }
            }
        }
        if (item.statusEnum == ItemStatus.RESERVED) {
            SectionTitle("Remise en cours")
            Text("Réservé pour ${item.reservedByName}.", fontSize = 13.sp)
        }
    }
    if (item.statusEnum == ItemStatus.RESERVED || item.statusEnum == ItemStatus.COMPLETED) {
        val profile by vm.profile.collectAsState()
        val me = profile
        if (me != null) {
            val reviewsWritten by vm.myReviewsWrittenFlow().collectAsState(initial = emptyList())
            var showReview by remember { mutableStateOf(false) }
            var showCancel by remember { mutableStateOf(false) }
            HandoverPanel(vm, me, item, reviewsWritten.any { it.itemId == item.id }, { showCancel = true }, { showReview = true })
            if (showCancel) ConfirmDialog(
                "Annuler l'échange ?", "L'objet redeviendra disponible et l'autre personne sera prévenue.", "Annuler l'échange",
                onConfirm = { vm.cancelReservation(item) }, onDismiss = { showCancel = false }, destructive = true
            )
            if (showReview) ReviewDialog(item.reservedByName, onDismiss = { showReview = false }) { r, b, c ->
                vm.submitReview(item, item.reservedByUserId, r, b, c)
            }
        }
    }
    if (item.statusEnum != ItemStatus.RESERVED && item.statusEnum != ItemStatus.COMPLETED) {
        SecondaryAction("Supprimer mon annonce", onDelete)
    }
}

/** Double confirmation de remise + annulation + avis. */
@Composable
private fun HandoverPanel(
    vm: AppViewModel, me: UserProfile, item: Item, alreadyReviewed: Boolean,
    onCancel: () -> Unit, onReview: () -> Unit
) {
    val iAmDonor = me.id == item.donorId
    val myConfirmed = if (iAmDonor) item.donorConfirmed else item.receiverConfirmed
    val otherConfirmed = if (iAmDonor) item.receiverConfirmed else item.donorConfirmed
    val otherName = if (iAmDonor) item.reservedByName else item.donorName
    val threadId = "${item.id}_${if (iAmDonor) item.reservedByUserId else me.id}"

    when (item.statusEnum) {
        ItemStatus.RESERVED -> {
            SectionTitle("Remise de l'objet")
            AppCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Convenez d'un lieu public avec $otherName, puis confirmez chacun la remise.", fontSize = 13.5.sp, lineHeight = 19.sp)
                    ConfirmRow("Vous", myConfirmed)
                    ConfirmRow(otherName, otherConfirmed)
                    if (!myConfirmed) {
                        PrimaryAction("J'ai remis / reçu l'objet", { vm.confirmHandover(item) }, tag = "confirm_handover")
                    } else {
                        Text("En attente de la confirmation de $otherName.", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SecondaryAction("Discuter", { vm.push(Route.Chat(threadId)) })
                    SmallTextButton("Annuler l'échange", onCancel)
                }
            }
        }
        ItemStatus.COMPLETED -> {
            SectionTitle("Échange terminé")
            InfoBanner(
                "Merci pour ce geste solidaire ! Vos Graines sont créditées automatiquement.",
                kind = BannerKind.SUCCESS
            )
            if (!alreadyReviewed) {
                PrimaryAction("Laisser un avis (+${GrainRules.REVIEW} 🌾)", onReview)
            } else {
                Text("Vous avez déjà laissé votre avis.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        else -> Unit
    }
}

@Composable
private fun ConfirmRow(name: String, confirmed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.CheckCircle, contentDescription = null,
            tint = if (confirmed) ForestGreenPrimary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("$name : ${if (confirmed) "remise confirmée" else "pas encore confirmé"}", fontSize = 13.sp)
    }
}

@Composable
private fun RequestDialog(item: Item, me: UserProfile, onDismiss: () -> Unit, onSubmit: (String, Int) -> Unit) {
    var message by remember { mutableStateOf("") }
    var topUp by remember { mutableFloatStateOf(0f) }
    val maxTop = minOf(GrainRules.MAX_TOPUP, me.grains).toFloat()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item.isTroc) "Proposer un troc" else "Demander cet objet", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (item.isTroc) {
                    AppTextField(
                        message, { message = it }, "Objet que vous proposez", singleLine = false, minLines = 2, maxLength = 300,
                        placeholder = item.wantedInReturn.ifBlank { "Décrivez votre objet" }
                    )
                    if (maxTop >= 1f) {
                        Text("Complément en Graines : ${topUp.toInt()} 🌾", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Slider(value = topUp, onValueChange = { topUp = it }, valueRange = 0f..maxTop, steps = (maxTop.toInt() - 1).coerceAtLeast(0))
                        Text(
                            "Utile si votre objet vaut un peu moins. Les Graines ne sont transférées qu'à la remise confirmée.",
                            fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    AppTextField(
                        message, { message = it }, "Message (facultatif)", singleLine = false, minLines = 2, maxLength = 300,
                        placeholder = "Ex. Je suis disponible demain après-midi."
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(message, topUp.toInt()); onDismiss() },
                enabled = !item.isTroc || message.trim().length >= 3
            ) { Text("Envoyer", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
