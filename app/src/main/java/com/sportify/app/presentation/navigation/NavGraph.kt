package com.sportify.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import com.sportify.app.presentation.auth.LoginScreen
import com.sportify.app.presentation.auth.RegisterScreen
import com.sportify.app.presentation.bookings.BookingsScreen
import com.sportify.app.presentation.checkout.CheckoutScreen
import com.sportify.app.presentation.common.CenterLoading
import com.sportify.app.presentation.home.HomeScreen
import com.sportify.app.presentation.profile.ProfileScreen
import com.sportify.app.presentation.venue.VenueDetailScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    /** null = still resolving, true = signed in, false = signed out */
    val loggedIn: StateFlow<Boolean?> = auth.authState
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            auth.authState.collect { if (it) auth.registerFcmToken() }
        }
    }
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Explore", Icons.Default.Explore),
    Bookings("bookings", "Bookings", Icons.Default.EventAvailable),
    Profile("profile", "Profile", Icons.Default.Person)
}

@Composable
fun SportifyRoot(vm: RootViewModel = hiltViewModel()) {
    val loggedIn by vm.loggedIn.collectAsStateWithLifecycle()
    when (loggedIn) {
        null -> CenterLoading()
        false -> AuthGraph()
        true -> MainGraph()
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
            composable(Tab.Home.route) { HomeScreen(onVenueClick = { nav.navigate("venue/$it") }) }
            composable(Tab.Bookings.route) { BookingsScreen(onPay = { nav.navigate("checkout/$it") }) }
            composable(Tab.Profile.route) { ProfileScreen() }
            composable("venue/{venueId}") {
                VenueDetailScreen(onBack = { nav.popBackStack() }, onBookingCreated = { id -> nav.navigate("checkout/$id") })
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
