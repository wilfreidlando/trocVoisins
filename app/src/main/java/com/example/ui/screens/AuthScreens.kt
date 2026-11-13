package com.example.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.AssociationType
import com.example.data.model.Cities
import com.example.data.model.UserRole
import com.example.data.repository.OnboardingForm
import com.example.firebase.performGoogleSignIn
import com.example.ui.AppViewModel
import com.example.ui.components.AppTextField
import com.example.ui.components.DropdownField
import com.example.ui.components.InfoBanner
import com.example.ui.components.BannerKind
import com.example.ui.components.PrimaryAction
import com.example.ui.components.UseMyLocationButton
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.google.firebase.auth.FirebaseUser

private fun validEmail(e: String) = android.util.Patterns.EMAIL_ADDRESS.matcher(e.trim()).matches()

@Composable
private fun BrandHeader(subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(ForestGreenPrimary), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Handshake, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("TrocVoisins", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ForestGreenDark)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Connexion ou création de compte (e-mail + mot de passe, ou Google). */
@Composable
fun SignInScreen(vm: AppViewModel) {
    val error by vm.authError.collectAsState()
    val busy by vm.busy.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var registerMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showSetup by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { showSetup = !vm.adminExists() }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        BrandHeader("Donnez, échangez, aidez.\nEntre voisins, près de chez vous.")

        TabRow(selectedTabIndex = if (registerMode) 1 else 0, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = !registerMode, onClick = { registerMode = false; localError = null; vm.clearAuthError() }, text = { Text("Connexion") })
            Tab(selected = registerMode, onClick = { registerMode = true; localError = null; vm.clearAuthError() }, text = { Text("Créer un compte") })
        }
        Spacer(Modifier.height(18.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(email, { email = it; localError = null }, "Adresse e-mail", keyboardType = KeyboardType.Email)
            AppTextField(password, { password = it; localError = null }, "Mot de passe", password = true)
            if (registerMode) {
                AppTextField(confirm, { confirm = it; localError = null }, "Confirmer le mot de passe", password = true)
            }
            (localError ?: error)?.let { InfoBanner(it, kind = BannerKind.DANGER) }

            PrimaryAction(
                text = if (registerMode) "Continuer" else "Se connecter",
                enabled = !busy,
                tag = "submit_auth_button",
                onClick = {
                    when {
                        !validEmail(email) -> localError = "Saisissez une adresse e-mail valide."
                        password.length < 6 -> localError = "Le mot de passe doit contenir au moins 6 caractères."
                        registerMode && password != confirm -> localError = "Les deux mots de passe ne correspondent pas."
                        registerMode -> vm.register(email, password)
                        else -> vm.signIn(email, password)
                    }
                }
            )

            if (!registerMode) {
                TextButton(onClick = { vm.resetPassword(email) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Mot de passe oublié ?", fontSize = 13.sp)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                Text("  ou  ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
            }

            Button(
                onClick = {
                    vm.clearAuthError()
                    performGoogleSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        scope = scope,
                        onSuccess = { },
                        onError = { vm.reportGoogleError("Connexion Google impossible. Utilisez votre e-mail.") }
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("google_auth_button")
            ) {
                Icon(painterResource(R.drawable.ic_google_logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Continuer avec Google", fontWeight = FontWeight.SemiBold)
            }
        }

        if (showSetup) {
            Spacer(Modifier.height(28.dp))
            InfoBanner(
                title = "Première installation",
                text = "Aucun administrateur n'existe encore. Si vous êtes le propriétaire de l'application, créez d'abord votre compte, puis choisissez l'option administrateur à l'étape suivante.",
                kind = BannerKind.WARNING
            )
            TextButton(
                onClick = { vm.setAdminSetupMode(true); registerMode = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) { Text("Je suis l'administrateur", fontSize = 13.sp) }
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** Étape de profil juste après la création du compte. */
@Composable
fun ProfileSetupScreen(vm: AppViewModel, user: FirebaseUser) {
    val adminMode by vm.adminSetupMode.collectAsState()
    val busy by vm.busy.collectAsState()
    val context = LocalContext.current

    var role by remember { mutableStateOf(UserRole.CITOYEN) }
    var name by remember { mutableStateOf(user.displayName.orEmpty()) }
    var city by remember { mutableStateOf(Cities.all.first().name) }
    var neighborhood by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf(0.0) }
    var lng by remember { mutableStateOf(0.0) }
    var locationMsg by remember { mutableStateOf<String?>(null) }

    var orgName by remember { mutableStateOf("") }
    var orgType by remember { mutableStateOf(AssociationType.ORPHELINAT) }
    var regNumber by remember { mutableStateOf("") }
    var orgPhone by remember { mutableStateOf("") }
    var setupCode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val isAssociation = !adminMode && role == UserRole.ASSOCIATION

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
    ) {
        BrandHeader(if (adminMode) "Configuration de l'administrateur" else "Dites-nous qui vous êtes")

        if (adminMode) {
            InfoBanner(
                text = "Ce compte aura les droits de médiateur : validation des annonces, litiges, certification des associations. Saisissez le code de configuration inscrit dans le fichier firestore.rules.",
                kind = BannerKind.WARNING
            )
            Spacer(Modifier.height(12.dp))
        } else {
            Text("Je rejoins TrocVoisins en tant que", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoleCard("Citoyen", "Je donne et j'échange entre voisins", Icons.Default.Handshake, role == UserRole.CITOYEN, Modifier.weight(1f)) { role = UserRole.CITOYEN }
                RoleCard("Association", "Orphelinat, paroisse, école, centre social", Icons.Default.Apartment, role == UserRole.ASSOCIATION, Modifier.weight(1f)) { role = UserRole.ASSOCIATION }
            }
            Spacer(Modifier.height(16.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppTextField(name, { name = it }, if (isAssociation) "Nom du responsable" else "Nom complet", maxLength = 100)

            if (isAssociation) {
                AppTextField(orgName, { orgName = it }, "Nom officiel de l'association", maxLength = 120)
                DropdownField("Type d'association", AssociationType.entries, orgType, { it.label }, { orgType = it })
                AppTextField(regNumber, { regNumber = it }, "Numéro d'agrément / d'enregistrement", maxLength = 60)
                AppTextField(orgPhone, { orgPhone = it }, "Téléphone de l'association", keyboardType = KeyboardType.Phone, maxLength = 30)
                InfoBanner(
                    text = "Un médiateur vérifiera votre dossier. En attendant, vous pouvez utiliser l'application ; les cagnottes et les publications s'ouvrent après certification.",
                    kind = BannerKind.INFO
                )
            }

            DropdownField("Ville", Cities.names, city, { it }, { city = it })
            AppTextField(neighborhood, { neighborhood = it }, "Quartier", placeholder = "Ex. Akwa, Bastos, Ndokoti", maxLength = 100)
            AppTextField(
                landmark, { landmark = it }, "Repère connu (lieu de rencontre)",
                placeholder = "Ex. Carrefour Salle des Fêtes, pharmacie", maxLength = 150,
                supportingText = "Seul ce repère approximatif est montré aux autres, jamais votre adresse."
            )
            UseMyLocationButton(
                onLocation = { a, b -> lat = a; lng = b; locationMsg = "Position enregistrée (approximative)." },
                onUnavailable = { locationMsg = "Position indisponible : la ville choisie sera utilisée." }
            )
            locationMsg?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }

            if (adminMode) {
                AppTextField(setupCode, { setupCode = it }, "Code de configuration", password = true, maxLength = 80)
            }

            error?.let { InfoBanner(it, kind = BannerKind.DANGER) }

            PrimaryAction(
                text = if (adminMode) "Créer l'administrateur" else "Terminer l'inscription",
                enabled = !busy,
                onClick = {
                    val coords = if (lat != 0.0 || lng != 0.0) Pair(lat, lng) else Cities.find(city).let { Pair(it.lat, it.lng) }
                    error = when {
                        name.trim().length < 2 -> "Indiquez votre nom."
                        neighborhood.isBlank() -> "Indiquez votre quartier."
                        isAssociation && orgName.trim().length < 2 -> "Indiquez le nom de l'association."
                        isAssociation && regNumber.trim().length < 3 -> "Indiquez le numéro d'agrément."
                        adminMode && setupCode.isBlank() -> "Saisissez le code de configuration."
                        else -> null
                    }
                    if (error == null) {
                        val form = OnboardingForm(
                            name = name, role = if (adminMode) UserRole.MEDIATEUR else role,
                            city = city, neighborhood = neighborhood, landmark = landmark,
                            lat = coords.first, lng = coords.second,
                            associationName = orgName, associationType = orgType.name,
                            associationRegNumber = regNumber, associationPhone = orgPhone
                        )
                        if (adminMode) vm.completeAdminSetup(form, setupCode) else vm.completeProfile(form)
                    }
                }
            )
            TextButton(onClick = { vm.setAdminSetupMode(false); vm.signOut() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Utiliser un autre compte", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun RoleCard(title: String, subtitle: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) ForestGreenPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) ForestGreenPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = if (selected) ForestGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 15.sp)
        }
    }
}
