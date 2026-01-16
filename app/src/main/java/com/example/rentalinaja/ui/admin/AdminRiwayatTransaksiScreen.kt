package com.example.rentalinaja.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.rentalinaja.viewmodel.BookingRiwayatAdminViewModel
import com.example.rentalinaja.viewmodel.AdminRiwayatItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRiwayatTransaksiScreen(
    navController: NavController,
    vm: BookingRiwayatAdminViewModel = viewModel()
) {
    val list by vm.list.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()

    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") } // ALL | Pending | Approved | Rejected | Cancelled

    val filteredList = remember(list, query, filter) {
        val q = query.trim().lowercase()

        list
            .asSequence()
            .filter { item ->
                val st = normalizeStatus(item.status)
                when (filter) {
                    "Pending" -> st == "pending"
                    "Approved" -> st == "approved"
                    "Rejected" -> st == "rejected"
                    "Cancelled" -> st == "cancelled"
                    else -> true
                }
            }
            .filter { item ->
                if (q.isBlank()) return@filter true

                val mobil = item.mobilNama.lowercase()
                val email = item.userEmail.lowercase()
                val uid = item.userId.lowercase()

                mobil.contains(q) || email.contains(q) || uid.contains(q)
            }
            .toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi") },
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                // ✅ Cancelled
                FilterChip(
                    selected = filter == "Cancelled",
                    onClick = { filter = "Cancelled" },
                    label = { Text("Cancelled") }
                )
            }

            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            if (error != null) {
                Text(text = error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            if (!loading && filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Data tidak ditemukan.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredList, key = { it.bookingId }) { item ->
                        AdminRiwayatCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRiwayatCard(item: AdminRiwayatItem) {
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
        }
    }
}

private fun normalizeStatus(status: String): String {
    val st = status.trim().lowercase()
    return when (st) {
        "canceled" -> "cancelled"
        else -> st
    }
}

private fun formatDate(millis: Long): String {
    if (millis <= 0L) return "-"
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("in", "ID"))
    return sdf.format(Date(millis))
}
