package com.example.rentalinaja.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.rentalinaja.data.model.Mobil
import com.example.rentalinaja.navigation.Screen
import com.example.rentalinaja.viewmodel.MobilViewModel

private const val PLACEHOLDER_IMG =
    "https://via.placeholder.com/900x450?text=RentalinAja+Car"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailMobilScreen(
    navController: NavController,
    mobilId: String,
    vm: MobilViewModel = viewModel()
) {
    val list by vm.mobilList.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    val mobil: Mobil? = remember(list, mobilId) {
        list.firstOrNull { it.mobilId == mobilId }
    }

    val tersedia = mobil?.status?.equals("Tersedia", ignoreCase = true) == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Mobil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            }

            if (mobil == null) {
                Text("Data mobil belum termuat / tidak ditemukan.")
                return@Column
            }

            AsyncImage(
                model = mobil.fotoUrl.ifBlank { PLACEHOLDER_IMG },
                contentDescription = "Foto Mobil",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Text(mobil.namaMobil, style = MaterialTheme.typography.headlineSmall)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Harga per hari: Rp ${mobil.hargaPerHari}")
                    Text("Kapasitas: ${mobil.kapasitas} orang")
                    Text("Status: ${mobil.status}")
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { navController.navigate(Screen.Booking.createRoute(mobil.mobilId)) },
                enabled = tersedia,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(if (tersedia) "Booking Mobil Ini" else "Tidak Bisa Booking (Disewa)")
            }
        }
    }
}
