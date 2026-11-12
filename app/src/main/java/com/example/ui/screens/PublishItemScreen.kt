package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.Category
import com.example.data.model.ExchangeType
import com.example.data.model.ItemCondition
import com.example.ui.TrocViewModel
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.DonGreen
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.TrocAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishItemScreen(
    viewModel: TrocViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val currentUser by viewModel.currentUser.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.SCOLAIRE_EDUCATION) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var selectedCondition by remember { mutableStateOf(ItemCondition.BON_ETAT) }
    var exchangeType by remember { mutableStateOf(ExchangeType.DON) }
    var wantedInReturn by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var isEmergencyResponse by remember { mutableStateOf(false) }
    var targetOrphanage by remember { mutableStateOf("") }
    var customCity by remember { mutableStateOf(currentUser?.city ?: "Douala") }
    var customNeighborhood by remember { mutableStateOf(currentUser?.neighborhood ?: "Akwa") }
    var customLandmark by remember { mutableStateOf(currentUser?.landmark ?: "Carrefour Ndokoti") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Auto-fill when AI analysis completes
    LaunchedEffect(analysisResult) {
        analysisResult?.let { res ->
            if (title.isBlank()) {
                title = res.detectedTitle
            }
            selectedCategory = res.suggestedCategory
            selectedCondition = res.suggestedCondition
            if (res.matchingEmergencyNeed != null) {
                isEmergencyResponse = true
                targetOrphanage = "Orphelinat Sainte-Thérèse de Deido"
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        photoUri = uri
        if (uri != null) {
            // Automatically launch smart analysis
            viewModel.analyzePhoto(title.ifBlank { "objet solidaire" }, uri.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Donner ou Proposer au Troc", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("publish_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Photo Picker & Real Image Preview Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("photo_picker_card")
            ) {
                if (photoUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Photo sélectionnée",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Changer la photo", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ForestGreenPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ajouter la photo de votre objet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sélectionner depuis votre galerie ou appareil photo",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SMART VISION ANALYSIS BUTTON
            Button(
                onClick = {
                    viewModel.analyzePhoto(
                        hint = title.ifBlank { "fournitures scolaires sac habits ventilateur" },
                        imageUri = photoUri?.toString()
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_analyze_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyser avec l'IA Solidaire", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }

            // AI Analysis High-Contrast Legible Result Card
            AnimatedVisibility(visible = analysisResult != null) {
                analysisResult?.let { res ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF673AB7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF673AB7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Résultat d'analyse IA Gemini",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "• Intitulé suggéré : ${res.detectedTitle}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• Catégorie : ${res.suggestedCategory.label}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• État estimé : ${res.suggestedCondition.label}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "• Récompense : +${res.recommendedGrains} 🌾 Graines de Solidarité",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreenPrimary
                                )
                                Text(
                                    text = "• Éco-gain estimé : ${res.estimatedCo2SavedKg} kg de CO₂ économisés",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (res.matchingEmergencyNeed != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFEBEE),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F))
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = res.matchingEmergencyNeed,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Mode d'échange : Don ou Troc en Graines
            Text(
                text = "Mode de partage solidaire",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Don Card
                val isDon = exchangeType == ExchangeType.DON
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDon) DonGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isDon) 2.dp else 1.dp,
                        if (isDon) DonGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { exchangeType = ExchangeType.DON }
                        .testTag("select_mode_don")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = DonGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Don caritatif", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = DonGreen)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("100% gratuit • Gagnez +20 🌾", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Troc en Graines Card
                val isTroc = exchangeType == ExchangeType.TROC
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTroc) TrocAmber.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isTroc) 2.dp else 1.dp,
                        if (isTroc) TrocAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { exchangeType = ExchangeType.TROC }
                        .testTag("select_mode_troc")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null, tint = TrocAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Troc solidaire", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TrocAmber)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Échange en Graines sans argent", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (exchangeType == ExchangeType.TROC) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = wantedInReturn,
                    onValueChange = { wantedInReturn = it },
                    label = { Text("Contre quoi souhaitez-vous troquer ?") },
                    placeholder = { Text("Ex: 15 Graines de Solidarité ou fer à repasser...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TrocAmber),
                    modifier = Modifier.fillMaxWidth().testTag("wanted_in_return_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Titre de l'objet
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre de l'objet *") },
                placeholder = { Text("Ex: Cartable scolaire, Ventilateur, Manuels...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreenPrimary),
                modifier = Modifier.fillMaxWidth().testTag("item_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Catégorie
            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Catégorie de l'objet") },
                    leadingIcon = {
                        Icon(getCategoryIcon(selectedCategory.name), contentDescription = null, tint = ForestGreenPrimary)
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreenPrimary),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    Category.values().filter { it != Category.TOUT }.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.label) },
                            leadingIcon = { Icon(cat.icon, contentDescription = null, tint = ForestGreenPrimary) },
                            onClick = {
                                selectedCategory = cat
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // État
            Text(text = "État de l'objet", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ItemCondition.values().forEach { cond ->
                    val isSelected = selectedCondition == cond
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCondition = cond },
                        label = { Text(cond.label, fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreenPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description & conseils de réemploi") },
                placeholder = { Text("Précisez l'état, le fonctionnement, la classe pour un manuel...") },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreenPrimary),
                modifier = Modifier.fillMaxWidth().testTag("item_description_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Don prioritaire à un orphelinat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F5E9))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isEmergencyResponse,
                    onCheckedChange = { isEmergencyResponse = it },
                    colors = CheckboxDefaults.colors(checkedColor = ForestGreenPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Don prioritaire pour orphelinat / urgence locale",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenDark
                    )
                    Text(
                        text = "Priorité accordée aux structures d'accueil d'enfants",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CUSTOM LOCATION SELECTION PER ITEM (Ville, Quartier, Repère Public)
            Text(text = "Lieu de rencontre pour cet objet", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customCity,
                    onValueChange = { customCity = it },
                    label = { Text("Ville") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = customNeighborhood,
                    onValueChange = { customNeighborhood = it },
                    label = { Text("Quartier") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customLandmark,
                onValueChange = { customLandmark = it },
                label = { Text("Repère public sécurisé *") },
                placeholder = { Text("Ex: Carrefour Ndokoti devant la Pharmacie / Station") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreenPrimary),
                modifier = Modifier.fillMaxWidth().testTag("custom_landmark_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Safety Warning Box for Custom Location
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFF3E0),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Exigez un repère public sécurisé (Station, Pharmacie, Carrefour). Les rendez-vous isolés sont interdits.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        lineHeight = 15.sp
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bouton de publication
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Veuillez renseigner un titre pour l'objet."
                        return@Button
                    }
                    if (customLandmark.isBlank()) {
                        errorMessage = "Veuillez préciser un repère public sécurisé pour le rendez-vous."
                        return@Button
                    }
                    errorMessage = null
                    viewModel.publishItem(
                        title = title.trim(),
                        description = description.trim().ifBlank { "Don solidaire remis en bon état entre voisins." },
                        category = selectedCategory,
                        condition = selectedCondition.name,
                        exchangeType = exchangeType,
                        wantedInReturn = wantedInReturn.trim(),
                        imageUri = photoUri?.toString() ?: "",
                        isEmergencyResponse = isEmergencyResponse,
                        targetOrphanageName = targetOrphanage.ifBlank { "Orphelinat local" },
                        onSuccess = { newId ->
                            viewModel.openItemDetail(newId)
                        }
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("publish_submit_button")
            ) {
                Text(
                    text = if (exchangeType == ExchangeType.DON) "Publier mon don solidaire (+20 🌾)" else "Proposer au troc en Graines",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
