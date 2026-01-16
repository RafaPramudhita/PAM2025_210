package com.example.rentalinaja.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.rentalinaja.ui.admin.AdminDashboardScreen
import com.example.rentalinaja.ui.admin.AdminPesananScreen
import com.example.rentalinaja.ui.admin.AdminRiwayatTransaksiScreen
import com.example.rentalinaja.ui.admin.KelolaMobilScreen
import com.example.rentalinaja.ui.admin.MobilFormScreen
import com.example.rentalinaja.ui.auth.LoginScreen
import com.example.rentalinaja.ui.auth.RegisterScreen
import com.example.rentalinaja.ui.user.BookingScreen
import com.example.rentalinaja.ui.user.DetailMobilScreen
import com.example.rentalinaja.ui.user.UserHomeScreen
import com.example.rentalinaja.ui.user.UserRiwayatScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {

        // ===== AUTH =====
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }

        // ===== USER =====
        composable(Screen.UserHome.route) { UserHomeScreen(navController) }
        composable(Screen.UserRiwayat.route) { UserRiwayatScreen(navController) }

        composable(
            route = Screen.DetailMobil.route,
            arguments = listOf(navArgument("mobilId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mobilId = backStackEntry.arguments?.getString("mobilId") ?: ""
            DetailMobilScreen(navController = navController, mobilId = mobilId)
        }

        composable(
            route = Screen.Booking.route,
            arguments = listOf(navArgument("mobilId") { type = NavType.StringType })
        ) { backStackEntry ->
            val mobilId = backStackEntry.arguments?.getString("mobilId") ?: ""
            BookingScreen(navController = navController, mobilId = mobilId)
        }

        // ===== ADMIN =====
        composable(Screen.AdminDashboard.route) { AdminDashboardScreen(navController) }
        composable(Screen.KelolaMobil.route) { KelolaMobilScreen(navController) }
        composable(Screen.AdminPesanan.route) { AdminPesananScreen(navController) }
        composable(Screen.AdminRiwayat.route) { AdminRiwayatTransaksiScreen(navController) }

        // ===== MOBIL FORM (query param mobilId optional) =====
        composable(
            route = Screen.MobilForm.route,
            arguments = listOf(
                navArgument("mobilId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val mobilId = backStackEntry.arguments?.getString("mobilId")?.ifBlank { null }
            MobilFormScreen(navController = navController, mobilId = mobilId)
        }
    }
}
