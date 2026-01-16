package com.example.rentalinaja.ui.user

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rentalinaja.viewmodel.BookingUserViewModel
import com.example.rentalinaja.viewmodel.UserBookingItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRiwayatScreen(
    navController: NavController,
    vm: BookingUserViewModel = viewModel()
) {
    val list by vm.list.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var filter by remember { mutableStateOf("ALL") } // ALL | Pending | Approved | Rejected | Cancelled
    var confirmCancel by remember { mutableStateOf<UserBookingItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var lastShownError by remember { mutableStateOf<String?>(null) }

    // SnackBar kalau error berubah
    LaunchedEffect(error) {
        if (!error.isNullOrBlank() && error != lastShownError) {
            lastShownError = error
            snackbarHostState.showSnackbar(error!!)
        }
    }

    val filteredList = remember(list, filter) {
        when (filter) {
            "Pending" -> list.filter { it.status.equals("Pending", ignoreCase = true) }
            "Approved" -> list.filter { it.status.equals("Approved", ignoreCase = true) }
            "Rejected" -> list.filter { it.status.equals("Rejected", ignoreCase = true) }
            "Cancelled" -> list.filter { it.status.equals("Cancelled", ignoreCase = true) }
            else -> list
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Booking") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Filter Chips (rapih)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = filter == "ALL",
                    onClick = { filter = "ALL" },
                    label = { Text("Semua") }
                )
                FilterChip(
                    selected = filter == "Pending",
                    onClick = { filter = "Pending" },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = filter == "Approved",
                    onClick = { filter = "Approved" },
                    label = { Text("Approved") }
                )
                FilterChip(
                    selected = filter == "Rejected",
                    onClick = { filter = "Rejected" },
                    label = { Text("Rejected") }
                )
                FilterChip(
                    selected = filter == "Cancelled",
                    onClick = { filter = "Cancelled" },
                    label = { Text("Cancelled") }
                )
            }

            Spacer(Modifier.height(10.dp))

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }

            // kalau mau tetap tampil error text juga (opsional)
            if (!error.isNullOrBlank()) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(10.dp))
            }

            if (!loading && filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada riwayat booking.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredList, key = { it.bookingId }) { item ->
                        RiwayatCard(
                            item = item,
                            onCancel = { confirmCancel = item }
                        )
                    }
                }
            }
        }
    }

    // Dialog konfirmasi cancel
    if (confirmCancel != null) {
        val item = confirmCancel!!
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            title = { Text("Cancel Booking") },
            text = { Text("Yakin mau cancel booking mobil ini?") },
            confirmButton = {
                TextButton(onClick = {
                    if (item.status.equals("Pending", ignoreCase = true)) {
                        vm.cancelBookingPending(
                            bookingId = item.bookingId,
                            mobilId = item.mobilId
                        )
                    }
                    confirmCancel = null
                }) {
                    Text("Ya, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun RiwayatCard(
    item: UserBookingItem,
    onCancel: () -> Unit
) {
    val isPending = item.status.equals("Pending", ignoreCase = true)

    val dateText = remember(item.tanggalSewaMillis, item.tanggalKembaliMillis) {
        "${formatDate(item.tanggalSewaMillis)} - ${formatDate(item.tanggalKembaliMillis)}"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (item.mobilNama.isNotBlank()) {
                Text(item.mobilNama, style = MaterialTheme.typography.titleMedium)
            }

            Text("Tanggal: $dateText")
            Text("Total: Rp ${item.totalHarga}")
            Text("Status: ${item.status}")

            // tombol cancel hanya untuk pending
            if (isPending) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Booking")
                }
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    if (millis <= 0L) return "-"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))
    return sdf.format(Date(millis))
}
