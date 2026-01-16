package com.example.rentalinaja.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.rentalinaja.data.model.Mobil
import com.example.rentalinaja.navigation.Screen
import com.example.rentalinaja.viewmodel.MobilViewModel
import com.google.firebase.auth.FirebaseAuth

private const val PLACEHOLDER_IMG =
    "https://via.placeholder.com/900x450?text=RentalinAja+Car"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    navController: NavController,
    vm: MobilViewModel = viewModel()
) {
    val list by vm.mobilList.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") } // ALL | Tersedia | Disewa
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val filteredList = remember(list, query, filter) {
        list.filter { mobil ->
            val matchQuery = query.isBlank() || mobil.namaMobil.contains(query, ignoreCase = true)
            val matchFilter = when (filter) {
                "ALL" -> true
                "Tersedia" -> mobil.status.equals("Tersedia", ignoreCase = true)
                "Disewa" -> mobil.status.equals("Disewa", ignoreCase = true)
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RentalinAja") },
                actions = {
                    IconButton(onClick = { vm.refreshMobils() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showLogoutConfirm = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Cari mobil...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = filter == "ALL",
                    onClick = { filter = "ALL" },
                    label = { Text("Semua") }
                )
                FilterChip(
                    selected = filter == "Tersedia",
                    onClick = { filter = "Tersedia" },
                    label = { Text("Tersedia") }
                )
                FilterChip(
                    selected = filter == "Disewa",
                    onClick = { filter = "Disewa" },
                    label = { Text("Disewa") }
                )
            }

            // Tombol Riwayat
            Button(
                onClick = { navController.navigate(Screen.UserRiwayat.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Riwayat Booking")
            }

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            }

            if (!loading && filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada mobil yang cocok.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredList) { mobil ->
                        UserMobilCard(
                            mobil = mobil,
                            onDetail = {
                                navController.navigate(Screen.DetailMobil.createRoute(mobil.mobilId))
                            },
                            onBooking = {
                                navController.navigate(Screen.Booking.createRoute(mobil.mobilId))
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout") },
            text = { Text("Yakin ingin logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.UserHome.route) { inclusive = true }
                    }
                }) { Text("Ya") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun UserMobilCard(
    mobil: Mobil,
    onDetail: () -> Unit,
    onBooking: () -> Unit
) {
    val tersedia = mobil.status.equals("Tersedia", ignoreCase = true)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = mobil.fotoUrl.ifBlank { PLACEHOLDER_IMG },
                contentDescription = "Foto Mobil",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )

            Text(mobil.namaMobil, style = MaterialTheme.typography.titleMedium)
            Text("Harga/hari: Rp ${mobil.hargaPerHari}")
            Text("Kapasitas: ${mobil.kapasitas} orang")
            Text("Status: ${mobil.status}")

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDetail,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Detail")
                }
                Button(
                    onClick = onBooking,
                    enabled = tersedia,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Booking")
                }
            }
        }
    }
}
