package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssociationType
import com.example.data.model.CollectionPoint
import com.example.data.model.CollectionPointType
import com.example.data.model.Contribution
import com.example.data.model.ContributionStatus
import com.example.data.model.Fundraiser
import com.example.data.model.FundraiserStatus
import com.example.data.model.UserProfile
import com.example.data.repository.CollectionPointForm
import com.example.data.repository.FundraiserForm
import com.example.ui.AppViewModel
import com.example.ui.Route
import com.example.ui.components.AppCard
import com.example.ui.components.AppTextField
import com.example.ui.components.BannerKind
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.DropdownField
import com.example.ui.components.EmptyState
import com.example.ui.components.InfoBanner
import com.example.ui.components.Pill
import com.example.ui.components.PrimaryAction
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.SectionTitle
import com.example.ui.components.SecondaryAction
import com.example.ui.components.SmallTextButton
import com.example.ui.components.SmallTonalButton
import com.example.ui.components.TabScaffold
import com.example.ui.components.CertifiedBadge
import com.example.ui.components.EmergencyBadge
import com.example.ui.theme.DonGreen
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.TrocAmber
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun fcfa(v: Long): String = NumberFormat.getInstance(Locale.FRANCE).format(v) + " FCFA"
private fun dateOf(ms: Long): String = if (ms <= 0) "—" else SimpleDateFormat("dd MMM yyyy", Locale.FRANCE).format(Date(ms))

// =============================================================================================
//  Onglet « Solidarité » : cagnottes + points relais
// =============================================================================================

@Composable
fun SolidarityScreen(vm: AppViewModel) {
    val profile by vm.profile.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val me = profile ?: return

    TabScaffold(
        title = "Solidarité",
        floatingActionButton = {
            if (me.isCertifiedAssociation) {
                ExtendedFloatingActionButton(
                    onClick = { vm.push(if (tab == 0) Route.FundraiserCreate else Route.PointCreate) },
                    containerColor = ForestGreenPrimary, contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(if (tab == 0) "Nouvelle cagnotte" else "Nouveau relais", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Cagnottes") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Points relais") })
            }
            if (tab == 0) FundraiserList(vm, me) else RelaisList(vm, me)
        }
    }
}

@Composable
private fun FundraiserList(vm: AppViewModel, me: UserProfile) {
    val all by vm.fundraisers.collectAsState()
    val visible = all.filter {
        it.statusEnum == FundraiserStatus.ACTIVE || it.statusEnum == FundraiserStatus.FUNDED || it.associationId == me.id
    }.sortedWith(compareByDescending<Fundraiser> { it.isEmergency }.thenByDescending { it.createdAtMs })

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            InfoBanner(
                "Soutenez les orphelinats, écoles et centres sociaux certifiés : en Graines (immédiat) ou en FCFA (remis directement à l'association).",
                kind = BannerKind.INFO
            )
        }
        if (me.isAssociation && !me.isCertifiedAssociation) {
            item {
                InfoBanner("Vous pourrez créer des cagnottes dès que votre association sera certifiée.", kind = BannerKind.WARNING)
            }
        }
        if (visible.isEmpty()) {
            item { EmptyState(Icons.Default.Savings, "Aucune cagnotte pour le moment", "Les cagnottes validées par la médiation apparaîtront ici.") }
        }
        items(visible, key = { it.id }) { f -> FundraiserCard(f) { vm.push(Route.FundraiserDetail(f.id)) } }
    }
}

