package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.Screen
import com.example.ui.TrocViewModel
import com.example.ui.theme.DonGreen
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.TrocAmber
import com.example.ui.components.UserAvatar
import com.example.ui.components.RatingStars

@Composable
fun ProfileInfoContent(
    viewModel: TrocViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // User Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(
                    photoUrl = user?.photoUrl ?: firebaseUser?.photoUrl?.toString(),
                    initials = user?.avatarInitials ?: "AK",
                    size = 68.dp,
                    fontSize = 24
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = user?.name ?: "Amadou Kamga",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Role pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ForestGreenPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = user?.role ?: UserRole.CITOYEN.name,
                        color = ForestGreenPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                RatingStars(
                    rating = user?.rating ?: 4.9f,
                    reviewCount = user?.reviewCount ?: 18
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Portefeuille de Graines de Solidarité
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mon Solde de Graines",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB78103)
                    )
                    Text(
                        text = "${user?.solidarityGrains ?: 45} 🌾 Graines",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE65100)
                    )
                }

                Button(
                    onClick = { viewModel.navigateTo(Screen.FUNDRAISERS) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reverser", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Impact Stats
        Text(text = "Mon impact social & environnemental", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatPill(title = "Dons offerts", value = "${user?.giftsGiven ?: 12}", icon = Icons.Default.CardGiftcard, accent = DonGreen, modifier = Modifier.weight(1f))
            StatPill(title = "Trocs réussis", value = "${user?.trocsCompleted ?: 7}", icon = Icons.Default.SyncAlt, accent = TrocAmber, modifier = Modifier.weight(1f))
            StatPill(title = "CO₂ évité", value = "${user?.co2SavedKg?.toInt() ?: 46} kg", icon = Icons.Default.Recycling, accent = ForestGreenPrimary, modifier = Modifier.weight(1f))
        }
        // Theme Mode Selector Card (Light / Dark / System)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "Apparence & Thème", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                val currentThemeMode by viewModel.themeMode.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentThemeMode == com.example.ui.ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(com.example.ui.ThemeMode.LIGHT) },
                        label = { Text("☀️ Clair", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("theme_light_chip")
                    )
                    FilterChip(
                        selected = currentThemeMode == com.example.ui.ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(com.example.ui.ThemeMode.DARK) },
                        label = { Text("🌙 Sombre", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("theme_dark_chip")
                    )
                    FilterChip(
                        selected = currentThemeMode == com.example.ui.ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(com.example.ui.ThemeMode.SYSTEM) },
                        label = { Text("⚙️ Système", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("theme_system_chip")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Administration & Médiation shortcut (accessible to admins only)
        if (user?.isAdmin == true || user?.role == "ADMIN_MEDIATEUR") {
            OutlinedButton(
                onClick = { viewModel.navigateTo(Screen.ADMIN_MEDIATION) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFFD32F2F))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Accéder au Centre de Médiation", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Avis reçus
        val reviews by viewModel.reviews.collectAsState()
        Text(text = "Avis & témoignages reçus (${reviews.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        reviews.forEach { rev ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = rev.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = rev.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    RatingStars(rating = rev.rating.toFloat(), reviewCount = 0)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "\"${rev.comment}\"", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatPill(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = accent)
            Text(text = title, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
