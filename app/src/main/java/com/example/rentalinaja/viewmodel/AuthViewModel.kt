package com.example.rentalinaja.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // UI State
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> get() = _loading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> get() = _errorMessage

    private val _loginSuccess = MutableStateFlow<String?>(null) // role
    val loginSuccess: StateFlow<String?> get() = _loginSuccess

    private val _registerSuccess = MutableStateFlow<Boolean>(false)
    val registerSuccess: StateFlow<Boolean> get() = _registerSuccess


    // ==============================
    // REGISTER USER
    // ==============================
    fun registerUser(nama: String, email: String, password: String) {
        _loading.value = true
        _errorMessage.value = null

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user!!.uid

                val userData = hashMapOf(
                    "nama" to nama,
                    "email" to email,
                    "role" to "user"        // role default untuk pelanggan
                )

                db.collection("users")
                    .document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        _loading.value = false
                        _registerSuccess.value = true
                    }
                    .addOnFailureListener { e ->
                        _loading.value = false
                        _errorMessage.value = e.message
                    }

            }
            .addOnFailureListener { e ->
                _loading.value = false
                _errorMessage.value = e.message
            }
    }


    // ==============================
    // LOGIN USER / ADMIN
    // ==============================
    fun loginUser(email: String, password: String) {
        _loading.value = true
        _errorMessage.value = null

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user!!.uid

                db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val role = doc.getString("role")
                        _loading.value = false
                        _loginSuccess.value = role   // "admin" / "user"
                    }
                    .addOnFailureListener { e ->
                        _loading.value = false
                        _errorMessage.value = e.message
                    }
            }
            .addOnFailureListener { e ->
                _loading.value = false
                _errorMessage.value = e.message
            }
    }
}