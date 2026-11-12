package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.DisputeReason
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.TrocAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewDialog(
    targetUserId: String,
    targetUserName: String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, badge: String, comment: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var selectedBadge by remember { mutableStateOf("Voisin fraternel") }
    var comment by remember { mutableStateOf("") }

    val badges = listOf("Voisin fraternel", "Donneur généreux", "Éco-citoyen", "Ponctuel au carrefour", "Communication au top")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Évaluer votre voisin",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Comment s'est passée votre rencontre avec $targetUserName ?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    (1..5).forEach { starIndex ->
                        val isFilled = starIndex <= rating
                        Icon(
                            imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$starIndex étoiles",
                            tint = if (isFilled) Color(0xFFFFB300) else Color.Gray,
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { rating = starIndex }
                                .padding(2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Badge de fraternité :",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    badges.forEach { badge ->
                        val isSelected = selectedBadge == badge
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedBadge = badge },
                            label = { Text(badge, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Votre mot d'encouragement") },
                    placeholder = { Text("Ex: Merci pour ce don généreux pour les enfants !") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalComment = if (comment.isBlank()) "Échange fraternel parfait !" else comment.trim()
                    onSubmit(rating, selectedBadge, finalComment)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_review_button")
            ) {
                Text("Publier l'avis", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Plus tard")
            }
        }
    )
}

@Composable
fun ReportDisputeDialog(
    itemTitle: String,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, details: String) -> Unit
) {
    var selectedReason by remember { mutableStateOf(DisputeReason.NO_SHOW) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F)) },
        title = { Text("Signaler à la médiation", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Objet concerné : \"$itemTitle\"",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Motif du signalement :",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                DisputeReason.values().forEach { reason ->
                    val isSelected = selectedReason == reason
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedReason = reason },
                        label = { Text(reason.label, fontSize = 11.5.sp) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEBEE),
                            selectedLabelColor = Color(0xFFD32F2F)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Explications pour le médiateur") },
                    placeholder = { Text("Décrivez précisément ce qui s'est passé...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedReason.name, details) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Envoyer au médiateur", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun ContributeFundraiserDialog(
    fundraiserTitle: String,
    userGrains: Int,
    onDismiss: () -> Unit,
    onSubmit: (amountFcfa: Long, grains: Int) -> Unit
) {
    var mode by remember { mutableStateOf("GRAINES") } // or "MOMO"
    var selectedGrains by remember { mutableIntStateOf(10) }
    var selectedFcfa by remember { mutableStateOf(2000L) }
    var phoneInput by remember { mutableStateOf("+237 6") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contribuer à la levée de fonds", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = fundraiserTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ForestGreenPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Mode switcher (Graines de solidarité vs Mobile Money)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == "GRAINES",
                        onClick = { mode = "GRAINES" },
                        label = { Text("🌾 Graines ($userGrains dispo)", fontSize = 11.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mode == "MOMO",
                        onClick = { mode = "MOMO" },
                        label = { Text("📱 Mobile Money", fontSize = 11.5.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (mode == "GRAINES") {
                    Text(text = "Nombre de Graines à verser :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 20, 30).forEach { g ->
                            FilterChip(
                                selected = selectedGrains == g,
                                onClick = { selectedGrains = g },
                                label = { Text("$g 🌾", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    Text(
                        text = "Vos Graines issues de dons d'objets aident l'association à progresser vers son objectif !",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(text = "Montant du don (FCFA) :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1000L, 2000L, 5000L, 10000L).forEach { amount ->
                            FilterChip(
                                selected = selectedFcfa == amount,
                                onClick = { selectedFcfa = amount },
                                label = { Text("${amount / 1000}k F", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Numéro MTN / Orange Money") },
                        placeholder = { Text("+237 6XX XX XX XX") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Une notification de validation de débit sécurisé apparaîtra sur votre téléphone.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mode == "GRAINES") {
                        onSubmit(0L, selectedGrains)
                    } else {
                        onSubmit(selectedFcfa, 0)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (mode == "GRAINES") "Valider le don de Graines" else "Donner $selectedFcfa FCFA",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialog requiring proof of barter item and safety agreement before reserving a TROC item.
 */
@Composable
fun ReserveTrocWithProofDialog(
    itemTitle: String,
    wantedInReturn: String,
    onDismiss: () -> Unit,
    onConfirm: (proofDescription: String, proofPhotoUri: String?) -> Unit
) {
    var proofDescription by remember { mutableStateOf("") }
    var proofPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var acceptsPublicMeetingSafety by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val proofPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        proofPhotoUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SyncAlt, contentDescription = null, tint = TrocAmber, modifier = Modifier.size(28.dp)) },
        title = { Text("Preuve de Troc & Sécurité", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Réservation de : \"$itemTitle\"",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (wantedInReturn.isNotBlank()) {
                    Text(
                        text = "Échange demandé par le voisin : $wantedInReturn",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TrocAmber
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Upload photo proof of barter item
                Text(
                    text = "1. Photo-preuve de votre objet à échanger :",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            proofPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                ) {
                    if (proofPhotoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                        ) {
                            AsyncImage(
                                model = proofPhotoUri,
                                contentDescription = "Preuve de troc",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Preuve jointe", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TrocAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Ajouter une photo-preuve", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Prouve au voisin que l'objet proposé est en bon état", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = proofDescription,
                    onValueChange = { proofDescription = it },
                    label = { Text("Description de votre objet proposé") },
                    placeholder = { Text("Ex: Fer à repasser Calor fonctionnel en très bon état...") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // MANDATORY SAFETY AGREEMENT
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF8E1),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = acceptsPublicMeetingSafety,
                            onCheckedChange = { acceptsPublicMeetingSafety = it },
                            colors = CheckboxDefaults.colors(checkedColor = ForestGreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Protocole de Sécurité : Je m'engage à effectuer l'échange exclusivement dans un lieu public éclairé (Pharmacie, Station, Carrefour).",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            lineHeight = 14.sp
                        )
                    }
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMsg ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!acceptsPublicMeetingSafety) {
                        errorMsg = "Veuillez cocher l'engagement de sécurité dans un lieu public."
                        return@Button
                    }
                    errorMsg = null
                    onConfirm(proofDescription.ifBlank { "Objet conforme pour troc" }, proofPhotoUri?.toString())
                },
                colors = ButtonDefaults.buttonColors(containerColor = TrocAmber),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Confirmer la réservation", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
