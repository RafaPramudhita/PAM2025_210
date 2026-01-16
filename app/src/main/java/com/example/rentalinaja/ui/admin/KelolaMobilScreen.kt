package com.example.rentalinaja.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
fun KelolaMobilScreen(
    navController: NavController,
    vm: MobilViewModel = viewModel()
) {
    val list by vm.mobilList.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var showDelete by remember { mutableStateOf<Mobil?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Mobil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.MobilForm.createRoute(null))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Mobil")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list) { mobil ->
                    MobilCard(
                        mobil = mobil,
                        onEdit = { navController.navigate(Screen.MobilForm.createRoute(mobil.mobilId)) },
                        onDelete = { showDelete = mobil }
                    )
                }
            }
        }
    }

    if (showDelete != null) {
        AlertDialog(
            onDismissRequest = { showDelete = null },
            title = { Text("Hapus Mobil") },
            text = { Text("Yakin ingin menghapus mobil: ${showDelete!!.namaMobil}?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteMobil(showDelete!!)
                    showDelete = null
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun MobilCard(
    mobil: Mobil,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
                OutlinedButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Hapus")
                }
            }
        }
    }
}
