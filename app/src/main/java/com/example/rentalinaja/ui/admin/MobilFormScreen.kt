package com.example.rentalinaja.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rentalinaja.data.model.Mobil
import com.example.rentalinaja.viewmodel.MobilViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobilFormScreen(
    navController: NavController,
    mobilId: String?,
    vm: MobilViewModel = viewModel()
) {
    val list by vm.mobilList.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    // Cari data existing kalau mode edit
    val existing: Mobil? = remember(list, mobilId) {
        mobilId?.let { id -> list.firstOrNull { it.mobilId == id } }
    }

    // ---- STATE FORM (default kosong, nanti diisi via prefill) ----
    var nama by remember { mutableStateOf("") }
    var harga by remember { mutableStateOf("") }
    var kapasitas by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Tersedia") }
    var fotoUrl by remember { mutableStateOf("") }

    var showInvalid by remember { mutableStateOf(false) }

    // Supaya prefill cuma sekali dan gak menimpa saat user mulai edit
    var prefilled by remember { mutableStateOf(false) }

    // ✅ PREFILL: saat existing sudah ketemu, isi otomatis form
    LaunchedEffect(existing, mobilId) {
        if (mobilId == null) {
            // Mode Tambah: set default (sekali)
            if (!prefilled) {
                nama = ""
                harga = ""
                kapasitas = ""
                status = "Tersedia"
                fotoUrl = ""
                prefilled = true
            }
        } else {
            // Mode Edit: tunggu existing ketemu
            if (existing != null && !prefilled) {
                nama = existing.namaMobil
                harga = existing.hargaPerHari.toString()
                kapasitas = existing.kapasitas.toString()
                status = existing.status
                fotoUrl = existing.fotoUrl
                prefilled = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mobilId == null) "Tambah Mobil" else "Edit Mobil") },
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
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
            }

            // Kalau mode edit tapi data belum ketemu, tampilkan info (biar gak bingung)
            if (mobilId != null && existing == null && !loading) {
                Text(
                    text = "Data mobil belum ditemukan / belum termuat. Pastikan ID benar.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (showInvalid) {
                Text(
                    text = "Pastikan Nama, Harga, dan Kapasitas diisi dengan benar.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            OutlinedTextField(
                value = nama,
                onValueChange = { nama = it },
                label = { Text("Nama Mobil") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = harga,
                onValueChange = { harga = it.filter(Char::isDigit) },
                label = { Text("Harga per Hari (angka)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = kapasitas,
                onValueChange = { kapasitas = it.filter(Char::isDigit) },
                label = { Text("Kapasitas (angka)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = fotoUrl,
                onValueChange = { fotoUrl = it },
                label = { Text("Foto URL (opsional)") },
                placeholder = { Text("https://...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = status == "Tersedia",
                    onClick = { status = "Tersedia" },
                    label = { Text("Tersedia") }
                )
                FilterChip(
                    selected = status == "Disewa",
                    onClick = { status = "Disewa" },
                    label = { Text("Disewa") }
                )
            }

            Button(
                onClick = {
                    val hargaInt = harga.toIntOrNull()
                    val kapasitasInt = kapasitas.toIntOrNull()

                    // Validasi wajib
                    if (nama.isBlank() || hargaInt == null || hargaInt <= 0 || kapasitasInt == null || kapasitasInt <= 0) {
                        showInvalid = true
                        return@Button
                    }

                    // fotoUrl opsional; kalau diisi harus http/https
                    if (fotoUrl.isNotBlank() && !(fotoUrl.startsWith("http://") || fotoUrl.startsWith("https://"))) {
                        showInvalid = true
                        return@Button
                    }

                    showInvalid = false

                    val hargaVal = hargaInt
                    val kapasitasVal = kapasitasInt

                    if (mobilId == null) {
                        vm.addMobil(nama, hargaVal, kapasitasVal, status, fotoUrl)
                    } else {
                        vm.updateMobil(mobilId, nama, hargaVal, kapasitasVal, status, fotoUrl)
                    }

                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !loading
            ) {
                Text(if (mobilId == null) "Simpan" else "Update")
            }
        }
    }
}
