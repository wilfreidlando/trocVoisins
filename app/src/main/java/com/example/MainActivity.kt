package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firebase.configureAppCheck
import com.example.ui.Screen
import com.example.ui.TrocViewModel
import com.example.ui.components.ReviewDialog
import com.example.ui.screens.AdminMediationScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CollectionPointsScreen
import com.example.ui.screens.FundraisersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.LocationSettingsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PublishItemScreen
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TrocViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Step 1: Configure Firebase App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                com.example.ui.ThemeMode.LIGHT -> false
                com.example.ui.ThemeMode.DARK -> true
                com.example.ui.ThemeMode.SYSTEM -> systemDark
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TrocApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun TrocApp(viewModel: TrocViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showReviewDialog by remember { mutableStateOf(false) }
    var reviewTargetUserId by remember { mutableStateOf("") }
    var reviewTargetUserName by remember { mutableStateOf("") }

    val mainScreens = listOf(
        Screen.HOME,
        Screen.FUNDRAISERS,
        Screen.COLLECTION_POINTS,
        Screen.PROFILE,
        Screen.ADMIN_MEDIATION
    )

    val isMainScreen = currentScreen in mainScreens

    Scaffold(
        bottomBar = {
            if (isMainScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("bottom_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.HOME,
                        onClick = { viewModel.navigateTo(Screen.HOME) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Accueil") },
                        label = { Text("Accueil", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreenPrimary,
                            selectedTextColor = ForestGreenPrimary,
                            indicatorColor = ForestGreenPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.FUNDRAISERS,
                        onClick = { viewModel.navigateTo(Screen.FUNDRAISERS) },
                        icon = { Icon(Icons.Default.VolunteerActivism, contentDescription = "Cagnottes") },
                        label = { Text("Cagnottes", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreenPrimary,
                            selectedTextColor = ForestGreenPrimary,
                            indicatorColor = ForestGreenPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_fundraisers")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.COLLECTION_POINTS,
                        onClick = { viewModel.navigateTo(Screen.COLLECTION_POINTS) },
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = "Relais") },
                        label = { Text("Relais", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreenPrimary,
                            selectedTextColor = ForestGreenPrimary,
                            indicatorColor = ForestGreenPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_relais")
                    )

                    NavigationBarItem(
                        selected = currentScreen == Screen.PROFILE,
                        onClick = { viewModel.navigateTo(Screen.PROFILE) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                        label = { Text("Profil", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ForestGreenPrimary,
                            selectedTextColor = ForestGreenPrimary,
                            indicatorColor = ForestGreenPrimary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_profile")
                    )

                    if (currentUser?.isAdmin == true || currentUser?.role == "ADMIN_MEDIATEUR") {
                        NavigationBarItem(
                            selected = currentScreen == Screen.ADMIN_MEDIATION,
                            onClick = { viewModel.navigateTo(Screen.ADMIN_MEDIATION) },
                            icon = { Icon(Icons.Default.Gavel, contentDescription = "Médiation") },
                            label = { Text("Médiation", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ForestGreenPrimary,
                                selectedTextColor = ForestGreenPrimary,
                                indicatorColor = ForestGreenPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_tab_admin")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(viewModel = viewModel)
                    Screen.DETAIL -> ItemDetailScreen(viewModel = viewModel)
                    Screen.PUBLISH -> PublishItemScreen(viewModel = viewModel)
                    Screen.CHAT -> ChatScreen(
                        viewModel = viewModel,
                        onShowReviewDialog = { id, name ->
                            reviewTargetUserId = id
                            reviewTargetUserName = name
                            showReviewDialog = true
                        }
                    )
                    Screen.PROFILE -> ProfileScreen(viewModel = viewModel)
                    Screen.LOCATION_SETTINGS -> LocationSettingsScreen(viewModel = viewModel)
                    Screen.AUTH -> AuthScreen(viewModel = viewModel)
                    Screen.FUNDRAISERS -> FundraisersScreen(viewModel = viewModel)
                    Screen.COLLECTION_POINTS -> CollectionPointsScreen(viewModel = viewModel)
                    Screen.ADMIN_MEDIATION -> AdminMediationScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showReviewDialog) {
        ReviewDialog(
            targetUserId = reviewTargetUserId,
            targetUserName = reviewTargetUserName,
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating: Int, badge: String, comment: String ->
                viewModel.submitReview(reviewTargetUserId, rating, badge, comment)
                showReviewDialog = false
            }
        )
    }
}
