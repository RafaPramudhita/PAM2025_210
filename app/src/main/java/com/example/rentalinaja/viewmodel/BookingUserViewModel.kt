package com.example.rentalinaja.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UserBookingItem(
    val bookingId: String = "",
    val mobilId: String = "",
    val mobilNama: String = "",
    val tanggalSewaMillis: Long = 0L,
    val tanggalKembaliMillis: Long = 0L,
    val totalHarga: Int = 0,
    val status: String = "Pending",
    val createdAtMillis: Long = 0L
)

class BookingUserViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var reg: ListenerRegistration? = null

    private val _list = MutableStateFlow<List<UserBookingItem>>(emptyList())
    val list: StateFlow<List<UserBookingItem>> get() = _list

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    init {
        listenMyBookings()
    }

    private fun listenMyBookings() {
        reg?.remove()

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _error.value = "User belum login."
            _list.value = emptyList()
            return
        }

        _loading.value = true
        _error.value = null

        reg = db.collection("bookings")
            .whereEqualTo("userId", user.uid)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _loading.value = false
                    _error.value = e.message
                    return@addSnapshotListener
                }

                val result = snap?.documents?.map { doc ->
                    val createdAt = doc.getTimestamp("createdAt") ?: Timestamp(0, 0)
                    UserBookingItem(
                        bookingId = doc.id,
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

    fun refresh() {
        listenMyBookings()
    }

    /**
     * Cancel booking hanya jika:
     * - status == Pending
     * - milik user login
     *
     * Efek:
     * - booking.status -> Cancelled
     * - mobil.status -> Tersedia (hanya jika status mobil masih "Dipesan")
     *
     * ✅ FIX: semua READ dulu, baru WRITE (biar gak kena error transaksi)
     */
    fun cancelBookingPending(bookingId: String, mobilId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _error.value = "User belum login."
            return
        }

        _loading.value = true
        _error.value = null

        val bookingRef = db.collection("bookings").document(bookingId)
        val mobilRef = db.collection("mobil").document(mobilId)

        db.runTransaction { txn ->
            // ✅ READ SEMUA DULU
            val bookingSnap = txn.get(bookingRef)
            val mobilSnap = txn.get(mobilRef)

            val bookingStatus = bookingSnap.getString("status") ?: "Pending"
            val bookingUserId = bookingSnap.getString("userId") ?: ""

            if (bookingUserId != user.uid) {
                throw IllegalStateException("Booking ini bukan milik user yang sedang login.")
            }

            if (!bookingStatus.equals("Pending", ignoreCase = true)) {
                throw IllegalStateException("Booking sudah diproses, tidak bisa dicancel. Status: $bookingStatus")
            }

            val mobilStatus = mobilSnap.getString("status") ?: "Tersedia"

            // ✅ BARU WRITE
            txn.update(bookingRef, "status", "Cancelled")

            // Balikin mobil jadi tersedia hanya jika masih "Dipesan"
            if (mobilStatus.equals("Dipesan", ignoreCase = true)) {
                txn.update(mobilRef, "status", "Tersedia")
            }

            null
        }.addOnSuccessListener {
            _loading.value = false
        }.addOnFailureListener { e ->
            _loading.value = false
            _error.value = e.message ?: "Gagal cancel booking."
        }
    }

    override fun onCleared() {
        reg?.remove()
        super.onCleared()
    }
}
