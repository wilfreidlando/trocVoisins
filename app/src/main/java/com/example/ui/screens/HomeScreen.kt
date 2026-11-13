package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssociationStatus
import com.example.data.model.Category
import com.example.data.model.ExchangeType
import com.example.data.model.Item
import com.example.ui.AppViewModel
import com.example.ui.HomeViewMode
import com.example.ui.Route
import com.example.ui.components.BannerKind
import com.example.ui.components.EmptyState
import com.example.ui.components.InfoBanner
import com.example.ui.components.InteractiveNeighborhoodMap
import com.example.ui.components.ItemCard
import com.example.ui.components.ScreenScaffold
import com.example.ui.components.TabScaffold
import com.example.ui.theme.ForestGreenPrimary

private val radiusOptions: List<Pair<String, Float?>> = listOf(
    "1 km" to 1f, "3 km" to 3f, "5 km" to 5f, "10 km" to 10f, "25 km" to 25f, "Toute la ville" to null
)

/** Accueil : annonces proches, avec recherche, filtres, liste ou carte. `asTab` = faux quand le médiateur le consulte. */
@Composable
fun HomeScreen(vm: AppViewModel, asTab: Boolean = true) {
    val profile by vm.profile.collectAsState()
    val items by vm.homeItems.collectAsState()
    val filters by vm.filters.collectAsState()
    val mode by vm.viewMode.collectAsState()
    val notifications by vm.notifications.collectAsState()
    var selected by remember { mutableStateOf<Item?>(null) }

    val me = profile ?: return
    val unread = notifications.count { !it.read }
    val canPublish = !me.isMediator && (!me.isAssociation || me.associationStatus == AssociationStatus.CERTIFIED.name)

    val actions: @Composable () -> Unit = {
        IconButton(onClick = { vm.push(Route.Notifications) }, modifier = Modifier.testTag("notifications_button")) {
            BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 9) "9+" else unread.toString()) } }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications")
            }
        }
        IconButton(onClick = { vm.push(Route.Help) }) { Icon(Icons.Default.HelpOutline, contentDescription = "Aide") }
    }
    val fab: @Composable () -> Unit = {
        if (canPublish) {
            ExtendedFloatingActionButton(
                onClick = { vm.push(Route.Publish) },
                containerColor = ForestGreenPrimary,
                contentColor = androidx.compose.ui.graphics.Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Donner / Troquer", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("publish_fab")
            )
        }
    }

    val body: @Composable (PaddingValues) -> Unit = { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = filters.query,
                onValueChange = { vm.setFilters(filters.copy(query = it.take(60))) },
                placeholder = { Text("Rechercher un objet, un quartier…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setFilters(filters.copy(query = "")) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer la recherche")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("search_field")
            )

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Category.entries) { c ->
                    FilterChip(
                        selected = filters.category == c,
                        onClick = { vm.setFilters(filters.copy(category = c)) },
                        label = { Text(c.label, fontSize = 12.5.sp) },
                        leadingIcon = { Icon(c.icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                    val options = listOf<ExchangeType?>(null, ExchangeType.DON, ExchangeType.TROC)
                    val labels = listOf("Tout", "Dons", "Trocs")
                    options.forEachIndexed { i, opt ->
                        SegmentedButton(
                            selected = filters.exchange == opt,
                            onClick = { vm.setFilters(filters.copy(exchange = opt)) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size)
                        ) { Text(labels[i], fontSize = 12.sp) }
                    }
                }
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = mode == HomeViewMode.LIST, onClick = { vm.setViewMode(HomeViewMode.LIST) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        icon = {}
                    ) { Icon(Icons.Default.List, contentDescription = "Liste", modifier = Modifier.size(18.dp)) }
                    SegmentedButton(
                        selected = mode == HomeViewMode.MAP, onClick = { vm.setViewMode(HomeViewMode.MAP) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        icon = {}
                    ) { Icon(Icons.Default.Map, contentDescription = "Carte", modifier = Modifier.size(18.dp)) }
                }
            }

            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(radiusOptions) { (label, value) ->
                    FilterChip(
                        selected = filters.radiusKm == value,
                        onClick = { vm.setFilters(filters.copy(radiusKm = value)) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = filters.onlyEmergency,
                        onClick = { vm.setFilters(filters.copy(onlyEmergency = !filters.onlyEmergency)) },
                        label = { Text("Urgences", fontSize = 12.sp) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            if (mode == HomeViewMode.MAP) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    InteractiveNeighborhoodMap(
                        items = items,
                        selectedRadiusKm = filters.radiusKm ?: 25f,
                        onRadiusChange = { vm.setFilters(filters.copy(radiusKm = it)) },
                        selectedItem = selected,
                        onSelectItem = { selected = it },
                        onOpenItemDetail = { vm.push(Route.ItemDetail(it)) }
                    )
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (me.isAssociation && me.associationStatus != AssociationStatus.CERTIFIED.name) {
                        item {
                            InfoBanner(
                                title = "Certification en cours",
                                text = if (me.associationStatus == AssociationStatus.REJECTED.name)
                                    "Votre demande a été refusée. Ouvrez votre profil pour corriger vos informations."
                                else "Un médiateur examine votre dossier. Vous pourrez publier et lancer des cagnottes dès qu'il sera validé.",
                                kind = BannerKind.WARNING
                            )
                        }
                    }
                    if (items.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Inventory2,
                                title = "Rien à cette distance",
                                message = if (filters.query.isNotBlank() || filters.category != Category.TOUT || filters.exchange != null)
                                    "Aucune annonce ne correspond à ces filtres. Essayez d'élargir la recherche."
                                else "Soyez le premier à donner ! Ou élargissez la distance pour voir plus loin.",
                                actionLabel = if (filters.radiusKm != null) "Voir toute la ville" else null,
                                onAction = if (filters.radiusKm != null) ({ vm.setFilters(filters.copy(radiusKm = null)) }) else null
                            )
                        }
                    } else {
                        items(items, key = { it.id }) { item ->
                            ItemCard(item, onClick = { vm.push(Route.ItemDetail(item.id)) })
                        }
                    }
                }
            }
        }
    }

    if (asTab) {
        TabScaffold(title = "Près de chez vous", actions = actions, floatingActionButton = fab, content = body)
    } else {
        ScreenScaffold(title = "Annonces", onBack = { vm.back() }, actions = actions, floatingActionButton = fab, content = body)
    }
}
