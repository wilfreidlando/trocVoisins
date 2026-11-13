package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.DisputeStatus
import com.example.data.model.FundraiserStatus
import com.example.data.model.ItemStatus
import com.example.data.model.RequestStatus
import com.example.data.model.UserProfile
import com.example.firebase.configureAppCheck
import com.example.ui.AppViewModel
import com.example.ui.Route
import com.example.ui.Session
import com.example.ui.TabSpec
import com.example.ui.Tabs
import com.example.ui.ThemeMode
import com.example.ui.components.EmptyState
import com.example.ui.components.LoadingBox
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminDisputesScreen
import com.example.ui.screens.AdminValidateScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.EditLocationScreen
import com.example.ui.screens.ExchangesScreen
import com.example.ui.screens.FundraiserCreateScreen
import com.example.ui.screens.FundraiserDetailScreen
import com.example.ui.screens.HelpScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ItemDetailScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PointCreateScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.PublicProfileScreen
import com.example.ui.screens.PublishScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SolidarityScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.ForestGreenPrimary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        // Notifications Android 13+ : demandées une fois, sans bloquer l'application.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val dark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }
            MyApplicationTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TrocApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun TrocApp(vm: AppViewModel) {
    if (!vm.backendAvailable) {
        EmptyState(
            androidx.compose.material.icons.Icons.Default.Info,
            "Configuration Firebase manquante",
            "L'application ne peut pas joindre Firebase. Vérifiez le fichier google-services.json et le projet Firebase."
        )
        return
    }

    val session by vm.session.collectAsState()
    val busy by vm.busy.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("busy_bar"), color = ForestGreenPrimary) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = session) {
                Session.Loading -> LoadingBox()
                Session.SignedOut -> SignInScreen(vm)
                is Session.NeedsProfile -> ProfileSetupScreen(vm, s.user)
                is Session.Active -> MainShell(vm, s.profile)
            }
        }
    }
}

@Composable
private fun MainShell(vm: AppViewModel, profile: UserProfile) {
    val stack by vm.stack.collectAsState()
    val route = stack.last()
    val tabs = remember(profile.role) { Tabs.forUser(profile) }

    // À l'arrivée (ou au changement de rôle), on se place sur l'onglet de départ du rôle.
    LaunchedEffect(profile.role) {
        val first = stack.first()
        if (tabs.none { it.route == first }) vm.resetNavigation()
    }

    Scaffold(
        bottomBar = { if (Tabs.isTab(route)) BottomBar(vm, tabs, route) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(targetState = route, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "route") { r ->
                RouteContent(vm, r)
            }
        }
    }
}

@Composable
private fun RouteContent(vm: AppViewModel, route: Route) {
    when (route) {
        Route.Home -> HomeScreen(vm, asTab = true)
        Route.Browse -> HomeScreen(vm, asTab = false)
        Route.Solidarity -> SolidarityScreen(vm)
        Route.Exchanges -> ExchangesScreen(vm)
        Route.Messages -> MessagesScreen(vm)
        Route.Profile -> ProfileScreen(vm)
        Route.AdminDashboard -> AdminDashboardScreen(vm)
        Route.AdminValidate -> AdminValidateScreen(vm)
        Route.AdminDisputes -> AdminDisputesScreen(vm)
        is Route.ItemDetail -> ItemDetailScreen(vm, route.id)
        Route.Publish -> PublishScreen(vm)
        is Route.Chat -> ChatScreen(vm, route.threadId)
        Route.Notifications -> NotificationsScreen(vm)
        Route.Help -> HelpScreen(vm)
        is Route.FundraiserDetail -> FundraiserDetailScreen(vm, route.id)
        Route.FundraiserCreate -> FundraiserCreateScreen(vm)
        Route.PointCreate -> PointCreateScreen(vm)
        is Route.PublicProfile -> PublicProfileScreen(vm, route.uid)
        Route.Wallet -> WalletScreen(vm)
        Route.EditLocation -> EditLocationScreen(vm)
    }
}

@Composable
private fun BottomBar(vm: AppViewModel, tabs: List<TabSpec>, current: Route) {
    val threads by vm.threads.collectAsState()
    val incoming by vm.incomingRequests.collectAsState()
    val items by vm.items.collectAsState()
    val disputes by vm.disputes.collectAsState()
    val funds by vm.fundraisers.collectAsState()
    val assos by vm.pendingAssociations.collectAsState()
    val profile by vm.profile.collectAsState()
    val me = profile?.id.orEmpty()

    fun badge(key: String): Int = when (key) {
        "messages" -> threads.count { it.lastSenderId.isNotBlank() && it.lastSenderId != me }
        "exchanges" -> incoming.count { it.statusEnum == RequestStatus.PENDING } +
                items.count {
                    it.statusEnum == ItemStatus.RESERVED &&
                            ((it.donorId == me && !it.donorConfirmed) || (it.reservedByUserId == me && !it.receiverConfirmed))
                }
        "validate" -> items.count { it.statusEnum == ItemStatus.PENDING_MODERATION } +
                funds.count { it.statusEnum == FundraiserStatus.PENDING_REVIEW } + assos.size
        "disputes" -> disputes.count { it.statusEnum == DisputeStatus.OPEN }
        else -> 0
    }

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp, modifier = Modifier.testTag("bottom_navigation_bar")) {
        tabs.forEach { tab ->
            val count = if (tab.badgeKey.isNotEmpty()) badge(tab.badgeKey) else 0
            NavigationBarItem(
                selected = current == tab.route,
                onClick = { vm.openTab(tab.route) },
                icon = {
                    BadgedBox(badge = { if (count > 0) Badge { Text(if (count > 9) "9+" else count.toString()) } }) {
                        Icon(tab.icon, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ForestGreenPrimary,
                    selectedTextColor = ForestGreenPrimary,
                    indicatorColor = ForestGreenPrimary.copy(alpha = 0.15f)
                )
            )
        }
    }
}
