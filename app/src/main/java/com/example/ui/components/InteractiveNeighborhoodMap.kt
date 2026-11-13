package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExchangeType
import com.example.data.model.Item
import com.example.ui.theme.DonGreen
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.TerracottaAccent
import com.example.ui.theme.TrocAmber
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun InteractiveNeighborhoodMap(
    items: List<Item>,
    selectedRadiusKm: Float,
    onRadiusChange: (Float) -> Unit,
    selectedItem: Item?,
    onSelectItem: (Item?) -> Unit,
    onOpenItemDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val centerLat = if (items.isNotEmpty()) items[0].approximateLat else 4.0515
    val centerLng = if (items.isNotEmpty()) items[0].approximateLng else 9.7675

    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        // Interactive Canvas Map
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panOffsetX += dragAmount.x
                        panOffsetY += dragAmount.y
                    }
                }
                .pointerInput(items, zoomLevel, panOffsetX, panOffsetY) {
                    detectTapGestures { tapOffset ->
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f + panOffsetX
                        val cy = h / 2f + panOffsetY
                        val scale = 36000f * zoomLevel

                        var hitItem: Item? = null
                        for (item in items) {
                            val px = cx + ((item.approximateLng - centerLng) * scale).toFloat()
                            val py = cy - ((item.approximateLat - centerLat) * scale).toFloat()
                            val dist = hypot((tapOffset.x - px).toDouble(), (tapOffset.y - py).toDouble())
                            if (dist < 32.dp.toPx()) {
                                hitItem = item
                                break
                            }
                        }
                        onSelectItem(hitItem)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val cx = width / 2f + panOffsetX
            val cy = height / 2f + panOffsetY
            val scale = 36000f * zoomLevel

            // Background map canvas color (warm urban paper tone)
            drawRect(color = Color(0xFFF3F5F1))

            // Draw Parks / Green areas
            drawRoundRect(
                color = Color(0xFFDCEAD8),
                topLeft = Offset(cx - 280f * zoomLevel, cy - 220f * zoomLevel),
                size = Size(160f * zoomLevel, 140f * zoomLevel),
                cornerRadius = CornerRadius(20f, 20f)
            )
            drawRoundRect(
                color = Color(0xFFDCEAD8),
                topLeft = Offset(cx + 120f * zoomLevel, cy + 90f * zoomLevel),
                size = Size(200f * zoomLevel, 120f * zoomLevel),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Draw Seine River / Canal curves in distance
            val riverPath = Path().apply {
                moveTo(0f, height * 0.92f + panOffsetY * 0.3f)
                cubicTo(
                    width * 0.3f, height * 0.88f + panOffsetY * 0.3f,
                    width * 0.7f, height * 0.95f + panOffsetY * 0.3f,
                    width, height * 0.90f + panOffsetY * 0.3f
                )
            }
            drawPath(
                path = riverPath,
                color = Color(0xFFC7DFEE),
                style = Stroke(width = 24f * zoomLevel)
            )

            // Draw Urban Street Grid & Avenues
            val streetColor = Color(0xFFFFFFFF)
            val streetStroke = 12f * zoomLevel

            for (i in -4..4) {
                val y = cy + i * 90f * zoomLevel
                drawLine(
                    color = streetColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = streetStroke
                )
                val x = cx + i * 110f * zoomLevel
                drawLine(
                    color = streetColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = streetStroke
                )
            }

            // Diagonal boulevard
            drawLine(
                color = Color(0xFFFFFEFA),
                start = Offset(cx - 400f * zoomLevel, cy + 300f * zoomLevel),
                end = Offset(cx + 400f * zoomLevel, cy - 300f * zoomLevel),
                strokeWidth = 16f * zoomLevel
            )

            // User's location radius circle (e.g. 500m, 2km, 5km)
            val radiusPixels = (selectedRadiusKm * 180f) * zoomLevel
            drawCircle(
                color = ForestGreenPrimary.copy(alpha = 0.08f),
                radius = radiusPixels,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = ForestGreenPrimary.copy(alpha = 0.45f),
                radius = radiusPixels,
                center = Offset(cx, cy),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 12f))
                )
            )

            // User location marker (Pulsing blue center)
            drawCircle(
                color = ForestGreenPrimary.copy(alpha = 0.25f),
                radius = 24.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White,
                radius = 11.dp.toPx(),
                center = Offset(cx, cy)
            )
            drawCircle(
                color = ForestGreenPrimary,
                radius = 8.dp.toPx(),
                center = Offset(cx, cy)
            )

            // If an item is selected: DRAW THE MANDATORY PRIVACY APPROXIMATE ZONE (200m circle)
            selectedItem?.let { item ->
                val px = cx + ((item.approximateLng - centerLng) * scale).toFloat()
                val py = cy - ((item.approximateLat - centerLat) * scale).toFloat()

                val privacyRadiusPx = 65f * zoomLevel // approx 200m radius
                val zoneColor = if (item.exchangeType == ExchangeType.DON.name) DonGreen else TrocAmber

                // Semi-transparent privacy circle
                drawCircle(
                    color = zoneColor.copy(alpha = 0.18f),
                    radius = privacyRadiusPx,
                    center = Offset(px, py)
                )
                // Dashed perimeter outline
                drawCircle(
                    color = zoneColor.copy(alpha = 0.8f),
                    radius = privacyRadiusPx,
                    center = Offset(px, py),
                    style = Stroke(
                        width = 2.5f.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                    )
                )
            }

            // Draw Item Pins or Clusters
            for (item in items) {
                val px = cx + ((item.approximateLng - centerLng) * scale).toFloat()
                val py = cy - ((item.approximateLat - centerLat) * scale).toFloat()

                val isSelected = selectedItem?.id == item.id
                val isDon = item.exchangeType == ExchangeType.DON.name
                val pinColor = if (isDon) DonGreen else TrocAmber
                val pinRadius = if (isSelected) 18.dp.toPx() else 14.dp.toPx()

                // Pin shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.22f),
                    radius = pinRadius + 2.dp.toPx(),
                    center = Offset(px, py + 3.dp.toPx())
                )

                // Pin background
                drawCircle(
                    color = Color.White,
                    radius = pinRadius + 2.dp.toPx(),
                    center = Offset(px, py)
                )
                drawCircle(
                    color = pinColor,
                    radius = pinRadius,
                    center = Offset(px, py)
                )

                // White inner dot or symbol
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        // Top Overlay: Neighborhood, Radius Filter & Privacy Notice
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Privacy banner (highlighting neighborhood respect)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vie privée préservée : zones approximatives (300m au carrefour)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Radius filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.5f to "< 500m", 2.0f to "< 2 km", 5.0f to "< 5 km").forEach { (rad, label) ->
                    val isSelected = selectedRadiusKm == rad
                    FilterChip(
                        selected = isSelected,
                        onClick = { onRadiusChange(rad) },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ForestGreenPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("radius_filter_$rad")
                    )
                }
            }
        }

        // Map control floating buttons (Zoom In, Zoom Out, Recenter)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledIconButton(
                onClick = { zoomLevel = (zoomLevel * 1.3f).coerceAtMost(2.8f) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, CircleShape)
                    .testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom avant")
            }

            FilledIconButton(
                onClick = { zoomLevel = (zoomLevel / 1.3f).coerceAtLeast(0.6f) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, CircleShape)
                    .testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom arrière")
            }

            FilledIconButton(
                onClick = {
                    panOffsetX = 0f
                    panOffsetY = 0f
                    zoomLevel = 1.0f
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = ForestGreenPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, CircleShape)
                    .testTag("recenter_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recentrer mon quartier")
            }
        }

        // Bottom Selected Item Preview Card
        AnimatedVisibility(
            visible = selectedItem != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            selectedItem?.let { item ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_item_preview_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExchangeBadge(exchangeType = item.exchangeType, compact = true)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "à ${item.distanceMeters} m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ForestGreenPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onSelectItem(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Fermer",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (item.exchangeType == ExchangeType.TROC.name)
                                "Recherche : ${item.wantedInReturn}"
                            else item.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(ForestGreenPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.donorName.take(1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreenPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.donorName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = { onOpenItemDetail(item.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                                modifier = Modifier.testTag("view_item_from_map_button")
                            ) {
                                Text("Voir l'annonce", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