@Composable
private fun FundraiserCard(f: Fundraiser, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                CertifiedBadge()
                if (f.isEmergency) EmergencyBadge()
                if (f.statusEnum != FundraiserStatus.ACTIVE) StatusPill(f.statusEnum)
            }
            Text(f.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${f.associationName} • ${f.neighborhood.ifBlank { f.city }}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (f.targetFcfa > 0) ProgressLine("${fcfa(f.currentFcfa)} sur ${fcfa(f.targetFcfa)}", f.fcfaProgress, DonGreen)
            if (f.targetGrains > 0) ProgressLine("${f.currentGrains} sur ${f.targetGrains} 🌾 Graines", f.grainsProgress, TrocAmber)
            Text("${f.contributorsCount} contribution${if (f.contributorsCount > 1) "s" else ""} • jusqu'au ${dateOf(f.deadlineMs)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusPill(status: FundraiserStatus) {
    when (status) {
        FundraiserStatus.PENDING_REVIEW -> Pill(status.label, Color(0xFFE8F1FB), Color(0xFF0B4F8A), compact = true)
        FundraiserStatus.FUNDED -> Pill(status.label, Color(0xFFE6F4EA), Color(0xFF1B5E20), compact = true)
        FundraiserStatus.REJECTED -> Pill(status.label, Color(0xFFFDECEA), Color(0xFF8C1D18), compact = true)
        FundraiserStatus.CLOSED -> Pill(status.label, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, compact = true)
        FundraiserStatus.ACTIVE -> Unit
    }
}

@Composable
private fun ProgressLine(label: String, progress: Float, color: Color) {
    Column {
        Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress }, color = color, trackColor = color.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth().height(8.dp), strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

// =============================================================================================
//  Détail d'une cagnotte
// =============================================================================================

@Composable
fun FundraiserDetailScreen(vm: AppViewModel, id: String) {
    val profile by vm.profile.collectAsState()
    val all by vm.fundraisers.collectAsState()
    val received by vm.contributionsReceived.collectAsState()
    val mine by vm.myContributions.collectAsState()
    val busy by vm.busy.collectAsState()
    val me = profile ?: return
    val f = all.firstOrNull { it.id == id }
    var showGrains by remember { mutableStateOf(false) }
    var showFcfa by remember { mutableStateOf(false) }
    var showClose by remember { mutableStateOf(false) }

    ScreenScaffold(title = "Cagnotte", onBack = { vm.back() }) { padding ->
        if (f == null) {
            EmptyState(Icons.Default.Savings, "Cagnotte introuvable", "Elle a peut-être été retirée.", Modifier.padding(padding), "Retour", { vm.back() })
            return@ScreenScaffold
        }
        val isOwner = f.associationId == me.id
        val canGive = f.statusEnum == FundraiserStatus.ACTIVE && !isOwner && !me.isMediator

        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CertifiedBadge(); if (f.isEmergency) EmergencyBadge(); StatusPill(f.statusEnum)
            }
            Text(f.title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("${f.associationName} • ${f.neighborhood.ifBlank { f.city }}, ${f.city}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(f.description, fontSize = 14.sp, lineHeight = 21.sp)

            AppCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (f.targetFcfa > 0) ProgressLine("${fcfa(f.currentFcfa)} sur ${fcfa(f.targetFcfa)}", f.fcfaProgress, DonGreen)
                    if (f.targetGrains > 0) ProgressLine("${f.currentGrains} sur ${f.targetGrains} 🌾 Graines", f.grainsProgress, TrocAmber)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text("${f.contributorsCount} contributions • échéance ${dateOf(f.deadlineMs)}", fontSize = 12.5.sp)
                    }
                }
            }

            if (f.statusEnum == FundraiserStatus.PENDING_REVIEW) {
                InfoBanner("Cette cagnotte est en cours de validation par la médiation. Elle sera visible par tous une fois approuvée.", kind = BannerKind.INFO)
            }
            if (f.proofNote.isNotBlank()) InfoBanner(f.proofNote, title = "Justificatifs", kind = BannerKind.SUCCESS)

            if (f.payInstructions.isNotBlank()) {
                InfoBanner(f.payInstructions, title = "Comment remettre un don en FCFA", kind = BannerKind.WARNING)
            }
            if (f.contactPhone.isNotBlank()) CallButton(f.contactPhone)

            if (canGive) {
                PrimaryAction("Donner des Graines 🌾 (vous : ${me.grains})", { showGrains = true }, enabled = me.grains > 0 && !busy)
                if (f.targetFcfa > 0) SecondaryAction("Promettre un don en FCFA", { showFcfa = true })
                InfoBanner(
                    "Votre don en FCFA est une promesse : il est compté quand l'association confirme l'avoir reçu. Ne payez que selon les instructions ci-dessus.",
                    kind = BannerKind.INFO
                )
            }

            val myOnThis = mine.filter { it.fundraiserId == f.id }
            if (myOnThis.isNotEmpty()) {
                SectionTitle("Mes contributions")
                myOnThis.forEach { c -> ContributionLine(c) }
            }

            if (isOwner) {
                val list = received.filter { it.fundraiserId == f.id }
                SectionTitle("Contributions reçues (${list.size})")
                if (list.isEmpty()) Text("Aucune contribution pour le moment.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                list.forEach { c ->
                    AppCard {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ContributionLine(c, showName = true)
                            if (c.statusEnum == ContributionStatus.PLEDGED) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SmallTonalButton("Argent reçu", { vm.confirmPledge(c, f, true) }, enabled = !busy)
                                    SmallTextButton("Non reçu", { vm.confirmPledge(c, f, false) })
                                }
                            }
                        }
                    }
                }
                if (f.statusEnum == FundraiserStatus.ACTIVE || f.statusEnum == FundraiserStatus.FUNDED) {
                    SecondaryAction("Clôturer la cagnotte", { showClose = true })
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (showGrains) GiveGrainsDialog(me.grains, onDismiss = { showGrains = false }) { g, anon -> vm.giveGrains(f, g, anon) { } }
        if (showFcfa) PledgeDialog(onDismiss = { showFcfa = false }) { amount, anon -> vm.pledgeFcfa(f, amount, anon) { } }
        if (showClose) ConfirmDialog(
            "Clôturer la cagnotte ?", "Plus personne ne pourra contribuer. Confirmez d'abord les dons reçus.", "Clôturer",
            onConfirm = { vm.closeFundraiser(f) }, onDismiss = { showClose = false }, destructive = true
        )
    }
}

