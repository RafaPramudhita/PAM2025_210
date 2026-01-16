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
import com.example.rentalinaja.viewmodel.BookingCreateViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    navController: NavController,
    mobilId: String,
    vm: BookingCreateViewModel = viewModel()
) {
    val mobil by vm.mobil.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val message by vm.message.collectAsState()

    val dayMs = 24L * 60L * 60L * 1000L

    // ===== today start (00:00) =====
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    // Load mobil once
    LaunchedEffect(mobilId) {
        vm.loadMobil(mobilId)
    }

    // date state
    var tanggalSewaMillis by remember { mutableStateOf(0L) }
    var tanggalKembaliMillis by remember { mutableStateOf(0L) }

    // dialogs
    var openSewaPicker by remember { mutableStateOf(false) }
    var openKembaliPicker by remember { mutableStateOf(false) }

    // snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // show message (success)
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message!!)
            vm.consumeMessage()
            // setelah sukses, kembali ke screen sebelumnya
            navController.popBackStack()
        }
    }

    // hitung total sementara (UI)
    val totalText = remember(mobil, tanggalSewaMillis, tanggalKembaliMillis) {
        val m = mobil ?: return@remember "-"
        val minKembaliValid = if (tanggalSewaMillis > 0L) (tanggalSewaMillis + dayMs) else 0L

        if (tanggalSewaMillis <= 0L || tanggalKembaliMillis <= 0L || tanggalKembaliMillis < minKembaliValid) {
            return@remember "-"
        }

        val days = ((tanggalKembaliMillis - tanggalSewaMillis) / dayMs).toInt().coerceAtLeast(1)
        "Rp ${days * m.hargaPerHari} ($days hari)"
    }

    // submit enabled hanya kalau valid
    val canSubmit = remember(mobil, loading, tanggalSewaMillis, tanggalKembaliMillis) {
        val tersedia = (mobil?.status?.equals("Tersedia", true) == true)
        val minKembali = if (tanggalSewaMillis > 0L) tanggalSewaMillis + dayMs else 0L
        !loading && tersedia && tanggalSewaMillis > 0L && tanggalKembaliMillis >= minKembali
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Booking") },
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

            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (!error.isNullOrBlank()) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            // Mobil info
            Text(text = mobil?.namaMobil ?: "Memuat mobil...", style = MaterialTheme.typography.titleMedium)
            Text(text = "Harga/hari: Rp ${mobil?.hargaPerHari ?: 0}")
            Text(text = "Status: ${mobil?.status ?: "-"}")

            Divider()

            // ===== Tanggal Sewa =====
            OutlinedTextField(
                value = formatDate(tanggalSewaMillis),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tanggal Sewa") }
            )
            Button(
                onClick = { openSewaPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pilih Tanggal Sewa")
            }

            // ===== Tanggal Kembali =====
            OutlinedTextField(
                value = formatDate(tanggalKembaliMillis),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tanggal Kembali (minimal H+1)") }
            )
            Button(
                onClick = { openKembaliPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Pilih Tanggal Kembali")
            }

            // Total
            Text(text = "Total: $totalText")

            Button(
                onClick = {
                    vm.submitBooking(
                        mobilId = mobilId,
                        tanggalSewaMillis = tanggalSewaMillis,
                        tanggalKembaliMillis = tanggalKembaliMillis
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = canSubmit
            ) {
                Text("Submit Booking")
            }
        }
    }

    // ===== DatePicker: Tanggal Sewa (>= hari ini) =====
    if (openSewaPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = if (tanggalSewaMillis > 0L) tanggalSewaMillis else todayStart,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= todayStart
                }
                override fun isSelectableYear(year: Int): Boolean {
                    return year >= currentYear
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { openSewaPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = state.selectedDateMillis ?: 0L
                    if (picked >= todayStart) {
                        tanggalSewaMillis = picked

                        // ✅ kalau tanggal kembali jadi tidak valid (harus minimal H+1), reset
                        val minKembali = tanggalSewaMillis + dayMs
                        if (tanggalKembaliMillis > 0L && tanggalKembaliMillis < minKembali) {
                            tanggalKembaliMillis = 0L
                        }
                    }
                    openSewaPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { openSewaPicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    // ===== DatePicker: Tanggal Kembali (>= tanggal sewa + 1 hari) =====
    if (openKembaliPicker) {
        // ✅ kalau tanggal sewa belum dipilih, minimal kembali = besok dari hari ini
        val minKembali = remember(tanggalSewaMillis) {
            if (tanggalSewaMillis > 0L) tanggalSewaMillis + dayMs else todayStart + dayMs
        }

        val state = rememberDatePickerState(
            initialSelectedDateMillis = when {
                tanggalKembaliMillis > 0L && tanggalKembaliMillis >= minKembali -> tanggalKembaliMillis
                else -> minKembali
            },
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= minKembali
                }
                override fun isSelectableYear(year: Int): Boolean {
                    return year >= currentYear
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { openKembaliPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val picked = state.selectedDateMillis ?: 0L
                    if (picked >= minKembali) {
                        tanggalKembaliMillis = picked
                    }
                    openKembaliPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { openKembaliPicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

private fun formatDate(millis: Long): String {
    if (millis <= 0L) return ""
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))
    return sdf.format(Date(millis))
}
