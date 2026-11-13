package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Category
import com.example.data.model.Item
import com.example.data.model.ItemStatus
import com.example.ui.theme.DonGreen
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.TrocAmber
import com.example.util.ImageUtils
import com.example.util.LocationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ------------------------------------------------------------------------------------------
//  Structure d'écran
// ------------------------------------------------------------------------------------------

/** Écran empilé avec barre supérieure standard (retour + titre + actions). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        modifier = modifier,
        content = content
    )
}

/** Écran racine d'un onglet : titre simple, sans bouton retour. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = floatingActionButton,
        modifier = modifier,
        content = content
    )
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier, label: String = "Chargement…") {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ForestGreenPrimary)
            Spacer(Modifier.height(12.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(ForestGreenPrimary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(32.dp)) }
        Spacer(Modifier.height(14.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 19.sp)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction, shape = RoundedCornerShape(12.dp)) { Text(actionLabel) }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        trailing()
    }
}

enum class BannerKind { INFO, WARNING, SUCCESS, DANGER }

@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier, kind: BannerKind = BannerKind.INFO, title: String? = null) {
    val (bg, fg) = when (kind) {
        BannerKind.INFO -> Pair(Color(0xFFE8F1FB), Color(0xFF0B4F8A))
        BannerKind.WARNING -> Pair(Color(0xFFFFF4E0), Color(0xFF7A4B00))
        BannerKind.SUCCESS -> Pair(Color(0xFFE6F4EA), Color(0xFF1B5E20))
        BannerKind.DANGER -> Pair(Color(0xFFFDECEA), Color(0xFF8C1D18))
    }
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(bg).padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            if (kind == BannerKind.WARNING || kind == BannerKind.DANGER) Icons.Default.Warning else Icons.Default.Info,
            contentDescription = null, tint = fg, modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            if (title != null) Text(title, color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(text, color = fg, fontSize = 12.5.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val cardModifier = if (onClick != null) modifier.fillMaxWidth().clickable(onClick = onClick) else modifier.fillMaxWidth()
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = cardModifier
    ) { content() }
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tag: String = ""
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth().height(50.dp).let { if (tag.isNotEmpty()) it.testTag(tag) else it }
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

@Composable
fun SecondaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth().height(48.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun SmallTonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    FilledTonalButton(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SmallTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
}

// ------------------------------------------------------------------------------------------
//  Images et avatars
// ------------------------------------------------------------------------------------------

private object BitmapCache {
    private val cache = object : android.util.LruCache<String, Bitmap>(24) {}
    fun get(key: String): Bitmap? = cache.get(key)
    fun put(key: String, value: Bitmap) { cache.put(key, value) }
}

@Composable
fun rememberBase64Bitmap(b64: String): Bitmap? {
    val state by produceState<Bitmap?>(initialValue = null, b64) {
        if (b64.isBlank()) { value = null; return@produceState }
        val key = b64.length.toString() + b64.takeLast(32)
        BitmapCache.get(key)?.let { value = it; return@produceState }
        val decoded = withContext(Dispatchers.Default) { ImageUtils.fromBase64(b64) }
        if (decoded != null) BitmapCache.put(key, decoded)
        value = decoded
    }
    return state
}

/** Photo d'annonce, ou pictogramme de la catégorie si aucune photo n'a été ajoutée. */
@Composable
fun ItemImage(item: Item, modifier: Modifier = Modifier, iconSize: Dp = 36.dp) {
    val bitmap = rememberBase64Bitmap(item.imageB64)
    val tint = if (item.isTroc) TrocAmber else DonGreen
    Box(modifier.background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(item.categoryEnum.icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
fun Avatar(initials: String, photoUrl: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    if (photoUrl.isNotBlank()) {
        AsyncImage(
            model = photoUrl, contentDescription = "Photo de profil", contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape)
        )
    } else {
        Box(
            modifier.size(size).clip(CircleShape).background(ForestGreenPrimary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = ForestGreenPrimary, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp)
        }
    }
}

// ------------------------------------------------------------------------------------------
//  Carte d'annonce
// ------------------------------------------------------------------------------------------

@Composable
fun ItemCard(item: Item, onClick: () -> Unit, modifier: Modifier = Modifier, showStatus: Boolean = false) {
    AppCard(modifier = modifier.testTag("item_card_${item.id}"), onClick = onClick) {
        Row(Modifier.padding(10.dp)) {
            ItemImage(item, Modifier.size(92.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExchangeBadge(item.exchangeType, compact = true)
                    if (item.isEmergencyResponse) EmergencyBadge()
                    if (showStatus) StatusChip(item.statusEnum)
                }
                Spacer(Modifier.height(5.dp))
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(
                    if (item.isTroc && item.wantedInReturn.isNotBlank()) "Cherche : ${item.wantedInReturn}" else item.description,
                    fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        buildString {
                            append(item.neighborhood.ifBlank { item.city })
                            if (item.distanceMeters > 0) append(" • ").append(LocationUtils.formatDistance(item.distanceMeters))
                        },
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.donorCertified) {
                        Spacer(Modifier.width(6.dp))
                        CertifiedBadge()
                    }
                }
            }
        }
    }
}

fun Category.shortLabel(): String = label
fun statusLabel(status: ItemStatus): String = status.label

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth().height(1.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)) {}
}
