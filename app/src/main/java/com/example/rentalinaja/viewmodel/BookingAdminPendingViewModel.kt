package com.example.rentalinaja.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AdminPendingItem(
    val bookingId: String = "",
    val mobilId: String = "",
    val mobilNama: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val tanggalSewaMillis: Long = 0L,
    val tanggalKembaliMillis: Long = 0L,
    val totalHarga: Int = 0,
    val status: String = "Pending",
    val createdAtMillis: Long = 0L
)

class BookingAdminPendingViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var reg: ListenerRegistration? = null

    private val _list = MutableStateFlow<List<AdminPendingItem>>(emptyList())
    val list: StateFlow<List<AdminPendingItem>> get() = _list

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    init {
        listenPending()
    }

    fun refresh() = listenPending()

    private fun listenPending() {
        reg?.remove()
        _loading.value = true
        _error.value = null

        reg = db.collection("bookings")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _loading.value = false
                    _error.value = e.message
                    return@addSnapshotListener
                }

                val result = snap?.documents?.map { doc ->
                    val createdAt = doc.getTimestamp("createdAt") ?: Timestamp(0, 0)
                    AdminPendingItem(
                        bookingId = doc.id,
                        mobilId = doc.getString("mobilId") ?: "",
                        mobilNama = doc.getString("mobilNama") ?: "",
                        userId = doc.getString("userId") ?: "",
                        userEmail = doc.getString("userEmail") ?: "",
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

    fun approve(bookingId: String, mobilId: String) {
        _loading.value = true
        _error.value = null

        val bookingRef = db.collection("bookings").document(bookingId)
        val mobilRef = db.collection("mobil").document(mobilId)

        db.runTransaction { txn ->
            // ✅ READ DULU (semua get)
            val bookingSnap = txn.get(bookingRef)
            val mobilSnap = txn.get(mobilRef)

            val st = bookingSnap.getString("status") ?: "Pending"
            if (!st.equals("Pending", ignoreCase = true)) {
                throw IllegalStateException("Booking sudah diproses. Status: $st")
            }

            // optional validasi
            val mobilStatus = mobilSnap.getString("status") ?: "Tersedia"
            // contoh: hanya boleh approve kalau mobil masih Dipesan / Tersedia (sesuaikan rule kamu)
            // if (mobilStatus.equals("Disewa", true)) throw IllegalStateException("Mobil sudah disewa.")

            // ✅ WRITE SETELAH READ
            txn.update(
                bookingRef,
                mapOf(
                    "status" to "Approved",
                    "processedAt" to Timestamp.now()
                )
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

    fun reject(bookingId: String, mobilId: String) {
        _loading.value = true
        _error.value = null

        val bookingRef = db.collection("bookings").document(bookingId)
        val mobilRef = db.collection("mobil").document(mobilId)

        db.runTransaction { txn ->
            // ✅ READ DULU (INI YANG FIX ERROR)
            val bookingSnap = txn.get(bookingRef)
            val mobilSnap = txn.get(mobilRef)

            val st = bookingSnap.getString("status") ?: "Pending"
            if (!st.equals("Pending", ignoreCase = true)) {
                throw IllegalStateException("Booking sudah diproses. Status: $st")
            }

            val mobilStatus = mobilSnap.getString("status") ?: "Tersedia"

            // ✅ WRITE SETELAH READ
            txn.update(
                bookingRef,
                mapOf(
                    "status" to "Rejected",
                    "processedAt" to Timestamp.now()
                )
            )

            // mobil -> Tersedia (jangan override kalau sudah Disewa)
            if (!mobilStatus.equals("Disewa", ignoreCase = true)) {
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
