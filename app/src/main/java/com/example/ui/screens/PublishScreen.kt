package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.ExchangeType
import com.example.data.model.GrainRules
import com.example.data.model.ItemCondition
import com.example.ui.AppViewModel
import com.example.ui.components.AppTextField
import com.example.ui.components.BannerKind
import com.example.ui.components.DropdownField
import com.example.ui.components.InfoBanner
import com.example.ui.components.PrimaryAction
import com.example.ui.components.ScreenScaffold
import com.example.ui.theme.DonGreen
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.TrocAmber
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PublishScreen(vm: AppViewModel) {
    val profile by vm.profile.collectAsState()
    val busy by vm.busy.collectAsState()
    val suggestion by vm.suggestion.collectAsState()
    val suggesting by vm.suggesting.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val me = profile ?: return

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.AUTRE) }
    var condition by remember { mutableStateOf(ItemCondition.BON_ETAT) }
    var exchange by remember { mutableStateOf(ExchangeType.DON) }
    var wanted by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var emergency by remember { mutableStateOf(false) }
    var orphanage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { vm.clearSuggestion() } }

    // L'assistant IA pré-remplit les champs encore vides ; l'utilisateur garde la main.
    LaunchedEffect(suggestion) {
        suggestion?.let { s ->
            if (title.isBlank()) title = s.title
            if (description.isBlank()) description = s.description
            category = s.category
            condition = s.condition
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { ImageUtils.loadScaledBitmap(context, uri) }
                if (bmp == null) error = "Impossible de lire cette photo." else { bitmap = bmp; error = null }
            }
        }
    }

    ScreenScaffold(title = if (me.isAssociation) "Publier un don" else "Donner ou troquer", onBack = { vm.back() }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Photo
            Box(
                Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), contentDescription = "Photo de l'objet", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Ajouter une photo", fontWeight = FontWeight.Bold)
                        Text("Une bonne photo multiplie les demandes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            OutlinedButton(
                onClick = { vm.suggest(bitmap, title.ifBlank { "objet à donner" }) },
                enabled = !suggesting && (bitmap != null || title.isNotBlank()),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (suggesting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (suggesting) "Analyse en cours…" else "Remplir avec l'assistant IA")
            }

            // Type d'échange
            Text("Je souhaite", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExchangeChoice("Donner", ExchangeType.DON.description, exchange == ExchangeType.DON, DonGreen, Modifier.weight(1f)) { exchange = ExchangeType.DON }
                ExchangeChoice("Troquer", ExchangeType.TROC.description, exchange == ExchangeType.TROC, TrocAmber, Modifier.weight(1f)) { exchange = ExchangeType.TROC }
            }

            AppTextField(title, { title = it }, "Titre de l'annonce", maxLength = 120, placeholder = "Ex. Sac d'école, ventilateur…")
            AppTextField(description, { description = it }, "Description", singleLine = false, minLines = 3, maxLength = 1000,
                placeholder = "État réel, taille, défauts éventuels…")
            DropdownField("Catégorie", Category.entries.filter { it != Category.TOUT }, category, { it.label }, { category = it })
            DropdownField("État de l'objet", ItemCondition.entries, condition, { it.label }, { condition = it })

            if (exchange == ExchangeType.TROC) {
                AppTextField(wanted, { wanted = it }, "Ce que vous cherchez en échange", maxLength = 200,
                    placeholder = "Ex. cahiers, ustensiles de cuisine…")
            }

            if (!me.isAssociation) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Répondre à un besoin urgent", fontWeight = FontWeight.SemiBold)
                        Text("Priorité aux orphelinats et écoles", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = emergency, onCheckedChange = { emergency = it })
                }
                if (emergency) {
                    AppTextField(orphanage, { orphanage = it }, "Association destinataire (facultatif)", maxLength = 120)
                }
            }

            InfoBanner(
                "Lieu de rencontre : ${me.landmark.ifBlank { me.neighborhood.ifBlank { me.city } }}. " +
                        "Seule une position approximative est montrée aux autres.",
                kind = BannerKind.INFO
            )
            InfoBanner(
                if (me.isTrusted) "Votre niveau ${me.level.label} vous permet de publier sans attendre : l'annonce est visible tout de suite."
                else "Votre annonce sera vérifiée par un médiateur avant d'être visible. Après ${GrainRules.TRUSTED_THRESHOLD} Graines gagnées, vos annonces seront publiées directement.",
                kind = BannerKind.INFO
            )

            error?.let { InfoBanner(it, kind = BannerKind.DANGER) }

            PrimaryAction(
                text = "Publier l'annonce",
                enabled = !busy,
                tag = "publish_submit",
                onClick = {
                    error = when {
                        title.trim().length < 2 -> "Donnez un titre à votre annonce."
                        exchange == ExchangeType.TROC && wanted.trim().length < 2 -> "Indiquez ce que vous cherchez en échange."
                        else -> null
                    }
                    if (error == null) {
                        vm.publish(title, description, category, condition, exchange, wanted, bitmap, emergency, orphanage) {
                            vm.back()
                        }
                    }
                }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ExchangeChoice(
    title: String, subtitle: String, selected: Boolean, color: androidx.compose.ui.graphics.Color,
    modifier: Modifier, onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp, if (selected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (selected) color else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 15.sp)
        }
    }
}
