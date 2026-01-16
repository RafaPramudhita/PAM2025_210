package com.example.rentalinaja.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AdminBookingItem(
    val bookingId: String = "",
    val userId: String = "",
    val userNama: String = "",
    val userEmail: String = "",
    val mobilId: String = "",
    val mobilNama: String = "",
    val tanggalSewaMillis: Long = 0L,
    val tanggalKembaliMillis: Long = 0L,
    val totalHarga: Int = 0,
    val status: String = "Pending",
    val createdAtMillis: Long = 0L
)

class BookingAdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var reg: ListenerRegistration? = null

    private val _list = MutableStateFlow<List<AdminBookingItem>>(emptyList())
    val list: StateFlow<List<AdminBookingItem>> get() = _list

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    init {
        listenAllBookings()
    }

    fun refresh() = listenAllBookings()

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
                    AdminBookingItem(
                        bookingId = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userNama = doc.getString("userNama") ?: "",
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

                _list.value = result.sortedByDescending { it.createdAtMillis }
                _loading.value = false
            }
    }

    fun approveBooking(bookingId: String, mobilId: String) {
        _loading.value = true
        _error.value = null

        val bookingRef = db.collection("bookings").document(bookingId)
        val mobilRef = db.collection("mobil").document(mobilId)

        db.runTransaction { txn ->
            // READ dulu
            val bookingSnap = txn.get(bookingRef)
            val mobilSnap = txn.get(mobilRef)

            val bookingStatus = bookingSnap.getString("status") ?: "Pending"
            if (!bookingStatus.equals("Pending", true)) {
                throw IllegalStateException("Booking sudah diproses. Status: $bookingStatus")
            }

            val mobilStatus = mobilSnap.getString("status") ?: "Tersedia"
            if (mobilStatus.equals("Disewa", true)) {
                throw IllegalStateException("Mobil sudah Disewa, tidak bisa approve.")
            }

            // WRITE setelah read
            txn.update(
                bookingRef,
                mapOf("status" to "Approved", "processedAt" to Timestamp.now())
            )
            txn.update(mobilRef, "status", "Disewa")

            null
        }.addOnSuccessListener {
            _loading.value = false
        }.addOnFailureListener { e ->
            _loading.value = false
            _error.value = e.message ?: "Gagal approve booking."
        }
    }

    fun rejectBooking(bookingId: String, mobilId: String) {
        _loading.value = true
        _error.value = null

        val bookingRef = db.collection("bookings").document(bookingId)
        val mobilRef = db.collection("mobil").document(mobilId)

        db.runTransaction { txn ->
            // READ dulu
            val bookingSnap = txn.get(bookingRef)
            val mobilSnap = txn.get(mobilRef)

            val bookingStatus = bookingSnap.getString("status") ?: "Pending"
            if (!bookingStatus.equals("Pending", true)) {
                throw IllegalStateException("Booking sudah diproses. Status: $bookingStatus")
            }

            val mobilStatus = mobilSnap.getString("status") ?: "Tersedia"

            // WRITE
            txn.update(
                bookingRef,
                mapOf("status" to "Rejected", "processedAt" to Timestamp.now())
            )

            // kalau masih Dipesan, balikin Tersedia
            if (!mobilStatus.equals("Disewa", true)) {
                txn.update(mobilRef, "status", "Tersedia")
            }

            null
        }.addOnSuccessListener {
            _loading.value = false
        }.addOnFailureListener { e ->
            _loading.value = false
            _error.value = e.message ?: "Gagal reject booking."
        }
    }

    override fun onCleared() {
        reg?.remove()
        super.onCleared()
    }
}
