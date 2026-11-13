package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.UserProfile
import com.example.data.model.UserRole

/** Écrans de l'application. Les « onglets » sont les racines de la barre du bas ; les autres s'empilent. */
sealed interface Route {
    // Onglets
    data object Home : Route
    data object Solidarity : Route
    data object Exchanges : Route
    data object Messages : Route
    data object Profile : Route
    data object AdminDashboard : Route
    data object AdminValidate : Route
    data object AdminDisputes : Route

    // Écrans empilés
    data class ItemDetail(val id: String) : Route
    data object Publish : Route
    data class Chat(val threadId: String) : Route
    data object Notifications : Route
    data object Help : Route
    data class FundraiserDetail(val id: String) : Route
    data object FundraiserCreate : Route
    data object PointCreate : Route
    data class PublicProfile(val uid: String) : Route
    data object Wallet : Route
    data object EditLocation : Route
    data object Browse : Route
}

data class TabSpec(val route: Route, val label: String, val icon: ImageVector, val badgeKey: String = "")

object Tabs {
    private val home = TabSpec(Route.Home, "Accueil", Icons.Default.Home)
    private val solidarity = TabSpec(Route.Solidarity, "Solidarité", Icons.Default.VolunteerActivism)
    private val exchanges = TabSpec(Route.Exchanges, "Échanges", Icons.Default.SwapHoriz, "exchanges")
    private val messages = TabSpec(Route.Messages, "Messages", Icons.Default.Chat, "messages")
    private val profile = TabSpec(Route.Profile, "Profil", Icons.Default.Person)

    private val dashboard = TabSpec(Route.AdminDashboard, "Tableau", Icons.Default.Dashboard)
    private val validate = TabSpec(Route.AdminValidate, "À valider", Icons.Default.TaskAlt, "validate")
    private val disputes = TabSpec(Route.AdminDisputes, "Litiges", Icons.Default.Gavel, "disputes")

    fun forUser(user: UserProfile): List<TabSpec> = when (user.roleEnum) {
        UserRole.MEDIATEUR -> listOf(dashboard, validate, disputes, profile)
        else -> listOf(home, solidarity, exchanges, messages, profile)
    }

    fun startRoute(user: UserProfile): Route = forUser(user).first().route

    fun isTab(route: Route): Boolean = route in listOf(
        Route.Home, Route.Solidarity, Route.Exchanges, Route.Messages, Route.Profile,
        Route.AdminDashboard, Route.AdminValidate, Route.AdminDisputes
    )
}

/** Routes réservées au médiateur. */
fun Route.requiresMediator(): Boolean =
    this == Route.AdminDashboard || this == Route.AdminValidate || this == Route.AdminDisputes

/** Routes interdites au médiateur (il n'échange pas d'objets). */
fun Route.forbiddenForMediator(): Boolean =
    this == Route.Exchanges || this == Route.Messages || this == Route.Solidarity ||
            this == Route.Publish || this == Route.FundraiserCreate
