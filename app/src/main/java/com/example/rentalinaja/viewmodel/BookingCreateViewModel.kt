package com.example.rentalinaja.viewmodel

import androidx.lifecycle.ViewModel
import com.example.rentalinaja.data.model.Mobil
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class BookingCreateViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _mobil = MutableStateFlow<Mobil?>(null)
    val mobil: StateFlow<Mobil?> get() = _mobil

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> get() = _message

    fun consumeMessage() { _message.value = null }
    fun clearError() { _error.value = null }

    fun loadMobil(mobilId: String) {
        _loading.value = true
        _error.value = null

        db.collection("mobil").document(mobilId)
            .get()
            .addOnSuccessListener { doc ->
                _mobil.value = Mobil(
                    mobilId = doc.id,
                    namaMobil = doc.getString("namaMobil") ?: "",
                    hargaPerHari = (doc.getLong("hargaPerHari") ?: 0L).toInt(),
                    kapasitas = (doc.getLong("kapasitas") ?: 0L).toInt(),
                    status = doc.getString("status") ?: "Tersedia",
                    fotoUrl = doc.getString("fotoUrl") ?: ""
                )
                _loading.value = false
            }
            .addOnFailureListener { e ->
                _loading.value = false
                _error.value = e.message
            }
    }

    /**
     * Submit booking:
     * - mobil.status harus "Tersedia"
     * - booking.status = "Pending"
     * - mobil.status = "Dipesan"
     */
    fun submitBooking(
        mobilId: String,
        tanggalSewaMillis: Long,
        tanggalKembaliMillis: Long
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _error.value = "User belum login."
            return
        }

        val m = _mobil.value
        if (m == null) {
            _error.value = "Data mobil belum siap."
            return
        }

        // ✅ hari ini (start of day) supaya tanggal lampau ditolak
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (tanggalSewaMillis < todayStart) {
            _error.value = "Tanggal sewa tidak boleh sebelum hari ini."
            return
        }

        if (tanggalSewaMillis <= 0L || tanggalKembaliMillis <= 0L || tanggalKembaliMillis <= tanggalSewaMillis) {
            _error.value = "Tanggal sewa/kembali tidak valid."
            return
        }

        val dayMs = 24L * 60L * 60L * 1000L
        val days = ((tanggalKembaliMillis - tanggalSewaMillis) / dayMs).toInt().coerceAtLeast(1)
        val total = days * m.hargaPerHari

        _loading.value = true
        _error.value = null

        val bookingRef = db.collection("bookings").document()
        val mobilRef = db.collection("mobil").document(mobilId)

        db.runTransaction { txn ->
            // READ dulu
            val mobilSnap = txn.get(mobilRef)
            val mobilStatus = mobilSnap.getString("status") ?: "Tersedia"

            if (!mobilStatus.equals("Tersedia", true)) {
                throw IllegalStateException("Mobil tidak tersedia. Status: $mobilStatus")
            }

            // WRITE setelah semua read
            txn.set(bookingRef, hashMapOf(
                "userId" to user.uid,
                "userEmail" to (user.email ?: ""),
                "userNama" to (user.displayName ?: ""),
                "mobilId" to mobilId,
                "mobilNama" to m.namaMobil,
                "tanggalSewaMillis" to tanggalSewaMillis,
                "tanggalKembaliMillis" to tanggalKembaliMillis,
                "totalHarga" to total,
                "status" to "Pending",
                "createdAt" to Timestamp.now()
            ))

            txn.update(mobilRef, "status", "Dipesan")

            null
        }.addOnSuccessListener {
            _loading.value = false
            _message.value = "Booking berhasil dibuat (Pending)."
        }.addOnFailureListener { e ->
            _loading.value = false
            _error.value = e.message ?: "Gagal membuat booking."
        }
    }
}
