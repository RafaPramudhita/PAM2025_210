package com.example.rentalinaja.data.model

data class Booking(
    val bookingId: String = "",
    val userId: String = "",
    val mobilId: String = "",
    val tanggalSewaMillis: Long = 0L,
    val tanggalKembaliMillis: Long = 0L,
    val totalHarga: Int = 0,
    val status: String = "Pending", // Pending/Approved/Rejected
    val createdAt: Long = 0L
)
