package com.example.rentalinaja.navigation

sealed class Screen(val route: String) {

    // ===== AUTH =====
    object Login : Screen("login")
    object Register : Screen("register")

    // ===== USER =====
    object UserHome : Screen("user_home")
    object UserRiwayat : Screen("user_riwayat")

    object DetailMobil : Screen("detail_mobil/{mobilId}") {
        fun createRoute(mobilId: String) = "detail_mobil/$mobilId"
    }

    object Booking : Screen("booking/{mobilId}") {
        fun createRoute(mobilId: String) = "booking/$mobilId"
    }

    // ===== ADMIN =====
    object AdminDashboard : Screen("admin_dashboard")
    object KelolaMobil : Screen("kelola_mobil")
    object AdminPesanan : Screen("admin_pesanan")

    // kamu pakai ini di AppNavigation: Screen.AdminRiwayat.route
    object AdminRiwayat : Screen("admin_riwayat")

    /**
     * MobilForm pakai mobilId optional (buat edit / tambah).
     * Route yang cocok untuk navArgument("mobilId") adalah pakai query param:
     * "mobil_form?mobilId={mobilId}"
     */
    object MobilForm : Screen("mobil_form?mobilId={mobilId}") {
        fun createRoute(mobilId: String? = null): String {
            return if (mobilId.isNullOrBlank()) "mobil_form"
            else "mobil_form?mobilId=$mobilId"
        }
    }
}
