package com.example.rentalinaja.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BookingViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> get() = _success

    fun clearError() {
        _error.value = null
    }

    fun resetSuccess() {
        _success.value = false
    }

    /**
     * Create booking (Pending) + set mobil status -> "Dipesan" (reserve).
     * Transaction ensures no double booking if mobil already not "Tersedia".
     */
    fun createBookingPending(
        mobilId: String,
        mobilNama: String,
        hargaPerHari: Int,
        tanggalSewaMillis: Long,
        tanggalKembaliMillis: Long,
        totalHarga: Int
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _error.value = "User belum login."
            return
        }

        _loading.value = true
        _error.value = null
        _success.value = false

        val mobilRef = db.collection("mobil").document(mobilId)
        val bookingRef = db.collection("bookings").document() // auto id

        db.runTransaction { txn ->
            val mobilSnap = txn.get(mobilRef)
            val status = mobilSnap.getString("status") ?: "Tersedia"

            // Cegah double booking
            if (!status.equals("Tersedia", ignoreCase = true)) {
                throw IllegalStateException("Mobil tidak tersedia. Status saat ini: $status")
            }

            // 1) create booking pending
            val bookingData = hashMapOf(
                "userId" to user.uid,
                "userEmail" to (user.email ?: ""),
                "mobilId" to mobilId,
                "mobilNama" to mobilNama,
                "hargaPerHari" to hargaPerHari,
                "tanggalSewaMillis" to tanggalSewaMillis,
                "tanggalKembaliMillis" to tanggalKembaliMillis,
                "totalHarga" to totalHarga,
                "status" to "Pending",
                "createdAt" to FieldValue.serverTimestamp()
            )
            txn.set(bookingRef, bookingData)

            // 2) reserve mobil: status -> Dipesan
            txn.update(mobilRef, "status", "Dipesan")

            null
        }.addOnSuccessListener {
            _loading.value = false
            _success.value = true
        }.addOnFailureListener { e ->
            _loading.value = false
            _error.value = e.message ?: "Gagal membuat booking."
        }
    }
}
