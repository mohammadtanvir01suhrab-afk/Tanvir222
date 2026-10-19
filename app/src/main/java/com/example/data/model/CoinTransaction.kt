package com.example.data.model

import java.util.Date

data class CoinTransaction(
    val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val targetUid: String = "",
    val amount: Long = 0L,
    val note: String = "",
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val timestamp: TimestampAdapter get() = TimestampAdapter(timestampMillis)

    class TimestampAdapter(private val millis: Long) {
        fun toDate(): Date = Date(millis)
    }
}