@Composable
private fun ContributionLine(c: Contribution, showName: Boolean = false) {
    val amount = buildString {
        if (c.amountFcfa > 0) append(fcfa(c.amountFcfa))
        if (c.grains > 0) { if (isNotEmpty()) append(" + "); append("${c.grains} 🌾") }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(if (showName) "${c.contributorName} : $amount" else amount, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
            Text(dateOf(c.createdAtMs), fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when (c.statusEnum) {
            ContributionStatus.CONFIRMED -> Pill("Reçu", Color(0xFFE6F4EA), Color(0xFF1B5E20), compact = true)
            ContributionStatus.PLEDGED -> Pill("À confirmer", Color(0xFFFFF4E0), Color(0xFF7A4B00), compact = true)
            ContributionStatus.REJECTED -> Pill("Non reçu", Color(0xFFFDECEA), Color(0xFF8C1D18), compact = true)
        }
    }
}

@Composable
private fun CallButton(phone: String) {
    val context = LocalContext.current
    SecondaryAction("Appeler l'association : $phone", {
        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' }))) }
    })
}

@Composable
private fun GiveGrainsDialog(balance: Int, onDismiss: () -> Unit, onConfirm: (Int, Boolean) -> Unit) {
    var grains by remember { mutableFloatStateOf(minOf(5, balance).toFloat()) }
    var anonymous by remember { mutableStateOf(false) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Donner des Graines", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${grains.toInt()} 🌾 sur $balance", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Slider(value = grains, onValueChange = { grains = it }, valueRange = 1f..balance.coerceAtLeast(1).toFloat(), steps = (balance - 2).coerceAtLeast(0))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = anonymous, onCheckedChange = { anonymous = it })
                    Spacer(Modifier.width(8.dp)); Text("Don anonyme", fontSize = 13.sp)
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onConfirm(grains.toInt(), anonymous); onDismiss() }, enabled = balance > 0) { Text("Donner", fontWeight = FontWeight.Bold) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun PledgeDialog(onDismiss: () -> Unit, onConfirm: (Long, Boolean) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var anonymous by remember { mutableStateOf(false) }
    val value = amount.filter { it.isDigit() }.toLongOrNull() ?: 0L
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Promettre un don", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(amount, { amount = it.filter { c -> c.isDigit() }.take(9) }, "Montant en FCFA", keyboardType = KeyboardType.Number)
                Text("Minimum 100 FCFA. L'association confirmera la réception.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = anonymous, onCheckedChange = { anonymous = it })
                    Spacer(Modifier.width(8.dp)); Text("Don anonyme", fontSize = 13.sp)
                }
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onConfirm(value, anonymous); onDismiss() }, enabled = value >= 100) { Text("Promettre", fontWeight = FontWeight.Bold) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

// =============================================================================================
//  Création d'une cagnotte
// =============================================================================================

@Composable
fun FundraiserCreateScreen(vm: AppViewModel) {
    val busy by vm.busy.collectAsState()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var targetFcfa by remember { mutableStateOf("") }
    var targetGrains by remember { mutableStateOf("") }
    var days by remember { mutableIntStateOf(30) }
    var category by remember { mutableStateOf(AssociationType.ORPHELINAT) }
    var emergency by remember { mutableStateOf(false) }
    var payInfo by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var proof by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Nouvelle cagnotte", onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoBanner("Un médiateur vérifie chaque cagnotte avant publication. Décrivez précisément l'usage des fonds et ajoutez vos justificatifs.", kind = BannerKind.INFO)
            AppTextField(title, { title = it }, "Titre", maxLength = 150, placeholder = "Ex. Rentrée scolaire des 40 enfants")
            AppTextField(description, { description = it }, "Description et usage des fonds", singleLine = false, minLines = 4, maxLength = 2500)
            DropdownField("Type de projet", AssociationType.entries, category, { it.label }, { category = it })
            AppTextField(neighborhood, { neighborhood = it }, "Quartier", maxLength = 100)
            AppTextField(targetFcfa, { targetFcfa = it.filter { c -> c.isDigit() }.take(9) }, "Objectif en FCFA (facultatif)", keyboardType = KeyboardType.Number)
            AppTextField(targetGrains, { targetGrains = it.filter { c -> c.isDigit() }.take(6) }, "Objectif en Graines (facultatif)", keyboardType = KeyboardType.Number)
            DropdownField("Durée", listOf(15, 30, 60, 90), days, { "$it jours" }, { days = it })
            AppTextField(payInfo, { payInfo = it }, "Comment remettre un don en FCFA", singleLine = false, minLines = 2, maxLength = 500,
                placeholder = "Ex. Remise à la paroisse le dimanche, ou Mobile Money au 6XX XX XX XX")
            AppTextField(phone, { phone = it }, "Téléphone de contact", keyboardType = KeyboardType.Phone, maxLength = 30)
            AppTextField(proof, { proof = it }, "Justificatifs (devis, lettre de la chefferie…)", singleLine = false, minLines = 2, maxLength = 1000)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Besoin urgent", fontWeight = FontWeight.SemiBold)
                    Text("Mise en avant en tête de liste", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = emergency, onCheckedChange = { emergency = it })
            }
            error?.let { InfoBanner(it, kind = BannerKind.DANGER) }
            PrimaryAction("Envoyer pour validation", enabled = !busy, onClick = {
                val fcfa = targetFcfa.toLongOrNull() ?: 0L
                val grains = targetGrains.toIntOrNull() ?: 0
                error = when {
                    title.trim().length < 3 -> "Donnez un titre à la cagnotte."
                    description.trim().length < 20 -> "Décrivez le projet et l'usage des fonds (20 caractères minimum)."
                    fcfa <= 0 && grains <= 0 -> "Indiquez un objectif en FCFA ou en Graines."
                    fcfa > 0 && payInfo.trim().length < 5 -> "Expliquez comment remettre un don en FCFA."
                    else -> null
                }
                if (error == null) {
                    vm.createFundraiser(
                        FundraiserForm(
                            title, description, neighborhood, fcfa, grains,
                            System.currentTimeMillis() + days * 24L * 3600 * 1000,
                            category, emergency, payInfo, phone, proof
                        )
                    ) { vm.back() }
                }
            })
            Spacer(Modifier.height(24.dp))
        }
    }
}

