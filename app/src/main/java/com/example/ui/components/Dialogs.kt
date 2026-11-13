package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DisputeAction
import com.example.data.model.DisputeReason

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message, fontSize = 14.sp, lineHeight = 20.sp) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

/** Saisie d'un texte libre (motif de refus, note de médiation…). */
@Composable
fun NoteDialog(
    title: String,
    label: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    required: Boolean = false
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(500) }, label = { Text(label) },
                minLines = 3, modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text); onDismiss() }, enabled = !required || text.isNotBlank()) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

private val reviewBadges = listOf("Ponctuel", "Aimable", "Objet conforme", "Très serviable")

@Composable
fun ReviewDialog(
    targetName: String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, badge: String, comment: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var badge by remember { mutableStateOf(reviewBadges.first()) }
    var comment by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Votre avis sur $targetName", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row {
                    for (star in 1..5) {
                        Icon(
                            if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "$star étoile${if (star > 1) "s" else ""}",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(38.dp).clickable { rating = star }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        reviewBadges.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                pair.forEach { b ->
                                    FilterChip(selected = badge == b, onClick = { badge = b }, label = { Text(b, fontSize = 12.sp) })
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment, onValueChange = { comment = it.take(300) },
                    label = { Text("Commentaire (facultatif)") }, minLines = 2, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSubmit(rating, badge, comment); onDismiss() }) { Text("Publier", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Plus tard") } }
    )
}

@Composable
fun ReportDialog(
    itemTitle: String,
    onDismiss: () -> Unit,
    onSubmit: (DisputeReason, String) -> Unit
) {
    var reason by remember { mutableStateOf(DisputeReason.ITEM_MISMATCH) }
    var details by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Signaler « $itemTitle »", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DisputeReason.entries.forEach { r ->
                    Row(
                        Modifier.fillMaxWidth().clickable { reason = r },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = reason == r, onClick = { reason = r })
                        Text(r.label, fontSize = 13.5.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = details, onValueChange = { details = it.take(600) },
                    label = { Text("Expliquez la situation") }, minLines = 3, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(reason, details); onDismiss() }, enabled = details.trim().length >= 3) {
                Text("Envoyer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
fun ResolveDisputeDialog(
    onDismiss: () -> Unit,
    onSubmit: (dismissed: Boolean, note: String, action: DisputeAction) -> Unit
) {
    var dismissed by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf(DisputeAction.NONE) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Décision de médiation", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = dismissed, onCheckedChange = { dismissed = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (dismissed) "Signalement non fondé (rejeté)" else "Signalement fondé", fontSize = 13.5.sp)
                }
                if (!dismissed) {
                    Spacer(Modifier.height(6.dp))
                    Text("Mesure appliquée", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    DisputeAction.entries.forEach { a ->
                        Row(Modifier.fillMaxWidth().clickable { action = a }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = action == a, onClick = { action = a })
                            Text(a.label, fontSize = 13.5.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note, onValueChange = { note = it.take(500) },
                    label = { Text("Motif de la décision (envoyé aux parties)") }, minLines = 3, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(dismissed, note, action); onDismiss() }, enabled = note.trim().length >= 3) {
                Text("Clore le litige", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
