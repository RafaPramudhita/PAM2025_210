package com.example.rentalinaja.viewmodel

import androidx.lifecycle.ViewModel
import com.example.rentalinaja.data.model.Mobil
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MobilViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var reg: ListenerRegistration? = null

    private val _mobilList = MutableStateFlow<List<Mobil>>(emptyList())
    val mobilList: StateFlow<List<Mobil>> get() = _mobilList

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    init {
        listenMobils()
    }

    fun clearError() {
        _error.value = null
    }

    /**
     * Kamu sudah pakai addSnapshotListener, jadi data sebenarnya realtime.
     * refreshMobils() ini hanya untuk "paksa pasang ulang listener" (buat tombol refresh).
     */
    fun refreshMobils() {
        listenMobils()
    }

    private fun listenMobils() {
        reg?.remove()

        _loading.value = true
        _error.value = null

        reg = db.collection("mobil")
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _loading.value = false
                    _error.value = e.message
                    return@addSnapshotListener
                }

                _mobilList.value = snap?.documents?.map { doc ->
                    Mobil(
                        mobilId = doc.id,
                        namaMobil = doc.getString("namaMobil") ?: "",
                        hargaPerHari = (doc.getLong("hargaPerHari") ?: 0L).toInt(),
                        kapasitas = (doc.getLong("kapasitas") ?: 0L).toInt(),
                        status = doc.getString("status") ?: "Tersedia",
                        fotoUrl = doc.getString("fotoUrl") ?: ""
                    )
                } ?: emptyList()

                _loading.value = false
            }
    }

    fun addMobil(
        nama: String,
        harga: Int,
        kapasitas: Int,
        status: String,
        fotoUrl: String
    ) {
        _loading.value = true
        _error.value = null

        val data = hashMapOf(
            "namaMobil" to nama.trim(),
            "hargaPerHari" to harga,
            "kapasitas" to kapasitas,
            "status" to status,
            "fotoUrl" to fotoUrl.trim()
        )

        db.collection("mobil")
            .document()
            .set(data)
            .addOnSuccessListener {
                _loading.value = false
                _error.value = null
            }
            .addOnFailureListener { e ->
                _loading.value = false
                _error.value = e.message
            }
    }

    fun updateMobil(
        mobilId: String,
        nama: String,
        harga: Int,
        kapasitas: Int,
        status: String,
        fotoUrl: String
    ) {
        _loading.value = true
        _error.value = null

        val data: Map<String, Any> = mapOf(
            "namaMobil" to nama.trim(),
            "hargaPerHari" to harga,
            "kapasitas" to kapasitas,
            "status" to status,
            "fotoUrl" to fotoUrl.trim()
        )

        db.collection("mobil")
            .document(mobilId)
            .update(data)
            .addOnSuccessListener {
                _loading.value = false
                _error.value = null
            }
            .addOnFailureListener { e ->
                _loading.value = false
                _error.value = e.message
            }
    }

    fun deleteMobil(mobil: Mobil) {
        // ✅ Biar aman: gak boleh hapus kalau dipakai/dipesan
        val st = mobil.status.trim()

        if (st.equals("Disewa", ignoreCase = true) || st.equals("Dipesan", ignoreCase = true)) {
            _error.value = "Mobil sedang dipakai/dipesan, tidak bisa dihapus."
            return
        }

        _loading.value = true
        _error.value = null

        db.collection("mobil")
            .document(mobil.mobilId)
            .delete()
            .addOnSuccessListener {
                _loading.value = false
                _error.value = null
            }
            .addOnFailureListener { e ->
                _loading.value = false
                _error.value = e.message
            }
    }

    override fun onCleared() {
        reg?.remove()
        super.onCleared()
    }
}
