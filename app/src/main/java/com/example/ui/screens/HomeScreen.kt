package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.ExchangeType
import com.example.ui.Screen
import com.example.ui.TrocViewModel
import com.example.ui.ViewMode
import com.example.ui.components.GrainsBadge
import com.example.ui.components.InteractiveNeighborhoodMap
import com.example.ui.components.ItemCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.DonGreen
import com.example.ui.theme.ForestGreenDark
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.TrocAmber
import androidx.compose.foundation.layout.statusBarsPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TrocViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val exchangeFilter by viewModel.exchangeFilter.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val radiusKm by viewModel.radiusKm.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val mapSelectedItem by viewModel.mapSelectedItem.collectAsState()

    var activeMainSection by remember { mutableIntStateOf(0) }
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Bar Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ForestGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SyncAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "TrocVoisins CM",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ForestGreenPrimary
                                )
                            }
                        }

                        // Graines Badge + Theme Toggle + Profile/Auth button
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GrainsBadge(
                                grains = currentUser?.solidarityGrains ?: 45,
                                modifier = Modifier.clickable { viewModel.navigateTo(Screen.PROFILE) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.toggleThemeMode() },
                                modifier = Modifier.testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Brightness6,
                                    contentDescription = "Changer de thème",
                                    tint = ForestGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.navigateTo(Screen.AUTH) },
                                modifier = Modifier.testTag("auth_nav_button")
                            ) {
                                Box {
                                    UserAvatar(
                                        photoUrl = currentUser?.photoUrl ?: firebaseUser?.photoUrl?.toString(),
                                        initials = currentUser?.avatarInitials ?: "AK",
                                        size = 34.dp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isUserLoggedIn) DonGreen else TrocAmber)
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                            }
                        }
                    }

                    // Navigation Shortcuts Row
                    ScrollableTabRow(
                        selectedTabIndex = activeMainSection,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = ForestGreenPrimary
                    ) {
                        Tab(selected = activeMainSection == 0, onClick = { activeMainSection = 0 }, text = { Text("Dons & Trocs", fontSize = 12.5.sp, fontWeight = FontWeight.Bold) })
                        Tab(selected = activeMainSection == 1, onClick = { viewModel.navigateTo(Screen.FUNDRAISERS) }, text = { Text("Cagnottes 💰", fontSize = 12.5.sp, fontWeight = FontWeight.Bold) })
                        Tab(selected = activeMainSection == 2, onClick = { viewModel.navigateTo(Screen.COLLECTION_POINTS) }, text = { Text("Permanences 📍", fontSize = 12.5.sp, fontWeight = FontWeight.Bold) })
                        Tab(selected = activeMainSection == 3, onClick = { viewModel.navigateTo(Screen.ADMIN_MEDIATION) }, text = { Text("Médiation ⚖️", fontSize = 12.5.sp, fontWeight = FontWeight.Bold) })
                    }

                    // Search & Filter Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Chercher...", fontSize = 12.5.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("search_text_field")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.Place, contentDescription = "Filtres")
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.PUBLISH) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Donner / Troquer") },
                containerColor = ForestGreenPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Categories
            LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(Category.values()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(category.label) }
                    )
                }
            }
            
            // List / Map Content
            if (viewMode == ViewMode.MAP) {
                InteractiveNeighborhoodMap(items = items, selectedRadiusKm = radiusKm, onRadiusChange = { viewModel.setRadiusKm(it) }, selectedItem = mapSelectedItem, onSelectItem = { viewModel.selectMapItem(it) }, onOpenItemDetail = { viewModel.openItemDetail(it) })
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.id }) { item -> ItemCard(item = item, onClick = { viewModel.openItemDetail(item.id) }) }
                }
            }
        }
        
        if (showFilterSheet) {
            ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Filtrer", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ville")
                    val cities = listOf("Toutes les villes", "Douala", "Yaoundé", "Bafoussam")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(cities) { city ->
                            FilterChip(selected = selectedCity == city, onClick = { viewModel.setSelectedCity(city) }, label = { Text(city) })
                        }
                    }
                    Text("Type", modifier = Modifier.padding(top = 16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = exchangeFilter == null, onClick = { viewModel.setExchangeFilter(null) }, label = { Text("Tout") })
                        FilterChip(selected = exchangeFilter == ExchangeType.DON, onClick = { viewModel.setExchangeFilter(ExchangeType.DON) }, label = { Text("Don") })
                        FilterChip(selected = exchangeFilter == ExchangeType.TROC, onClick = { viewModel.setExchangeFilter(ExchangeType.TROC) }, label = { Text("Troc") })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.setViewMode(if (viewMode == ViewMode.LIST) ViewMode.MAP else ViewMode.LIST)
                            showFilterSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Basculer Liste/Carte")
                    }
                }
            }
        }
    }
}
