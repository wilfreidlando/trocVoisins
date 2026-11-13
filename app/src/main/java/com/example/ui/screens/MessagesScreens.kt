package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.ui.AppViewModel
import com.example.ui.Route
import com.example.ui.components.AppCard
import com.example.ui.components.Avatar
import com.example.ui.components.EmptyState
import com.example.ui.components.LoadingBox
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.TabScaffold
import com.example.ui.theme.ForestGreenPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatTime(ms: Long): String {
    if (ms <= 0) return ""
    val now = System.currentTimeMillis()
    val pattern = if (now - ms < 24L * 3600 * 1000) "HH:mm" else "dd/MM HH:mm"
    return SimpleDateFormat(pattern, Locale.FRANCE).format(Date(ms))
}

@Composable
fun MessagesScreen(vm: AppViewModel) {
    val threads by vm.threads.collectAsState()
    val profile by vm.profile.collectAsState()
    val me = profile ?: return

    TabScaffold(title = "Messages") { padding ->
        if (threads.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    Icons.Default.ChatBubbleOutline, "Aucune conversation",
                    "Une discussion s'ouvre automatiquement quand vous demandez un objet ou qu'on vous en demande un.",
                    actionLabel = "Voir les annonces", onAction = { vm.openTab(Route.Home) }
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(threads, key = { it.id }) { t ->
                    val other = t.otherName(me.id)
                    val unread = t.lastSenderId.isNotBlank() && t.lastSenderId != me.id
                    AppCard(onClick = { vm.push(Route.Chat(t.id)) }) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(other.take(2).uppercase(), "", size = 44.dp)
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(other, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text(formatTime(t.lastMessageAtMs), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(t.itemTitle, fontSize = 12.sp, color = ForestGreenPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    t.lastMessage, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (unread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(vm: AppViewModel, threadId: String) {
    val profile by vm.profile.collectAsState()
    val thread by vm.threadFlow(threadId).collectAsState(initial = null)
    val messages by vm.messagesFlow(threadId).collectAsState(initial = emptyList())
    val me = profile ?: return
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    ScreenScaffold(
        title = thread?.otherName(me.id) ?: "Discussion",
        onBack = { vm.back() },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it.take(1000) },
                        placeholder = { Text("Votre message…") },
                        shape = RoundedCornerShape(24.dp), maxLines = 4,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { thread?.let { vm.sendMessage(it, text) }; text = "" },
                        enabled = text.isNotBlank() && thread != null
                    ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Envoyer", tint = ForestGreenPrimary) }
                }
            }
        }
    ) { padding ->
        val t = thread
        if (t == null) { LoadingBox(Modifier.padding(padding)); return@ScreenScaffold }
        Column(Modifier.fillMaxSize().padding(padding)) {
            AppCard(onClick = { vm.push(Route.ItemDetail(t.itemId)) }, modifier = Modifier.padding(12.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("À propos de", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(t.itemTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("Voir l'annonce", fontSize = 12.sp, color = ForestGreenPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { m -> MessageBubble(m, mine = m.senderId == me.id) }
            }
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMessage, mine: Boolean) {
    if (m.isSystem) {
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    m.text, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (mine) 16.dp else 4.dp, bottomEnd = if (mine) 4.dp else 16.dp),
                color = if (mine) ForestGreenPrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 290.dp)
            ) {
                Text(
                    m.text, fontSize = 14.sp, lineHeight = 20.sp,
                    color = if (mine) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Text(formatTime(m.createdAtMs), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
    }
}
