package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ExchangeType
import com.example.data.model.ItemStatus
import com.example.ui.TrocViewModel
import com.example.ui.components.EmergencyBadge
import com.example.ui.components.ExchangeBadge
import com.example.ui.components.RatingStars
import com.example.ui.components.ReportDisputeDialog
import com.example.ui.components.ReserveTrocWithProofDialog
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.DonGreen
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.TrocAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    viewModel: TrocViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val item by viewModel.currentItem.collectAsState()
    val scrollState = rememberScrollState()

    var showReportDialog by remember { mutableStateOf(false) }
    var showReserveTrocDialog by remember { mutableStateOf(false) }
    var reportFeedback by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détail de l'objet solidaire", fontSize = 16.5.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showReportDialog = true },
                        modifier = Modifier.testTag("report_item_button")
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Signaler à la médiation", tint = Color(0xFFD32F2F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            item?.let { currentItem ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.openChat(currentItem.id) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Discuter", fontWeight = FontWeight.Bold)
                        }

                        if (currentItem.status == ItemStatus.AVAILABLE.name) {
                            Button(
                                onClick = {
                                    if (currentItem.exchangeType == ExchangeType.TROC.name) {
                                        showReserveTrocDialog = true
                                    } else {
                                        viewModel.reserveCurrentItem()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentItem.exchangeType == ExchangeType.TROC.name) TrocAmber else ForestGreenPrimary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1.3f).height(48.dp).testTag("reserve_item_button")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentItem.exchangeType == ExchangeType.TROC.name) "Troquer (Preuve)" else "Réserver le Don",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else if (currentItem.status == ItemStatus.RESERVED.name) {
                            Button(
                                onClick = { viewModel.completeCurrentItemExchange() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1.3f).height(48.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Finaliser l'échange", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1.3f).height(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Objet transmis", fontWeight = FontWeight.Bold, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        item?.let { currentItem ->
            val isTroc = currentItem.exchangeType == ExchangeType.TROC.name

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
            ) {
                // Visual Header with Photo if present
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(if (isTroc) TrocAmber.copy(alpha = 0.12f) else DonGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentItem.imageUri.isNotBlank()) {
                        AsyncImage(
                            model = currentItem.imageUri,
                            contentDescription = currentItem.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.60f))
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp)
                        ) {
                            ExchangeBadge(exchangeType = currentItem.exchangeType)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(if (isTroc) TrocAmber else DonGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getCategoryIcon(currentItem.category),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            ExchangeBadge(exchangeType = currentItem.exchangeType)
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    if (reportFeedback != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Text(
                                text = reportFeedback ?: "",
                                color = Color(0xFFD32F2F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    if (currentItem.isEmergencyResponse) {
                        EmergencyBadge()
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = currentItem.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location & Landmark
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ForestGreenPrimary.copy(alpha = 0.12f)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${currentItem.city} • ${currentItem.neighborhood}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreenPrimary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF9C4)
                        ) {
                            Text(
                                text = "Récompense : +${currentItem.grainsReward} 🌾",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description
                    Text(text = "Description de l'objet", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentItem.description,
                        fontSize = 13.5.sp,
                        lineHeight = 21.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Proposé par
                    Text(text = "Proposé par", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(CircleShape).background(ForestGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = currentItem.donorName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = currentItem.donorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                RatingStars(rating = currentItem.donorRating, reviewCount = currentItem.donorReviewsCount)
                                Text(
                                    text = "Repère convenu : ${currentItem.landmark}",
                                    fontSize = 11.sp,
                                    color = ForestGreenPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // MANDATORY SAFETY PROTOCOL & ANTI-ILLEGAL ACTIVITIES BANNER
                    Text(text = "Protocole de Sécurité Citoyenne", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE65100)),
                        modifier = Modifier.fillMaxWidth().testTag("safety_protocol_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Lieu Public Obligatoire • Anti-Fraude & Sécurité", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Rencontre uniquement dans un lieu public éclairé : Carrefour public, Pharmacie, Station-service, Chefferie ou Point Relais.\n• Ne jamais vous rendre seul au domicile privé d'un inconnu.\n• Signalez immédiatement tout comportement suspect à l'équipe de Médiation via le bouton d'alerte en haut.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF4E2700),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showReportDialog && item != null) {
        ReportDisputeDialog(
            itemTitle = item?.title ?: "",
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                viewModel.reportCurrentItem(reason, details)
                reportFeedback = "Signalement transmis à l'équipe de médiation. Merci pour votre vigilance citoyenne."
                showReportDialog = false
            }
        )
    }

    if (showReserveTrocDialog && item != null) {
        ReserveTrocWithProofDialog(
            itemTitle = item?.title ?: "",
            wantedInReturn = item?.wantedInReturn ?: "",
            onDismiss = { showReserveTrocDialog = false },
            onConfirm = { proofDescription, proofPhotoUri ->
                viewModel.proposeBarter(item!!.id, proofDescription, proofPhotoUri)
                reportFeedback = "Proposition de troc avec preuve envoyée au chat ! Le propriétaire l'examine avant validation."
                showReserveTrocDialog = false
            }
        )
    }
}
