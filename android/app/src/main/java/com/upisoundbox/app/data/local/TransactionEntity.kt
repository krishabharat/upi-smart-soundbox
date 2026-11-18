package com.upisoundbox.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["orderId"], unique = true),
        Index(value = ["bankRrn"]),
        Index(value = ["createdAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val merchantId: String,
    val orderId: String,
    val paymentId: String?,
    val bankRrn: String?,
    val payerVpa: String?,
    val payerName: String?,
    val amount: Double,
    val currency: String = "INR",
    val status: String,
    val announced: Boolean = false,
    val announcementText: String?,
    val createdAt: Long = System.currentTimeMillis()
)

