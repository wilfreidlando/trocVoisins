package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExchangeType
import com.example.data.model.GrainLevel
import com.example.data.model.ItemStatus
import com.example.ui.theme.DonGreen
import com.example.ui.theme.DonGreenContainer
import com.example.ui.theme.DonGreenText
import com.example.ui.theme.TrocAmber
import com.example.ui.theme.TrocAmberContainer
import com.example.ui.theme.TrocAmberText
import com.example.ui.theme.VerifiedBlue
import com.example.ui.theme.VerifiedBlueContainer

/** Pastille générique : une seule forme pour tous les badges de l'application. */
@Composable
fun Pill(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    border: Color? = null,
    compact: Boolean = false
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .padding(horizontal = if (compact) 6.dp else 9.dp, vertical = if (compact) 3.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(if (compact) 12.dp else 14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = content, fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExchangeBadge(exchangeType: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    val isDon = exchangeType == ExchangeType.DON.name
    Pill(
        text = if (isDon) "Don" else "Troc",
        background = if (isDon) DonGreenContainer else TrocAmberContainer,
        content = if (isDon) DonGreenText else TrocAmberText,
        border = (if (isDon) DonGreen else TrocAmber).copy(alpha = 0.35f),
        icon = if (isDon) Icons.Default.CardGiftcard else Icons.Default.SyncAlt,
        modifier = modifier,
        compact = compact
    )
}

@Composable
fun StatusChip(status: ItemStatus, modifier: Modifier = Modifier) {
    when (status) {
        ItemStatus.AVAILABLE -> Pill(status.label, DonGreenContainer, DonGreenText, modifier, Icons.Default.CheckCircle, compact = true)
        ItemStatus.RESERVED -> Pill(status.label, Color(0xFFFFF8E1), Color(0xFF8D5A00), modifier, Icons.Default.Lock, compact = true)
        ItemStatus.COMPLETED -> Pill(status.label, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, modifier, Icons.Default.CheckCircle, compact = true)
        ItemStatus.PENDING_MODERATION -> Pill(status.label, VerifiedBlueContainer, VerifiedBlue, modifier, Icons.Default.HourglassTop, compact = true)
        ItemStatus.REJECTED, ItemStatus.SUSPENDED -> Pill(status.label, Color(0xFFFFEBEE), Color(0xFFB3261E), modifier, Icons.Default.Block, compact = true)
    }
}

@Composable
fun GrainsPill(grains: Int, modifier: Modifier = Modifier) {
    Pill(
        text = "🌾 $grains",
        background = Color(0xFFFFF4C2),
        content = Color(0xFF7A4B00),
        border = Color(0xFFE9C46A),
        modifier = modifier
    )
}

@Composable
fun LevelBadge(level: GrainLevel, modifier: Modifier = Modifier) {
    Pill(
        text = "${level.emoji} ${level.label}",
        background = DonGreenContainer,
        content = DonGreenText,
        modifier = modifier,
        compact = true
    )
}

@Composable
fun CertifiedBadge(modifier: Modifier = Modifier, compact: Boolean = true) {
    Pill(
        text = "Association certifiée",
        background = VerifiedBlueContainer,
        content = VerifiedBlue,
        icon = Icons.Default.VerifiedUser,
        modifier = modifier,
        compact = compact
    )
}

@Composable
fun EmergencyBadge(modifier: Modifier = Modifier) {
    Pill(
        text = "Urgent",
        background = Color(0xFFFFEBEE),
        content = Color(0xFFB3261E),
        icon = Icons.Default.Emergency,
        modifier = modifier,
        compact = true
    )
}

@Composable
fun RatingStars(rating: Double, reviewCount: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(3.dp))
        if (reviewCount == 0) {
            Text("Nouveau", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                String.format(java.util.Locale.US, "%.1f", rating),
                fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(3.dp))
            Text("($reviewCount)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