// =============================================================================================
//  Points relais
// =============================================================================================

@Composable
private fun RelaisList(vm: AppViewModel, me: UserProfile) {
    val points by vm.collectionPoints.collectAsState()
    var toDelete by remember { mutableStateOf<CollectionPoint?>(null) }
    val sorted = points.sortedWith(compareByDescending<CollectionPoint> { it.city == me.city }.thenBy { it.neighborhood })

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            InfoBanner(
                "Les points relais sont des lieux sûrs et connus (paroisses, chefferies, orphelinats…) où déposer ou remettre des objets.",
                kind = BannerKind.INFO
            )
        }
        if (sorted.isEmpty()) {
            item {
                EmptyState(Icons.Default.Place, "Aucun point relais",
                    if (me.isCertifiedAssociation) "Ajoutez le lieu de votre association pour faciliter les remises."
                    else "Les associations certifiées peuvent ajouter leurs lieux ici.")
            }
        }
        items(sorted, key = { it.id }) { p ->
            AppCard {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${p.typeLabel} • ${p.organizationName}", fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${p.landmark.ifBlank { p.neighborhood }}, ${p.city}", fontSize = 13.sp)
                    }
                    if (p.openingHours.isNotBlank()) Text("Horaires : ${p.openingHours}", fontSize = 12.5.sp)
                    if (p.acceptedCategories.isNotBlank()) Text("Accepte : ${p.acceptedCategories}", fontSize = 12.5.sp)
                    if (p.contactPhone.isNotBlank()) CallButton(p.contactPhone)
                    if (p.ownerId == me.id || me.isMediator) SmallTextButton("Supprimer ce relais", { toDelete = p })
                }
            }
        }
    }
    toDelete?.let { p ->
        ConfirmDialog("Supprimer ce point relais ?", p.name, "Supprimer", { vm.deletePoint(p) }, { toDelete = null }, destructive = true)
    }
}

