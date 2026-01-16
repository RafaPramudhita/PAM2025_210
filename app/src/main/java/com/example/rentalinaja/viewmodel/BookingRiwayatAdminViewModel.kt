package com.example.rentalinaja.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AdminRiwayatItem(
    val bookingId: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val mobilId: String = "",
    val mobilNama: String = "",
    val tanggalSewaMillis: Long = 0L,
    val tanggalKembaliMillis: Long = 0L,
    val totalHarga: Int = 0,
    val status: String = "Pending",
    val createdAtMillis: Long = 0L
)

class BookingRiwayatAdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var reg: ListenerRegistration? = null

    private val _list = MutableStateFlow<List<AdminRiwayatItem>>(emptyList())
    val list: StateFlow<List<AdminRiwayatItem>> get() = _list

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    init {
        listenAllBookings()
    }

    private fun listenAllBookings() {
        reg?.remove()
        _loading.value = true
        _error.value = null

        reg = db.collection("bookings")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _loading.value = false
                    _error.value = e.message
                    return@addSnapshotListener
                }

                val result = snap?.documents?.map { doc ->
                    val createdAt = doc.getTimestamp("createdAt") ?: Timestamp(0, 0)
                    AdminRiwayatItem(
                        bookingId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
                        mobilId = doc.getString("mobilId") ?: "",
                        mobilNama = doc.getString("mobilNama") ?: "",
                        tanggalSewaMillis = doc.getLong("tanggalSewaMillis") ?: 0L,
                        tanggalKembaliMillis = doc.getLong("tanggalKembaliMillis") ?: 0L,
                        totalHarga = (doc.getLong("totalHarga") ?: 0L).toInt(),
                        status = doc.getString("status") ?: "Pending",
                        createdAtMillis = createdAt.toDate().time
                    )
                } ?: emptyList()

                // sort client-side biar gak ribet index Firestore
                _list.value = result.sortedByDescending { it.createdAtMillis }
                _loading.value = false
            }
    }

    fun refresh() {
        listenAllBookings()
    }

    override fun onCleared() {
        reg?.remove()
        super.onCleared()
    }
}
