package com.example.rentalinaja

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.rememberNavController
import com.example.rentalinaja.navigation.AppNavigation
import com.example.rentalinaja.ui.theme.RentalinAjaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RentalinAjaTheme {
                val navController = rememberNavController()
                Scaffold {
                    AppNavigation(navController = navController)
                }
            }
        }
    }
}
