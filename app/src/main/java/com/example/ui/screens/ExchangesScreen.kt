package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemStatus
import com.example.data.model.RequestStatus
import com.example.ui.AppViewModel
import com.example.ui.Route
import com.example.ui.components.AppCard
import com.example.ui.components.EmptyState
import com.example.ui.components.ItemCard
import com.example.ui.components.ItemImage
import com.example.ui.components.Pill
import com.example.ui.components.SectionTitle
import com.example.ui.components.TabScaffold
import com.example.ui.theme.ForestGreenPrimary

/** « Mes échanges » : ce qui demande mon action, mes annonces, l'historique. */
@Composable
fun ExchangesScreen(vm: AppViewModel) {
    val profile by vm.profile.collectAsState()
    val items by vm.items.collectAsState()
    val myRequests by vm.myRequests.collectAsState()
    val incoming by vm.incomingRequests.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val me = profile ?: return

    val myItems = items.filter { it.donorId == me.id }
    val pendingIncoming = incoming.filter { it.statusEnum == RequestStatus.PENDING }
    val inProgress = items.filter {
        it.statusEnum == ItemStatus.RESERVED && (it.donorId == me.id || it.reservedByUserId == me.id)
    }
    val myPending = myRequests.filter { it.statusEnum == RequestStatus.PENDING }
    val done = items.filter {
        it.statusEnum == ItemStatus.COMPLETED && (it.donorId == me.id || it.reservedByUserId == me.id)
    }
    val todoCount = pendingIncoming.size + inProgress.count {
        if (it.donorId == me.id) !it.donorConfirmed else !it.receiverConfirmed
    }

    TabScaffold(title = "Mes échanges") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(if (todoCount > 0) "En cours ($todoCount)" else "En cours") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Mes annonces") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Terminés") })
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (tab) {
                    0 -> {
                        if (pendingIncoming.isEmpty() && inProgress.isEmpty() && myPending.isEmpty()) {
                            item {
                                EmptyState(
                                    Icons.Default.Inbox, "Rien en cours",
                                    "Vos demandes reçues, vos remises à confirmer et vos demandes envoyées apparaissent ici.",
                                    actionLabel = "Voir les annonces", onAction = { vm.openTab(Route.Home) }
                                )
                            }
                        }
                        if (pendingIncoming.isNotEmpty()) {
                            item { SectionTitle("Demandes à traiter") }
                            items(pendingIncoming, key = { "in_" + it.id }) { r ->
                                RequestRow(
                                    title = r.itemTitle,
                                    subtitle = "${r.requesterName} • ${if (r.graineTopUp > 0) "+${r.graineTopUp} 🌾" else "demande reçue"}",
                                    badge = "Répondre",
                                    onClick = { vm.push(Route.ItemDetail(r.itemId)) }
                                )
                            }
                        }
                        if (inProgress.isNotEmpty()) {
                            item { SectionTitle("Remises à confirmer") }
                            items(inProgress, key = { "rs_" + it.id }) { it2 ->
                                val iAmDonor = it2.donorId == me.id
                                val confirmed = if (iAmDonor) it2.donorConfirmed else it2.receiverConfirmed
                                RequestRow(
                                    title = it2.title,
                                    subtitle = if (iAmDonor) "Pour ${it2.reservedByName}" else "De ${it2.donorName}",
                                    badge = if (confirmed) "En attente de l'autre" else "À confirmer",
                                    onClick = { vm.push(Route.ItemDetail(it2.id)) }
                                )
                            }
                        }
                        if (myPending.isNotEmpty()) {
                            item { SectionTitle("Mes demandes en attente") }
                            items(myPending, key = { "mp_" + it.id }) { r ->
                                RequestRow(
                                    title = r.itemTitle, subtitle = "En attente de réponse du propriétaire",
                                    badge = "Voir", onClick = { vm.push(Route.ItemDetail(r.itemId)) }
                                )
                            }
                        }
                    }
                    1 -> {
                        if (myItems.isEmpty()) {
                            item {
                                EmptyState(
                                    Icons.Default.CardGiftcard, "Aucune annonce",
                                    "Publiez un objet dont vous n'avez plus l'usage : quelqu'un en a besoin près de chez vous.",
                                    actionLabel = if (!me.isAssociation || me.isCertifiedAssociation) "Publier un objet" else null,
                                    onAction = if (!me.isAssociation || me.isCertifiedAssociation) ({ vm.push(Route.Publish) }) else null
                                )
                            }
                        } else {
                            items(myItems, key = { it.id }) { item ->
                                ItemCard(item, onClick = { vm.push(Route.ItemDetail(item.id)) }, showStatus = true)
                            }
                        }
                    }
                    else -> {
                        if (done.isEmpty()) {
                            item {
                                EmptyState(Icons.Default.Inbox, "Pas encore d'échange terminé",
                                    "Vos dons et trocs réussis s'afficheront ici, avec la possibilité de laisser un avis.")
                            }
                        } else {
                            items(done, key = { "d_" + it.id }) { item ->
                                ItemCard(item, onClick = { vm.push(Route.ItemDetail(item.id)) }, showStatus = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestRow(title: String, subtitle: String, badge: String, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1)
                Text(subtitle, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            Pill(badge, ForestGreenPrimary.copy(alpha = 0.12f), ForestGreenPrimary, compact = true)
        }
    }
}
