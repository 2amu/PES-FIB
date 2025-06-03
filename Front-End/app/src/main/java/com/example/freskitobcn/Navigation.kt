package com.example.freskitobcn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.freskitobcn.Mapa.MapaScreen
import com.example.freskitobcn.Refugi.DetailsRefugi
import com.example.freskitobcn.ResetPassword.PasswordResetScreen
import com.example.freskitobcn.Xat.MainXatScreen
import com.example.freskitobcn.Xat.XatScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignIn : Screen("sign_in")
    object PasswordReset : Screen("password_reset")
    object Home : Screen("home")
    object Xat : Screen("xat")
    object Map : Screen("map")
    object Ranking : Screen("ranking")
    object MapWithRefugi : Screen("map/{refugiName}") {
        fun createRoute(refugiName: String): String {
            return "map/$refugiName"
        }
    }
    object Settings : Screen("settings")

    // New route with parameter for details screen
    object RefugiDetails : Screen("refugi_details/{refugiName}") {
        fun createRoute(refugiName: String): String {
            return "refugi_details/$refugiName"
        }
    }

    object XatDetails : Screen("xat/{refugiId}/{refugiName}") {
        fun createRoute(refugiId: Int, refugiName: String): String {
            return "xat/$refugiId/$refugiName"
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry.value?.destination?.route ?: Screen.Login.route

    // Check if we're on a main tab screen
    currentRoute == Screen.Home.route ||
    currentRoute == Screen.Xat.route ||
    currentRoute == Screen.Map.route ||
    currentRoute.startsWith("map/") ||
    currentRoute == Screen.Settings.route

    // Determine current tab for Footer highlight
    val currentTab = when {
        currentRoute == Screen.Home.route -> Screen.Home
        currentRoute == Screen.Xat.route -> Screen.Xat
        currentRoute == Screen.Map.route || currentRoute.startsWith("map/") -> Screen.Map
        currentRoute == Screen.Settings.route -> Screen.Settings
        currentRoute.startsWith("refugi_details") -> Screen.Home // When on details, keep Home tab active
        else -> Screen.Home
    }

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Ranking.route) {
            MainAppScaffold(
                navController = navController,
                currentScreen = Screen.Ranking
            ) {
                RefugiRankingViewVisual(navController)
            }
        }

        composable(Screen.Login.route) {
            LogInScreen(
                onNavigateToSignIn = { navController.navigate(Screen.SignIn.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToPasswordReset = {
                    navController.navigate(Screen.PasswordReset.route)
                }
            )
        }

        composable(Screen.SignIn.route) {
            SignInScreen(
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.PasswordReset.route) {
            PasswordResetScreen(
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // Main tab screens
        composable(Screen.Home.route) {
            MainAppScaffold(
                navController = navController,
                currentScreen = currentTab
            ) {
                MainPage(
                    onRefugiClick = { refugiName ->
                        navController.navigate(Screen.RefugiDetails.createRoute(refugiName))
                    }
                )
            }
        }

        composable(Screen.Xat.route) {
            MainAppScaffold(
                navController = navController,
                currentScreen = currentTab
            ) {
                MainXatScreen(
                    onRefugiClick = { refugiId, refugiName ->
                        navController.navigate(Screen.XatDetails.createRoute(refugiId, refugiName))
                    }
                )
            }
        }

        composable(Screen.Map.route) {
            MainAppScaffold(
                navController = navController,
                currentScreen = currentTab
            ) {
                MapaScreen(
                    onRefugiClick = { refugiName ->
                        navController.navigate(Screen.RefugiDetails.createRoute(refugiName))
                    }
                )
            }
        }

        composable(
            route = Screen.MapWithRefugi.route,
            arguments = listOf(navArgument("refugiName") { type = NavType.StringType })
        ) { backStackEntry ->
            val refugiName = backStackEntry.arguments?.getString("refugiName") ?: ""

            MainAppScaffold(
                navController = navController,
                currentScreen = currentTab
            ) {
                MapaScreen(
                    selectedRefugiName = refugiName,
                    onRefugiClick = { clickedRefugiName ->
                        navController.navigate(Screen.RefugiDetails.createRoute(clickedRefugiName))
                    }
                )
            }
        }

        composable(Screen.Settings.route) {
            MainAppScaffold(
                navController = navController,
                currentScreen = currentTab
            ) {
                SettingsScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(
            route = Screen.RefugiDetails.route,
            arguments = listOf(navArgument("refugiName") { type = NavType.StringType })
        ) { backStackEntry ->
            val refugiName = backStackEntry.arguments?.getString("refugiName") ?: ""

            MainAppScaffold(
                navController = navController,
                currentScreen = currentTab
            ) {
                DetailsRefugi(
                    refugiName = refugiName,
                    onBackClick = {
                        navController.popBackStack()
                    },
                    navigateToMap = {
                        navController.navigate(Screen.MapWithRefugi.createRoute(refugiName)) {
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(
            route = Screen.XatDetails.route,
            arguments = listOf(
                navArgument("refugiId") { type = NavType.IntType },
                navArgument("refugiName") { type = NavType.StringType })
        ) { backStackEntry ->
            val refugiId = backStackEntry.arguments?.getInt("refugiId") ?: -1
            val refugiName = backStackEntry.arguments?.getString("refugiName") ?: ""

            MainAppScaffold(
                navController = navController,
                currentScreen = Screen.Xat
            ) {
                XatScreen(
                    refugiId = refugiId,
                    refugi = refugiName,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    navController: NavHostController,
    currentScreen: Screen,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Common header
        Header("241ebae7fbd04c5fb37141416252404", navController, currentScreen)

        // Main content area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            content()
        }

        // Common footer with navigation
        Footer(
            currentScreen = currentScreen,
            onScreenSelected = { screen ->
                if (screen.route != currentScreen.route) {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to avoid building up
                        // a large stack of destinations
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            }
        )
    }
}