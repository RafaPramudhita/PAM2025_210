package com.example.rentalinaja.data.model

data class Mobil(
    val mobilId: String = "",
    val namaMobil: String = "",
    val hargaPerHari: Int = 0,
    val kapasitas: Int = 0,
    val status: String = "Tersedia",
    val fotoUrl: String = ""
)