@Composable
fun PointCreateScreen(vm: AppViewModel) {
    val busy by vm.busy.collectAsState()
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CollectionPointType.CENTRE_SOCIAL) }
    var neighborhood by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "Nouveau point relais", onBack = { vm.back() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(name, { name = it }, "Nom du lieu", maxLength = 120, placeholder = "Ex. Paroisse Saint-Jean, salle paroissiale")
            DropdownField("Type de lieu", CollectionPointType.entries, type, { it.label }, { type = it })
            AppTextField(neighborhood, { neighborhood = it }, "Quartier", maxLength = 100)
            AppTextField(landmark, { landmark = it }, "Repère pour s'y rendre", maxLength = 150)
            AppTextField(phone, { phone = it }, "Téléphone", keyboardType = KeyboardType.Phone, maxLength = 30)
            AppTextField(hours, { hours = it }, "Horaires d'ouverture", maxLength = 150, placeholder = "Ex. Lun–Sam 8h–17h")
            AppTextField(accepted, { accepted = it }, "Objets acceptés", maxLength = 300, placeholder = "Ex. vêtements, fournitures scolaires")
            error?.let { InfoBanner(it, kind = BannerKind.DANGER) }
            PrimaryAction("Ajouter le point relais", enabled = !busy, onClick = {
                error = when {
                    name.trim().length < 2 -> "Indiquez le nom du lieu."
                    neighborhood.isBlank() -> "Indiquez le quartier."
                    else -> null
                }
                if (error == null) vm.createPoint(CollectionPointForm(name, type, neighborhood, landmark, phone, hours, accepted)) { vm.back() }
            })
            Spacer(Modifier.height(24.dp))
        }
    }
}
