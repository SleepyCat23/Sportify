package com.sportify.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportify.app.domain.repository.AuthRepository
import com.sportify.app.presentation.admin.AdminReviewsScreen
import com.sportify.app.presentation.admin.AdminTransactionsScreen
import com.sportify.app.presentation.admin.AdminVenuesScreen
import com.sportify.app.presentation.auth.LoginScreen
import com.sportify.app.presentation.auth.RegisterScreen
import com.sportify.app.presentation.bookings.BookingsRoute
import com.sportify.app.presentation.bookings.BookingsScreen
import com.sportify.app.presentation.checkout.CheckoutScreen
import com.sportify.app.presentation.checkout.PaymentInfoScreen
import com.sportify.app.presentation.common.CenterLoading
import com.sportify.app.presentation.home.HomeRoute
import com.sportify.app.presentation.home.HomeScreen
import com.sportify.app.presentation.openplay.OpenPlayRoute
import com.sportify.app.presentation.favorites.FavoritesRoute
import com.sportify.app.presentation.profile.ProfileScreen
import com.sportify.app.presentation.venue.VenueDetailScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    /** null = still resolving, true = signed in, false = signed out */
    val loggedIn: StateFlow<Boolean?> = auth.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** null = profile still loading. Role comes from users/{uid}.role, which only the console can set. */
    val isAdmin: StateFlow<Boolean?> = auth.observeProfile().map { it?.role == "ADMIN" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun signOut() = auth.signOut()

    init {
        viewModelScope.launch {
            auth.authState.collect { if (it) auth.registerFcmToken() }
        }
    }
}

private enum class AdminTab(val route: String, val label: String, val icon: ImageVector) {
    Venues("admin_venues", "Venues", Icons.Default.Stadium),
    Payments("admin_payments", "Payments", Icons.Default.Payments),
    Reviews("admin_reviews", "Reviews", Icons.Default.RateReview)
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Explore", Icons.Default.Explore),
    Favorites("favorites", "Favorites", Icons.Default.Favorite),
    Play("play", "Play", Icons.Default.Groups),
    Bookings("bookings", "Bookings", Icons.Default.EventAvailable),
    Profile("profile", "Profile", Icons.Default.Person)
}

@Composable
fun SportifyRoot(vm: RootViewModel = hiltViewModel()) {
    val loggedIn by vm.loggedIn.collectAsStateWithLifecycle()
    when (loggedIn) {
        null -> CenterLoading()
        false -> AuthGraph()
        true -> {
            val isAdmin by vm.isAdmin.collectAsStateWithLifecycle()
            when (isAdmin) {
                null -> CenterLoading()
                true -> AdminGraph(onSignOut = vm::signOut)
                false -> MainGraph()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminGraph(onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sportify Admin") },
                actions = { IconButton(onSignOut) { Icon(Icons.AutoMirrored.Filled.Logout, "Sign out") } }
            )
        },
        bottomBar = {
            NavigationBar {
                AdminTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, tab.label) }, label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = AdminTab.Venues.route, modifier = Modifier.padding(pad).consumeWindowInsets(pad)) {
            composable(AdminTab.Venues.route) { AdminVenuesScreen() }
            composable(AdminTab.Payments.route) { AdminTransactionsScreen() }
            composable(AdminTab.Reviews.route) { AdminReviewsScreen() }
        }
    }
}

@Composable
private fun AuthGraph() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "login") {
        composable("login") { LoginScreen(onRegister = { nav.navigate("register") }) }
        composable("register") { RegisterScreen(onBack = { nav.popBackStack() }) }
    }
}

@Composable
private fun MainGraph() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    Scaffold(bottomBar = {
        if (Tab.entries.any { it.route == route }) NavigationBar {
            Tab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = route == tab.route,
                    onClick = {
                        nav.navigate(tab.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    icon = { Icon(tab.icon, tab.label) }, label = { Text(tab.label) }
                )
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = Tab.Home.route, modifier = Modifier.padding(pad).consumeWindowInsets(pad)) {
            composable(Tab.Home.route) { HomeRoute(onVenueClick = { nav.navigate("venue/$it") }) }
            composable(Tab.Favorites.route) { FavoritesRoute(onVenueClick = { nav.navigate("venue/$it") }) }
            composable(Tab.Play.route) { OpenPlayRoute() }
            composable(Tab.Bookings.route) { BookingsRoute(onPay = { nav.navigate("payment_info/$it") }) }
            composable(Tab.Profile.route) { ProfileScreen() }
            composable("venue/{venueId}") {
                VenueDetailScreen(
                    onBack = { nav.popBackStack() },
                    onBookingCreated = { id -> nav.navigate("payment_info/$id") }
                )
            }
            composable("payment_info/{bookingId}") {
                PaymentInfoScreen(
                    onBack = { nav.popBackStack() },
                    onDone = {
                        nav.navigate(Tab.Bookings.route) { popUpTo(Tab.Home.route); launchSingleTop = true }
                    }
                )
            }
            composable("checkout/{bookingId}") {
                CheckoutScreen(
                    onBack = { nav.popBackStack() },
                    onDone = {
                        nav.navigate(Tab.Bookings.route) { popUpTo(Tab.Home.route); launchSingleTop = true }
                    }
                )
            }
        }
    }
}
