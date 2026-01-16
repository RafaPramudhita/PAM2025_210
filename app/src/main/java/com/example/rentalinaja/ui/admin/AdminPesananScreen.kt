package com.example.rentalinaja.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rentalinaja.viewmodel.AdminPendingItem
import com.example.rentalinaja.viewmodel.BookingAdminPendingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPesananScreen(
    navController: NavController,
    vm: BookingAdminPendingViewModel = viewModel()
) {
    val list by vm.list.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var query by remember { mutableStateOf("") }

    var confirmApprove by remember { mutableStateOf<AdminPendingItem?>(null) }
    var confirmReject by remember { mutableStateOf<AdminPendingItem?>(null) }

    val filtered = remember(list, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) list
        else list.filter { item ->
            item.mobilNama.lowercase().contains(q) ||
                    item.userEmail.lowercase().contains(q) ||
                    item.userId.lowercase().contains(q)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pesanan Masuk (Pending)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Cari (mobil / email / userId)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Text(text = error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            if (!loading && filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tidak ada pesanan pending.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filtered, key = { it.bookingId }) { item ->
                        PendingCard(
                            item = item,
                            onApprove = { confirmApprove = item },
                            onReject = { confirmReject = item }
                        )
                    }
                }
            }
        }
    }

    // ===== Confirm Approve =====
    if (confirmApprove != null) {
        val item = confirmApprove!!
        AlertDialog(
            onDismissRequest = { confirmApprove = null },
            title = { Text("Approve Booking") },
            text = { Text("Approve booking untuk mobil: ${item.mobilNama}?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.approve(item.bookingId, item.mobilId)
                    confirmApprove = null
                }) { Text("Approve") }
            },
            dismissButton = {
                TextButton(onClick = { confirmApprove = null }) { Text("Batal") }
            }
        )
    }

    // ===== Confirm Reject =====
    if (confirmReject != null) {
        val item = confirmReject!!
        AlertDialog(
            onDismissRequest = { confirmReject = null },
            title = { Text("Reject Booking") },
            text = { Text("Reject booking untuk mobil: ${item.mobilNama}?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.reject(item.bookingId, item.mobilId)
                    confirmReject = null
                }) { Text("Reject") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReject = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun PendingCard(
    item: AdminPendingItem,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val dateText = remember(item.tanggalSewaMillis, item.tanggalKembaliMillis) {
        "${formatDate(item.tanggalSewaMillis)} - ${formatDate(item.tanggalKembaliMillis)}"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(item.mobilNama, style = MaterialTheme.typography.titleMedium)
            Text("User: ${item.userEmail}")
            Text("Tanggal: $dateText")
            Text("Total: Rp ${item.totalHarga}")
            Text("Status: ${item.status}")

            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Approve")
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reject")
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
