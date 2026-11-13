package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssociationType
import com.example.data.model.Dispute
import com.example.data.model.DisputeStatus
import com.example.data.model.Fundraiser
import com.example.data.model.FundraiserStatus
import com.example.data.model.Item
import com.example.data.model.ItemStatus
import com.example.data.model.UserProfile
import com.example.ui.AppViewModel
import com.example.ui.Route
import com.example.ui.components.AppCard
import com.example.ui.components.EmptyState
import com.example.ui.components.ItemCard
import com.example.ui.components.NoteDialog
import com.example.ui.components.Pill
import com.example.ui.components.ResolveDisputeDialog
import com.example.ui.components.SectionTitle
import com.example.ui.components.SmallTextButton
import com.example.ui.components.SmallTonalButton
import com.example.ui.components.TabScaffold
import com.example.ui.theme.ForestGreenPrimary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun shortDate(ms: Long): String = if (ms <= 0) "" else SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(ms))

// =============================================================================================
//  Tableau de bord du médiateur
// =============================================================================================

@Composable
fun AdminDashboardScreen(vm: AppViewModel) {
    val items by vm.items.collectAsState()
    val funds by vm.fundraisers.collectAsState()
    val disputes by vm.disputes.collectAsState()
    val assos by vm.pendingAssociations.collectAsState()

    val pendingItems = items.count { it.statusEnum == ItemStatus.PENDING_MODERATION }
    val pendingFunds = funds.count { it.statusEnum == FundraiserStatus.PENDING_REVIEW }
    val openDisputes = disputes.count { it.statusEnum == DisputeStatus.OPEN || it.statusEnum == DisputeStatus.UNDER_REVIEW }
    val live = items.count { it.statusEnum == ItemStatus.AVAILABLE }
    val done = items.count { it.statusEnum == ItemStatus.COMPLETED }

    TabScaffold(title = "Médiation") { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("À traiter", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Counter("Annonces", pendingItems, Modifier.weight(1f)) { vm.openTab(Route.AdminValidate) }
                Counter("Associations", assos.size, Modifier.weight(1f)) { vm.openTab(Route.AdminValidate) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Counter("Cagnottes", pendingFunds, Modifier.weight(1f)) { vm.openTab(Route.AdminValidate) }
                Counter("Litiges ouverts", openDisputes, Modifier.weight(1f)) { vm.openTab(Route.AdminDisputes) }
            }
            Text("Activité de la plateforme", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Counter("Annonces en ligne", live, Modifier.weight(1f), null)
                Counter("Échanges terminés", done, Modifier.weight(1f), null)
            }
            AppCard(onClick = { vm.push(Route.Browse) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Parcourir les annonces comme un membre", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("Ouvrir", color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Counter(label: String, value: Int, modifier: Modifier, onClick: (() -> Unit)?) {
    AppCard(modifier, onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Text(value.toString(), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = if (value > 0 && onClick != null) MaterialTheme.colorScheme.error else ForestGreenPrimary)
            Text(label, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// =============================================================================================
//  À valider : annonces, associations, cagnottes
// =============================================================================================

@Composable
fun AdminValidateScreen(vm: AppViewModel) {
    val items by vm.items.collectAsState()
    val funds by vm.fundraisers.collectAsState()
    val assos by vm.pendingAssociations.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    val pendingItems = items.filter { it.statusEnum == ItemStatus.PENDING_MODERATION }
    val pendingFunds = funds.filter { it.statusEnum == FundraiserStatus.PENDING_REVIEW }
    var refuseItem by remember { mutableStateOf<Item?>(null) }
    var refuseAsso by remember { mutableStateOf<UserProfile?>(null) }

    TabScaffold(title = "À valider") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Annonces (${pendingItems.size})", fontSize = 13.sp) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Assos (${assos.size})", fontSize = 13.sp) })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Cagnottes (${pendingFunds.size})", fontSize = 13.sp) })
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (tab) {
                    0 -> {
                        if (pendingItems.isEmpty()) item { EmptyState(Icons.Default.TaskAlt, "Tout est à jour", "Aucune annonce n'attend de validation.") }
                        items(pendingItems, key = { it.id }) { item ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ItemCard(item, onClick = { vm.push(Route.ItemDetail(item.id)) })
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    SmallTonalButton("Approuver", { vm.moderateItem(item, true, "") })
                                    SmallTextButton("Refuser", { refuseItem = item })
                                    Text("par ${item.donorName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    1 -> {
                        if (assos.isEmpty()) item { EmptyState(Icons.Default.TaskAlt, "Aucune association en attente", "Les nouvelles demandes de certification apparaîtront ici.") }
                        items(assos, key = { it.id }) { a ->
                            AppCard {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(a.associationName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(AssociationType.entries.firstOrNull { it.name == a.associationType }?.label ?: a.associationType, fontSize = 13.sp)
                                    Text("N° d'agrément : ${a.associationRegNumber}", fontSize = 13.sp)
                                    Text("Responsable : ${a.name} • ${a.associationPhone.ifBlank { "pas de téléphone" }}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${a.neighborhood}, ${a.city} • inscrite le ${shortDate(a.createdAtMs)}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SmallTonalButton("Certifier", { vm.certifyAssociation(a, true, "") })
                                        SmallTextButton("Refuser", { refuseAsso = a })
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        if (pendingFunds.isEmpty()) item { EmptyState(Icons.Default.TaskAlt, "Aucune cagnotte en attente", "Les nouvelles cagnottes à valider apparaîtront ici.") }
                        items(pendingFunds, key = { it.id }) { f -> PendingFundraiser(vm, f) }
                    }
                }
            }
        }
    }

    refuseItem?.let { item ->
        NoteDialog("Refuser l'annonce", "Motif (envoyé à l'auteur)", "Refuser", { vm.moderateItem(item, false, it) }, { refuseItem = null }, required = true)
    }
    refuseAsso?.let { a ->
        NoteDialog("Refuser la certification", "Motif (envoyé à l'association)", "Refuser", { vm.certifyAssociation(a, false, it) }, { refuseAsso = null }, required = true)
    }
}

@Composable
private fun PendingFundraiser(vm: AppViewModel, f: Fundraiser) {
    val fmt = NumberFormat.getInstance(Locale.FRANCE)
    AppCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(f.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${f.associationName} • ${f.neighborhood.ifBlank { f.city }}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(f.description, fontSize = 13.sp, maxLines = 6)
            Text(
                "Objectif : " + listOfNotNull(
                    if (f.targetFcfa > 0) fmt.format(f.targetFcfa) + " FCFA" else null,
                    if (f.targetGrains > 0) "${f.targetGrains} 🌾" else null
                ).joinToString(" + "), fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
            if (f.proofNote.isNotBlank()) Text("Justificatifs : ${f.proofNote}", fontSize = 12.5.sp)
            if (f.payInstructions.isNotBlank()) Text("Remise : ${f.payInstructions}", fontSize = 12.5.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTonalButton("Publier", { vm.reviewFundraiser(f, true) })
                SmallTextButton("Refuser", { vm.reviewFundraiser(f, false) })
            }
        }
    }
}

// =============================================================================================
//  Litiges
// =============================================================================================

@Composable
fun AdminDisputesScreen(vm: AppViewModel) {
    val disputes by vm.disputes.collectAsState()
    val profile by vm.profile.collectAsState()
    var onlyOpen by remember { mutableStateOf(true) }
    var resolving by remember { mutableStateOf<Dispute?>(null) }
    val me = profile ?: return

    val shown = disputes.filter { !onlyOpen || it.statusEnum == DisputeStatus.OPEN || it.statusEnum == DisputeStatus.UNDER_REVIEW }

    TabScaffold(title = "Litiges") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = onlyOpen, onClick = { onlyOpen = true }, label = { Text("À traiter") })
                FilterChip(selected = !onlyOpen, onClick = { onlyOpen = false }, label = { Text("Tous") })
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (shown.isEmpty()) item { EmptyState(Icons.Default.Gavel, "Aucun litige", "Les signalements des membres apparaîtront ici.") }
                items(shown, key = { it.id }) { d ->
                    AppCard {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DisputePill(d.statusEnum)
                                Text(shortDate(d.createdAtMs), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(d.itemTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(d.reasonLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                            Text("« ${d.description} »", fontSize = 13.sp)
                            Text("Signalé par ${d.reporterName} • contre ${d.reportedUserName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (d.resolutionNote.isNotBlank()) Text("Décision : ${d.resolutionNote}", fontSize = 12.5.sp, color = ForestGreenPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                SmallTextButton("Voir l'annonce", { vm.push(Route.ItemDetail(d.itemId)) })
                                SmallTextButton("Profil signalé", { vm.push(Route.PublicProfile(d.reportedUserId)) })
                            }
                            if (d.statusEnum == DisputeStatus.OPEN) {
                                SmallTonalButton("Prendre en charge", { vm.takeDispute(d) })
                            }
                            if (d.statusEnum == DisputeStatus.OPEN || (d.statusEnum == DisputeStatus.UNDER_REVIEW && d.mediatorId == me.id)) {
                                SmallTonalButton("Décider", { resolving = d })
                            }
                        }
                    }
                }
            }
        }
    }

    resolving?.let { d ->
        ResolveDisputeDialog(onDismiss = { resolving = null }) { dismissed, note, action -> vm.resolveDispute(d, dismissed, note, action) }
    }
}

@Composable
private fun DisputePill(status: DisputeStatus) {
    when (status) {
        DisputeStatus.OPEN -> Pill(status.label, Color(0xFFFDECEA), Color(0xFF8C1D18), compact = true)
        DisputeStatus.UNDER_REVIEW -> Pill(status.label, Color(0xFFFFF4E0), Color(0xFF7A4B00), compact = true)
        DisputeStatus.RESOLVED -> Pill(status.label, Color(0xFFE6F4EA), Color(0xFF1B5E20), compact = true)
        DisputeStatus.DISMISSED -> Pill(status.label, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, compact = true)
    }
}
