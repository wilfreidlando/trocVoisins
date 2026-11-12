package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExchangeType
import com.example.data.model.ItemStatus
import com.example.ui.theme.DonGreen
import com.example.ui.theme.DonGreenContainer
import com.example.ui.theme.DonGreenText
import com.example.ui.theme.TrocAmber
import com.example.ui.theme.TrocAmberContainer
import com.example.ui.theme.TrocAmberText
import com.example.ui.theme.VerifiedBlue
import com.example.ui.theme.VerifiedBlueContainer

@Composable
fun ExchangeBadge(
    exchangeType: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val isDon = exchangeType == ExchangeType.DON.name
    val bgColor = if (isDon) DonGreenContainer else TrocAmberContainer
    val contentColor = if (isDon) DonGreenText else TrocAmberText
    val borderColor = if (isDon) DonGreen.copy(alpha = 0.35f) else TrocAmber.copy(alpha = 0.35f)
    val icon = if (isDon) Icons.Default.CardGiftcard else Icons.Default.SyncAlt
    val label = if (isDon) {
        if (compact) "Don" else "Don caritatif"
    } else {
        if (compact) "Troc" else "Troc solidaire"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(if (compact) 13.dp else 16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = if (compact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GrainsBadge(
    grains: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF9C4))
            .border(1.dp, Color(0xFFFBC02D), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🌾 $grains",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFE65100)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "Graines",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB78103)
        )
    }
}

@Composable
fun AssociationBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFE3F2FD))
            .border(1.dp, Color(0xFF90CAF9), RoundedCornerShape(6.dp))
            .padding(horizontal = if (compact) 5.dp else 8.dp, vertical = if (compact) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = Color(0xFF1565C0),
            modifier = Modifier.size(if (compact) 12.dp else 14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (compact) "Asso certifiée" else "Association certifiée",
            color = Color(0xFF1565C0),
            fontSize = if (compact) 10.5.sp else 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmergencyBadge(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFFFEBEE))
            .border(1.dp, Color(0xFFEF9A9A), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Emergency,
            contentDescription = null,
            tint = Color(0xFFD32F2F),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "Urgence orphelinat",
            color = Color(0xFFD32F2F),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    when (status) {
        ItemStatus.RESERVED.name -> {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFF8E1))
                    .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFF57F17),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Réservé",
                    color = Color(0xFFE65100),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        ItemStatus.REPORTED.name -> {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFEBEE))
                    .border(1.dp, Color(0xFFE57373), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "En médiation",
                    color = Color(0xFFD32F2F),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        ItemStatus.COMPLETED.name -> {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEEEEEE))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Donné / Transmis",
                    color = Color(0xFF616161),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RatingStars(
    rating: Float,
    reviewCount: Int,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFB300),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = String.format(java.util.Locale.US, "%.1f", rating),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = textColor
        )
        if (reviewCount > 0) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "($reviewCount)",
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}
