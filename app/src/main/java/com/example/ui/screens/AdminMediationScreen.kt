package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import com.example.data.model.DisputeEntity
import com.example.data.model.DisputeStatus
import com.example.data.model.ItemEntity
import com.example.ui.TrocViewModel
import com.example.ui.components.AssociationBadge
import com.example.ui.components.ExchangeBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMediationScreen(
    viewModel: TrocViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val disputes by viewModel.disputes.collectAsState()
    val allItems by viewModel.allItemsForAdmin.collectAsState()
    val reportedItems by viewModel.reportedItems.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFFD32F2F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Portail Administration & Médiation", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.statusBarsPadding()
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header stats
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Médiation active • Douala & Yaoundé",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFD32F2F).copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "${disputes.filter { it.status == DisputeStatus.OPEN.name }.size} litiges ouverts",
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Tabs: Litiges, Modération des Dons, Associations
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ForestGreenPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Litiges (${disputes.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Modération (${allItems.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Associations", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            if (actionFeedback != null) {
                Surface(
                    shape = RoundedCornerShape(0.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = actionFeedback ?: "",
                        color = ForestGreenPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Litiges & Arbitrage
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (disputes.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Aucun litige en attente. Entente parfaite !", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(disputes, key = { it.id }) { disp ->
                                DisputeItemCard(
                                    dispute = disp,
                                    onResolve = { notes ->
                                        viewModel.resolveDispute(disp.id, DisputeStatus.RESOLVED.name, notes)
                                        actionFeedback = "Litige résolu avec succès."
                                    },
                                    onDismiss = {
                                        viewModel.resolveDispute(disp.id, DisputeStatus.DISMISSED.name, "Signalement non justifié.")
                                        actionFeedback = "Signalement classé sans suite."
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Modération des objets
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allItems, key = { it.id }) { item ->
                            ModerationItemCard(
                                item = item,
                                onApprove = {
                                    viewModel.approveItem(item.id)
                                    actionFeedback = "Annonce \"${item.title}\" validée et visible."
                                },
                                onReject = {
                                    viewModel.rejectItem(item.id)
                                    actionFeedback = "Annonce retirée de la plateforme."
                                }
                            )
                        }
                    }
                }
                2 -> {
                    // Associations & Orphelinats certifiés
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Organisations Caritatives & Agréments Cameroun",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        item {
                            AssociationCertCard(
                                name = "Orphelinat Sainte-Thérèse de Deido",
                                city = "Douala - Deido",
                                regNumber = "MINAS-LT-2024-089",
                                status = "Certifié & Agréé",
                                phone = "+237 675 22 44 88"
                            )
                        }
                        item {
                            AssociationCertCard(
                                name = "Association Enfants de l'Avenir",
                                city = "Yaoundé - Biyem-Assi",
                                regNumber = "MINAS-CE-2023-112",
                                status = "Certifié & Agréé",
                                phone = "+237 690 11 33 55"
                            )
                        }
                        item {
                            AssociationCertCard(
                                name = "Collectif Mères Solidaires du Wouri",
                                city = "Douala - Akwa",
                                regNumber = "MINAS-LT-2025-034",
                                status = "Certifié & Agréé",
                                phone = "+237 671 99 88 77"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisputeItemCard(
    dispute: DisputeEntity,
    onResolve: (notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf(dispute.mediatorNotes) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = "Litige : ${dispute.reason}",
                        color = Color(0xFFD32F2F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "Statut : ${dispute.status}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dispute.status == DisputeStatus.OPEN.name) Color(0xFFD32F2F) else ForestGreenPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Objet : \"${dispute.itemTitle}\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Signalé par ${dispute.reporterName} contre ${dispute.reportedUserName}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "\"${dispute.description}\"",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (dispute.status == DisputeStatus.OPEN.name) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note d'arbitrage du médiateur") },
                    placeholder = { Text("Ex: Rendez-vous reprogrammé ou annonce débloquée...") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onResolve(notes) },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Trancher & Résoudre", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Classer sans suite", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModerationItemCard(
    item: ItemEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExchangeBadge(exchangeType = item.exchangeType, compact = true)
                Text(
                    text = "${item.city} • ${item.neighborhood}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
            Text(text = item.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Donneur : ${item.donorName} (${item.donorRole})",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Valider", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Retirer", fontSize = 11.sp, color = Color(0xFFD32F2F))
                    }
                }
            }
        }
    }
}

@Composable
private fun AssociationCertCard(
    name: String,
    city: String,
    regNumber: String,
    status: String,
    phone: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                AssociationBadge(compact = true)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "$city • N° Agrément : $regNumber", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "Contact bénévole : $phone", fontSize = 11.5.sp, color = ForestGreenPrimary, fontWeight = FontWeight.Medium)
        }
    }
}
